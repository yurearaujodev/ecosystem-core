package br.com.yat.ecosystemcore.application.menu;

/**
 * Fábrica do provedor de menu. Troca de implementação centralizada para a futura leitura JDBC.
 */
public final class MenuProviderFactory {

    private MenuProviderFactory() {
    }

    public static MenuProvider create() {
        return new MenuProviderStub();
        // Futuro:
        // return new MenuProviderJdbc(new MenuSistemaRepositoryImpl());
    }
}
