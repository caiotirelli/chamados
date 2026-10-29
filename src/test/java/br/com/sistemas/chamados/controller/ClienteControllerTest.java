package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;

/** @WebMvcTest sobe só a camada web (controller + exception handler); o Service é simulado. */
@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    private static final String JSON_ANA =
        "{\"nome\":\"Ana Souza\",\"email\":\"ana@exemplo.com\",\"telefone\":\"47999999999\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClienteService service;

    @MockitoBean
    private ChamadoService chamadoService;

    @Test
    void criarDeveRetornar201ComLocation() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenReturn(new ClienteResponse(1L, "Ana Souza", "ana@exemplo.com", "47999999999"));

        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_ANA))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/clientes/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("ana@exemplo.com"));
    }

    @Test
    void dadosInvalidosDevemRetornar400ComDetalhes() throws Exception {
        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"email\":\"email-invalido\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Dados inválidos"))
            .andExpect(jsonPath("$.detalhes.length()").value(2));
    }

    @Test
    void emailDuplicadoDeveRetornar409() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe cliente com o e-mail ana@exemplo.com"));

        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_ANA))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void idInexistenteDeveRetornar404() throws Exception {
        when(service.buscar(99999L))
            .thenThrow(new RecursoNaoEncontradoException("Cliente 99999 não encontrado"));

        mvc.perform(get("/clientes/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Cliente 99999 não encontrado"));
    }
}
