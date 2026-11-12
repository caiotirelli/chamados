package br.com.sistemas.chamados.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;

/**
 * Tratamento de erros das telas (pacote web): mostra a página "erro" em vez do JSON da API.
 * O GlobalExceptionHandler continua cuidando apenas dos @RestController.
 */
@ControllerAdvice(basePackageClasses = WebExceptionHandler.class)
public class WebExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ModelAndView naoEncontrado(RecursoNaoEncontradoException ex) {
        return pagina(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return pagina(HttpStatus.BAD_REQUEST,
            "Parâmetro inválido: " + ex.getName() + " = '" + ex.getValue() + "'");
    }

    private ModelAndView pagina(HttpStatus status, String mensagem) {
        ModelAndView mv = new ModelAndView("erro", status);
        mv.addObject("status", status.value());
        mv.addObject("mensagem", mensagem);
        return mv;
    }
}
