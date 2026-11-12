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

import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ClienteService;
import br.com.sistemas.chamados.web.form.ClienteForm;
import jakarta.validation.Valid;

/**
 * Controller WEB (Thymeleaf): devolve páginas HTML em vez de JSON.
 * Reaproveita o mesmo ClienteService usado pela API REST.
 */
@Controller
@RequestMapping("/app/clientes")
public class ClienteWebController {

    private static final String LISTA = "clientes/lista";
    private static final String FORM = "clientes/form";
    private static final String REDIRECT_LISTA = "redirect:/app/clientes";

    private final ClienteService service;

    public ClienteWebController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", service.listar());
        return LISTA;
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new ClienteForm());
        return FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") ClienteForm form, BindingResult result,
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
        redirect.addFlashAttribute("sucesso", "Cliente cadastrado com sucesso");
        return REDIRECT_LISTA;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("form", ClienteForm.de(service.buscar(id)));
        return FORM;
    }

    @PutMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ClienteForm form,
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
        redirect.addFlashAttribute("sucesso", "Cliente atualizado com sucesso");
        return REDIRECT_LISTA;
    }

    @DeleteMapping("/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Cliente excluído com sucesso");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return REDIRECT_LISTA;
    }
}
