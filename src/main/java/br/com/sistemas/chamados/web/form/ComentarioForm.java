package br.com.sistemas.chamados.web.form;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ComentarioForm {

    @NotBlank(message = "Texto é obrigatório")
    @Size(max = 1000, message = "Texto deve ter no máximo 1000 caracteres")
    private String texto;

    @NotBlank(message = "Autor é obrigatório")
    @Size(max = 100, message = "Autor deve ter no máximo 100 caracteres")
    private String autor;

    public ComentarioRequest toRequest() {
        return new ComentarioRequest(texto, autor);
    }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
}
