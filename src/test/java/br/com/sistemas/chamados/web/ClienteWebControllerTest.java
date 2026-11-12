package br.com.sistemas.chamados.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ClienteService;

/** Testa as telas Thymeleaf de clientes: o Service é simulado e o HTML é renderizado de verdade. */
@WebMvcTest(ClienteWebController.class)
class ClienteWebControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClienteService service;

    @Test
    void listarRenderizaTabelaComClientes() throws Exception {
        when(service.listar()).thenReturn(List.of(
            new ClienteResponse(1L, "Ana Souza", "ana@exemplo.com", "47999999999")));

        mvc.perform(get("/app/clientes"))
            .andExpect(status().isOk())
            .andExpect(view().name("clientes/lista"))
            .andExpect(content().string(containsString("Ana Souza")))
            .andExpect(content().string(containsString("ana@exemplo.com")));
    }

    @Test
    void criarComDadosValidosRedirecionaParaLista() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenReturn(new ClienteResponse(1L, "Ana", "ana@exemplo.com", null));

        mvc.perform(post("/app/clientes")
                .param("nome", "Ana")
                .param("email", "ana@exemplo.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/clientes"))
            .andExpect(flash().attribute("sucesso", "Cliente cadastrado com sucesso"));
    }

    @Test
    void criarComDadosInvalidosVoltaParaFormularioComErros() throws Exception {
        mvc.perform(post("/app/clientes")
                .param("nome", "")
                .param("email", "nao-e-email"))
            .andExpect(status().isOk())
            .andExpect(view().name("clientes/form"))
            .andExpect(model().attributeHasFieldErrors("form", "nome", "email"))
            .andExpect(content().string(containsString("Nome é obrigatório")));

        verify(service, never()).criar(any());
    }

    @Test
    void criarComEmailDuplicadoMostraErroDeRegraNoFormulario() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe cliente com o e-mail ana@exemplo.com"));

        mvc.perform(post("/app/clientes")
                .param("nome", "Ana")
                .param("email", "ana@exemplo.com"))
            .andExpect(status().isOk())
            .andExpect(view().name("clientes/form"))
            .andExpect(content().string(containsString("Já existe cliente com o e-mail ana@exemplo.com")));
    }

    @Test
    void editarCarregaFormularioPreenchido() throws Exception {
        when(service.buscar(1L)).thenReturn(new ClienteResponse(1L, "Ana", "ana@exemplo.com", "4799"));

        mvc.perform(get("/app/clientes/1/editar"))
            .andExpect(status().isOk())
            .andExpect(view().name("clientes/form"))
            .andExpect(content().string(containsString("value=\"ana@exemplo.com\"")))
            .andExpect(content().string(containsString("name=\"_method\" value=\"put\"")));
    }

    @Test
    void atualizarViaPutDoFormularioRedireciona() throws Exception {
        when(service.atualizar(any(), any(ClienteRequest.class)))
            .thenReturn(new ClienteResponse(1L, "Ana", "ana@exemplo.com", null));

        mvc.perform(post("/app/clientes/1")
                .param("_method", "put")
                .param("nome", "Ana")
                .param("email", "ana@exemplo.com"))
            .andExpect(redirectedUrl("/app/clientes"))
            .andExpect(flash().attribute("sucesso", "Cliente atualizado com sucesso"));
    }

    @Test
    void excluirComChamadosMostraMensagemDeErro() throws Exception {
        doThrow(new RegraNegocioException("Cliente 1 possui chamados e não pode ser excluído"))
            .when(service).excluir(1L);

        mvc.perform(post("/app/clientes/1").param("_method", "delete"))
            .andExpect(redirectedUrl("/app/clientes"))
            .andExpect(flash().attribute("erro", "Cliente 1 possui chamados e não pode ser excluído"));
    }

    @Test
    void clienteInexistenteMostraPaginaDeErro404() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

        mvc.perform(get("/app/clientes/99/editar"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("erro"))
            .andExpect(content().string(containsString("Cliente 99 não encontrado")));
    }
}
