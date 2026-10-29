package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;

@DataJpaTest
class ChamadoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ChamadoRepository repository;

    private Cliente ana;
    private Cliente joao;

    @BeforeEach
    void popular() {
        ana = em.persist(new Cliente("Ana", "ana@exemplo.com", null));
        joao = em.persist(new Cliente("João", "joao@exemplo.com", null));
        Categoria hardware = em.persist(new Categoria("Hardware", null));

        persistir("PC não liga", Prioridade.ALTA, StatusChamado.ABERTO, ana, hardware);
        persistir("Mouse quebrado", Prioridade.BAIXA, StatusChamado.FECHADO, ana, hardware);
        persistir("Impressora", Prioridade.ALTA, StatusChamado.ABERTO, joao, hardware);
    }

    @Test
    void semFiltrosDeveRetornarTodos() {
        assertThat(repository.filtrar(null, null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
    }

    @Test
    void deveFiltrarPorStatus() {
        Page<Chamado> page = repository.filtrar(StatusChamado.ABERTO, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Chamado::getTitulo)
            .containsExactlyInAnyOrder("PC não liga", "Impressora");
    }

    @Test
    void deveCombinarPrioridadeECliente() {
        Page<Chamado> page = repository.filtrar(null, Prioridade.ALTA, ana.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Chamado::getTitulo).containsExactly("PC não liga");
    }

    @Test
    void devePaginarEOrdenar() {
        Page<Chamado> page = repository.filtrar(null, null, null,
            PageRequest.of(0, 2, Sort.by("titulo")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Chamado::getTitulo)
            .containsExactly("Impressora", "Mouse quebrado");
    }

    @Test
    void existsByClienteIdDeveIndicarDependencia() {
        assertThat(repository.existsByClienteId(joao.getId())).isTrue();
        Cliente semChamados = em.persist(new Cliente("Sem", "sem@exemplo.com", null));
        assertThat(repository.existsByClienteId(semChamados.getId())).isFalse();
    }

    private void persistir(String titulo, Prioridade prioridade, StatusChamado status,
                           Cliente cliente, Categoria categoria) {
        Chamado c = new Chamado(titulo, "descrição", prioridade, cliente, categoria);
        c.setStatus(status);
        c.setDataAbertura(LocalDateTime.now());
        em.persist(c);
    }
}
