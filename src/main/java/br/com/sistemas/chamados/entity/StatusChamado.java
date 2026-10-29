package br.com.sistemas.chamados.entity;

/**
 * Ciclo de vida do chamado. Transições permitidas:
 * ABERTO -> EM_ATENDIMENTO | FECHADO
 * EM_ATENDIMENTO -> AGUARDANDO_CLIENTE | FECHADO
 * AGUARDANDO_CLIENTE -> EM_ATENDIMENTO | FECHADO
 * FECHADO -> (nenhuma)
 */
public enum StatusChamado {
    ABERTO,
    EM_ATENDIMENTO,
    AGUARDANDO_CLIENTE,
    FECHADO;

    public boolean podeMudarPara(StatusChamado novo) {
        return switch (this) {
            case ABERTO -> novo == EM_ATENDIMENTO || novo == FECHADO;
            case EM_ATENDIMENTO -> novo == AGUARDANDO_CLIENTE || novo == FECHADO;
            case AGUARDANDO_CLIENTE -> novo == EM_ATENDIMENTO || novo == FECHADO;
            case FECHADO -> false;
        };
    }
}
