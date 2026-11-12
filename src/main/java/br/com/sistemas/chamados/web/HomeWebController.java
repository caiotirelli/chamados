package br.com.sistemas.chamados.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeWebController {

    @GetMapping({"/", "/app"})
    public String inicio() {
        return "redirect:/app/chamados";
    }
}
