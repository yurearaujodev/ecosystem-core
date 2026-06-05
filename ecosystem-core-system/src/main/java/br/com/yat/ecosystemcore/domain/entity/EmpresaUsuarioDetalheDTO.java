package br.com.yat.ecosystemcore.domain.entity;

public class EmpresaUsuarioDetalheDTO {
    
    private Long empresaId;
    private String empresaNome;
    private Long perfilId;
    private String perfilNome;

    public EmpresaUsuarioDetalheDTO(Long empresaId, String empresaNome, Long perfilId, String perfilNome) {
        this.empresaId = empresaId;
        this.empresaNome = empresaNome;
        this.perfilId = perfilId;
        this.perfilNome = perfilNome;
    }
    
    public EmpresaUsuarioDetalheDTO() {
    }

    // Getters e Setters para o JavaFX conseguir ler os valores via PropertyValueFactory
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

    public String getEmpresaNome() { return empresaNome; }
    public void setEmpresaNome(String empresaNome) { this.empresaNome = empresaNome; }

    public Long getPerfilId() { return perfilId; }
    public void setPerfilId(Long perfilId) { this.perfilId = perfilId; }

    public String getPerfilNome() { return perfilNome; }
    public void setPerfilNome(String perfilNome) { this.perfilNome = perfilNome; }
    
    @Override
    public String toString() {
        return this.empresaNome;
    }
}
