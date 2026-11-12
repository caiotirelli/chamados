package br.com.sistemas.chamados.web;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.CategoriaService;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;
import br.com.sistemas.chamados.service.TecnicoService;
import br.com.sistemas.chamados.web.form.ChamadoForm;
import br.com.sistemas.chamados.web.form.ComentarioForm;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/app/chamados")
public class ChamadoWebController {

    private static final String LISTA = "chamados/lista";
    private static final String FORM = "chamados/form";
    private static final String DETALHE = "chamados/detalhe";

    private final ChamadoService service;
    private final ClienteService clienteService;
    private final CategoriaService categoriaService;
    private final TecnicoService tecnicoService;

    public ChamadoWebController(ChamadoService service, ClienteService clienteService,
                                CategoriaService categoriaService, TecnicoService tecnicoService) {
        this.service = service;
        this.clienteService = clienteService;
        this.categoriaService = categoriaService;
        this.tecnicoService = tecnicoService;
    }

    @ModelAttribute("prioridades")
    public Prioridade[] prioridades() {
        return Prioridade.values();
    }

    @ModelAttribute("todosStatus")
    public StatusChamado[] todosStatus() {
        return StatusChamado.values();
    }

    /** Ex.: /app/chamados?status=ABERTO&prioridade=ALTA&clienteId=1&page=0 */
    @GetMapping
    public String listar(@RequestParam(required = false) StatusChamado status,
                         @RequestParam(required = false) Prioridade prioridade,
                         @RequestParam(required = false) Long clienteId,
                         @PageableDefault(size = 10, sort = "dataAbertura", direction = Sort.Direction.DESC)
                         Pageable pageable,
                         Model model) {
        model.addAttribute("pagina", service.listar(status, prioridade, clienteId, pageable));
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("status", status);
        model.addAttribute("prioridade", prioridade);
        model.addAttribute("clienteId", clienteId);
        return LISTA;
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new ChamadoForm());
        return prepararForm(model, null);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") ChamadoForm form, BindingResult result,
                        Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return prepararForm(model, null);
        }
        ChamadoResponse criado = service.criar(form.toRequest());
        redirect.addFlashAttribute("sucesso", "Chamado #" + criado.id() + " aberto com sucesso");
        return "redirect:/app/chamados/" + criado.id();
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("comentarioForm", new ComentarioForm());
        return carregarDetalhe(id, model);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        ChamadoResponse chamado = service.buscar(id).chamado();
        if (chamado.status() == StatusChamado.FECHADO) {
            redirect.addFlashAttribute("erro", "Chamado " + id + " está fechado e não pode ser alterado");
            return redirectDetalhe(id);
        }
        model.addAttribute("form", ChamadoForm.de(chamado));
        return prepararForm(model, id);
    }

    @PutMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ChamadoForm form,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return prepararForm(model, id);
        }
        try {
            service.atualizar(id, form.toRequest());
            redirect.addFlashAttribute("sucesso", "Chamado atualizado com sucesso");
        } catch (ChamadoFechadoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return redirectDetalhe(id);
    }

    @DeleteMapping("/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        service.excluir(id);
        redirect.addFlashAttribute("sucesso", "Chamado #" + id + " excluído com sucesso");
        return "redirect:/app/chamados";
    }

    @PatchMapping("/{id}/atribuir")
    public String atribuir(@PathVariable Long id, @RequestParam(required = false) Long tecnicoId,
                           RedirectAttributes redirect) {
        if (tecnicoId == null) {
            redirect.addFlashAttribute("erro", "Selecione um técnico");
            return redirectDetalhe(id);
        }
        try {
            service.atribuir(id, tecnicoId);
            redirect.addFlashAttribute("sucesso", "Técnico atribuído com sucesso");
        } catch (ChamadoFechadoException | RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return redirectDetalhe(id);
    }

    @PatchMapping("/{id}/status")
    public String alterarStatus(@PathVariable Long id, @RequestParam(required = false) StatusChamado status,
                                RedirectAttributes redirect) {
        if (status == null) {
            redirect.addFlashAttribute("erro", "Selecione o novo status");
            return redirectDetalhe(id);
        }
        try {
            service.alterarStatus(id, status);
            redirect.addFlashAttribute("sucesso", "Status alterado para " + status);
        } catch (ChamadoFechadoException | RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return redirectDetalhe(id);
    }

    @PostMapping("/{id}/comentarios")
    public String comentar(@PathVariable Long id,
                           @Valid @ModelAttribute("comentarioForm") ComentarioForm form,
                           BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return carregarDetalhe(id, model);
        }
        try {
            service.comentar(id, form.toRequest());
            redirect.addFlashAttribute("sucesso", "Comentário adicionado");
        } catch (ChamadoFechadoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return redirectDetalhe(id);
    }

    private String prepararForm(Model model, Long id) {
        model.addAttribute("id", id);
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("categorias", categoriaService.listar());
        return FORM;
    }

    private String carregarDetalhe(Long id, Model model) {
        ChamadoDetalheResponse detalhe = service.buscar(id);
        StatusChamado atual = detalhe.chamado().status();
        List<StatusChamado> proximos = Arrays.stream(StatusChamado.values())
            .filter(atual::podeMudarPara)
            .toList();
        model.addAttribute("detalhe", detalhe);
        model.addAttribute("chamado", detalhe.chamado());
        model.addAttribute("proximosStatus", proximos);
        model.addAttribute("tecnicos", tecnicoService.listar());
        return DETALHE;
    }

    private String redirectDetalhe(Long id) {
        return "redirect:/app/chamados/" + id;
    }
}
