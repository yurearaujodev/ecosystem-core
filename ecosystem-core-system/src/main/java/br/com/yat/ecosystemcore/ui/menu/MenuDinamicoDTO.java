package br.com.yat.ecosystemcore.ui.menu;

/**
 * Modelo DTO alimentado pelo repositório que lê as permissões de menu do banco.
 */
public class MenuDinamicoDTO {
    private String categoria; 
    private String submenuNome;
    private String chaveEnumString; 

    public MenuDinamicoDTO(String categoria, String submenuNome, String chaveEnumString) {
        this.categoria = categoria;
        this.submenuNome = submenuNome;
        this.chaveEnumString = chaveEnumString;
    }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getSubmenuNome() { return submenuNome; }
    public void setSubmenuNome(String submenuNome) { this.submenuNome = submenuNome; }

    public String getChaveEnumString() { return chaveEnumString; }
    public void setChaveEnumString(String chaveEnumString) { this.chaveEnumString = chaveEnumString; }
}