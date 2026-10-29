package br.com.sistemas.chamados.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ComentarioRepository;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;
    private final ComentarioRepository comentarioRepository;
    private final ClienteService clienteService;
    private final CategoriaService categoriaService;
    private final TecnicoService tecnicoService;

    public ChamadoService(ChamadoRepository repository,
                          ComentarioRepository comentarioRepository,
                          ClienteService clienteService,
                          CategoriaService categoriaService,
                          TecnicoService tecnicoService) {
        this.repository = repository;
        this.comentarioRepository = comentarioRepository;
        this.clienteService = clienteService;
        this.categoriaService = categoriaService;
        this.tecnicoService = tecnicoService;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {
        Chamado chamado = new Chamado(dto.titulo(), dto.descricao(), dto.prioridade(),
            clienteService.buscarEntity(dto.clienteId()),
            categoriaService.buscarEntity(dto.categoriaId()));
        // Regra: todo chamado nasce ABERTO, com a data de abertura preenchida pelo sistema.
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDataAbertura(LocalDateTime.now());
        return ChamadoResponse.de(repository.save(chamado));
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listar(StatusChamado status, Prioridade prioridade,
                                        Long clienteId, Pageable pageable) {
        return repository.filtrar(status, prioridade, clienteId, pageable).map(ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listarPorCliente(Long clienteId, Pageable pageable) {
        clienteService.buscarEntity(clienteId);
        return listar(null, null, clienteId, pageable);
    }

    @Transactional(readOnly = true)
    public ChamadoDetalheResponse buscar(Long id) {
        return ChamadoDetalheResponse.de(buscarEntity(id));
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest dto) {
        Chamado chamado = buscarAberto(id);
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(clienteService.buscarEntity(dto.clienteId()));
        chamado.setCategoria(categoriaService.buscarEntity(dto.categoriaId()));
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscarEntity(id));
    }

    @Transactional
    public ChamadoResponse atribuir(Long id, Long tecnicoId) {
        Chamado chamado = buscarAberto(id);
        Tecnico tecnico = tecnicoService.buscarEntity(tecnicoId);
        chamado.setTecnico(tecnico);
        // Ao receber um técnico, o chamado ABERTO passa a estar em atendimento.
        if (chamado.getStatus() == StatusChamado.ABERTO) {
            chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        }
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse alterarStatus(Long id, StatusChamado novo) {
        Chamado chamado = buscarAberto(id);
        StatusChamado atual = chamado.getStatus();
        if (!atual.podeMudarPara(novo)) {
            throw new RegraNegocioException(
                "Transição de status não permitida: " + atual + " -> " + novo);
        }
        if (novo == StatusChamado.EM_ATENDIMENTO && chamado.getTecnico() == null) {
            throw new RegraNegocioException(
                "Atribua um técnico antes de colocar o chamado em atendimento");
        }
        chamado.setStatus(novo);
        if (novo == StatusChamado.FECHADO) {
            chamado.setDataFechamento(LocalDateTime.now());
        }
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ComentarioResponse comentar(Long id, ComentarioRequest dto) {
        Chamado chamado = buscarAberto(id);
        Comentario comentario = new Comentario(dto.texto(), dto.autor(), LocalDateTime.now(), chamado);
        chamado.getComentarios().add(comentario);
        return ComentarioResponse.de(comentarioRepository.save(comentario));
    }

    @Transactional(readOnly = true)
    public List<ComentarioResponse> listarComentarios(Long id) {
        buscarEntity(id);
        return comentarioRepository.findByChamadoIdOrderByDataAsc(id).stream()
            .map(ComentarioResponse::de).toList();
    }

    /** Busca o chamado garantindo que ele ainda pode ser alterado. */
    private Chamado buscarAberto(Long id) {
        Chamado chamado = buscarEntity(id);
        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new ChamadoFechadoException(id);
        }
        return chamado;
    }

    private Chamado buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Chamado " + id + " não encontrado"));
    }
}
