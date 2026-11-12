package br.com.sistemas.chamados.web.form;

import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChamadoForm {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 100, message = "Título deve ter no máximo 100 caracteres")
    private String titulo;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    private String descricao;

    @NotNull(message = "Prioridade é obrigatória")
    private Prioridade prioridade;

    @NotNull(message = "Cliente é obrigatório")
    private Long clienteId;

    @NotNull(message = "Categoria é obrigatória")
    private Long categoriaId;

    public static ChamadoForm de(ChamadoResponse c) {
        ChamadoForm form = new ChamadoForm();
        form.setTitulo(c.titulo());
        form.setDescricao(c.descricao());
        form.setPrioridade(c.prioridade());
        form.setClienteId(c.clienteId());
        form.setCategoriaId(c.categoriaId());
        return form;
    }

    public ChamadoRequest toRequest() {
        return new ChamadoRequest(titulo, descricao, prioridade, clienteId, categoriaId);
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Prioridade getPrioridade() { return prioridade; }
    public void setPrioridade(Prioridade prioridade) { this.prioridade = prioridade; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
}
