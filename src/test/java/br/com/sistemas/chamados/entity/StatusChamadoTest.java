package br.com.sistemas.chamados.entity;

import static br.com.sistemas.chamados.entity.StatusChamado.ABERTO;
import static br.com.sistemas.chamados.entity.StatusChamado.AGUARDANDO_CLIENTE;
import static br.com.sistemas.chamados.entity.StatusChamado.EM_ATENDIMENTO;
import static br.com.sistemas.chamados.entity.StatusChamado.FECHADO;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatusChamadoTest {

    @Test
    void transicoesValidas() {
        assertThat(ABERTO.podeMudarPara(EM_ATENDIMENTO)).isTrue();
        assertThat(ABERTO.podeMudarPara(FECHADO)).isTrue();
        assertThat(EM_ATENDIMENTO.podeMudarPara(AGUARDANDO_CLIENTE)).isTrue();
        assertThat(AGUARDANDO_CLIENTE.podeMudarPara(EM_ATENDIMENTO)).isTrue();
        assertThat(EM_ATENDIMENTO.podeMudarPara(FECHADO)).isTrue();
    }

    @Test
    void transicoesInvalidas() {
        assertThat(ABERTO.podeMudarPara(AGUARDANDO_CLIENTE)).isFalse();
        assertThat(ABERTO.podeMudarPara(ABERTO)).isFalse();
        assertThat(EM_ATENDIMENTO.podeMudarPara(ABERTO)).isFalse();
    }

    @Test
    void chamadoFechadoNaoMudaDeStatus() {
        for (StatusChamado s : StatusChamado.values()) {
            assertThat(FECHADO.podeMudarPara(s)).isFalse();
        }
    }
}
