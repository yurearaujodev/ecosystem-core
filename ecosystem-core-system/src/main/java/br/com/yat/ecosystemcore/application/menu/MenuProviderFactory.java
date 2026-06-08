package br.com.yat.ecosystemcore.application.menu;

//import br.com.yat.ecosystemcore.repository.menu.MenuSistemaRepository; // Ajuste para o seu pacote correto

public final class MenuProviderFactory {

    private MenuProviderFactory() {}

    public static MenuProvider create() {
        // 🔄 DEFAUTE DEFINITIVO: Troca o Stub pela implementação dinâmica do Banco de Dados
        return new MenuProviderJdbc(); 
    }
}