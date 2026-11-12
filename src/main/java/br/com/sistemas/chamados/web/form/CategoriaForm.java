package br.com.sistemas.chamados.web.form;

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoriaForm {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 60, message = "Nome deve ter no máximo 60 caracteres")
    private String nome;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String descricao;

    public static CategoriaForm de(CategoriaResponse c) {
        CategoriaForm form = new CategoriaForm();
        form.setNome(c.nome());
        form.setDescricao(c.descricao());
        return form;
    }

    public CategoriaRequest toRequest() {
        return new CategoriaRequest(nome, descricao);
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
