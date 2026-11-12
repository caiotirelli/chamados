package br.com.sistemas.chamados.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.TecnicoService;
import br.com.sistemas.chamados.web.form.TecnicoForm;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/app/tecnicos")
public class TecnicoWebController {

    private static final String LISTA = "tecnicos/lista";
    private static final String FORM = "tecnicos/form";
    private static final String REDIRECT_LISTA = "redirect:/app/tecnicos";

    private final TecnicoService service;

    public TecnicoWebController(TecnicoService service) {
        this.service = service;
    }

    /** Disponível em todas as páginas deste controller (opções do select de perfil). */
    @ModelAttribute("perfis")
    public Perfil[] perfis() {
        return Perfil.values();
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tecnicos", service.listar());
        return LISTA;
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new TecnicoForm());
        return FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") TecnicoForm form, BindingResult result,
                        RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return FORM;
        }
        try {
            service.criar(form.toRequest());
        } catch (RegraNegocioException e) {
            result.reject("regraNegocio", e.getMessage());
            return FORM;
        }
        redirect.addFlashAttribute("sucesso", "Técnico cadastrado com sucesso");
        return REDIRECT_LISTA;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("form", TecnicoForm.de(service.buscar(id)));
        return FORM;
    }

    @PutMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") TecnicoForm form,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        model.addAttribute("id", id);
        if (result.hasErrors()) {
            return FORM;
        }
        try {
            service.atualizar(id, form.toRequest());
        } catch (RegraNegocioException e) {
            result.reject("regraNegocio", e.getMessage());
            return FORM;
        }
        redirect.addFlashAttribute("sucesso", "Técnico atualizado com sucesso");
        return REDIRECT_LISTA;
    }

    @DeleteMapping("/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Técnico excluído com sucesso");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return REDIRECT_LISTA;
    }
}
