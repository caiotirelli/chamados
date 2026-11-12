package br.com.sistemas.chamados.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.CategoriaService;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;
import br.com.sistemas.chamados.service.TecnicoService;

@WebMvcTest(ChamadoWebController.class)
class ChamadoWebControllerTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 14, 30);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChamadoService service;
    @MockitoBean
    private ClienteService clienteService;
    @MockitoBean
    private CategoriaService categoriaService;
    @MockitoBean
    private TecnicoService tecnicoService;

    private static ChamadoResponse chamado(StatusChamado status) {
        return new ChamadoResponse(1L, "PC não liga", "Sem energia", Prioridade.ALTA, status,
            AGORA, null, 1L, "Ana", 1L, "Hardware", null, null);
    }

    @Test
    void listarRenderizaChamadosPaginados() throws Exception {
        when(service.listar(any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(chamado(StatusChamado.ABERTO)), PageRequest.of(0, 10), 1));

        mvc.perform(get("/app/chamados").param("status", "ABERTO"))
            .andExpect(status().isOk())
            .andExpect(view().name("chamados/lista"))
            .andExpect(content().string(containsString("PC não liga")))
            .andExpect(content().string(containsString("10/11/2026 14:30")));

        verify(service).listar(eq(StatusChamado.ABERTO), eq(null), eq(null), any());
    }

    @Test
    void detalheMostraComentariosEApenasTransicoesPermitidas() throws Exception {
        ChamadoResponse c = chamado(StatusChamado.ABERTO);
        when(service.buscar(1L)).thenReturn(new ChamadoDetalheResponse(c,
            List.of(new ComentarioResponse(1L, "Verificar a tomada", "Carlos", AGORA))));

        mvc.perform(get("/app/chamados/1"))
            .andExpect(status().isOk())
            .andExpect(view().name("chamados/detalhe"))
            .andExpect(content().string(containsString("Verificar a tomada")))
            .andExpect(model().attribute("proximosStatus",
                List.of(StatusChamado.EM_ATENDIMENTO, StatusChamado.FECHADO)));
    }

    @Test
    void detalheDeChamadoFechadoNaoMostraFormulariosDeAlteracao() throws Exception {
        when(service.buscar(1L)).thenReturn(new ChamadoDetalheResponse(chamado(StatusChamado.FECHADO), List.of()));

        mvc.perform(get("/app/chamados/1"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Atribuir técnico"))))
            .andExpect(content().string(containsString("não aceita novos comentários")));
    }

    @Test
    void criarComDadosValidosRedirecionaParaDetalhe() throws Exception {
        when(service.criar(any(ChamadoRequest.class))).thenReturn(chamado(StatusChamado.ABERTO));

        mvc.perform(post("/app/chamados")
                .param("titulo", "PC não liga")
                .param("descricao", "Sem energia")
                .param("prioridade", "ALTA")
                .param("clienteId", "1")
                .param("categoriaId", "1"))
            .andExpect(redirectedUrl("/app/chamados/1"));
    }

    @Test
    void criarSemCamposObrigatoriosVoltaParaFormulario() throws Exception {
        mvc.perform(post("/app/chamados").param("titulo", "").param("prioridade", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("chamados/form"))
            .andExpect(model().attributeHasFieldErrors("form", "titulo", "descricao", "prioridade",
                "clienteId", "categoriaId"));
    }

    @Test
    void alterarStatusComTransicaoInvalidaMostraErro() throws Exception {
        when(service.alterarStatus(1L, StatusChamado.EM_ATENDIMENTO))
            .thenThrow(new RegraNegocioException("Atribua um técnico antes de colocar o chamado em atendimento"));

        mvc.perform(post("/app/chamados/1/status")
                .param("_method", "patch")
                .param("status", "EM_ATENDIMENTO"))
            .andExpect(redirectedUrl("/app/chamados/1"))
            .andExpect(flash().attribute("erro", "Atribua um técnico antes de colocar o chamado em atendimento"));
    }
}
