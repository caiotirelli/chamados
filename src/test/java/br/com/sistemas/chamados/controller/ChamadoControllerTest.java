package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.service.ChamadoService;

@WebMvcTest(ChamadoController.class)
class ChamadoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChamadoService service;

    @Test
    void chamadoSemCamposObrigatoriosDeveRetornar400() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes.length()").value(5));
    }

    @Test
    void alterarChamadoFechadoDeveRetornar409() throws Exception {
        when(service.alterarStatus(eq(1L), eq(StatusChamado.ABERTO)))
            .thenThrow(new ChamadoFechadoException(1L));

        mvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ABERTO\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("Chamado 1 está fechado e não pode ser alterado"));
    }

    @Test
    void statusInexistenteNoCorpoDeveRetornar400() throws Exception {
        mvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"XYZ\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void filtroComStatusInvalidoDeveRetornar400() throws Exception {
        mvc.perform(get("/chamados").param("status", "XYZ"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Parâmetro inválido"));
    }
}
