package br.com.yat.ecosystemcore.application.menu;

import br.com.yat.ecosystemcore.application.menu.dto.MenuPermitidoDTO;
import br.com.yat.ecosystemcore.domain.enums.MenuChave;

import java.util.ArrayList;
import java.util.List;

/**
 * Provisório até existir consulta JDBC. Espelha o catálogo semeado em {@code DatabaseMenuSeeder}.
 */
public final class MenuProviderStub implements MenuProvider {

    @Override
    public List<MenuPermitidoDTO> carregarMenus(MenuUsuarioContext context) {
        List<MenuPermitidoDTO> itens = new ArrayList<>();

        itens.add(item("HOME", null, 5, null, "Dashboard", MenuChave.HOME, 10));

        itens.add(item("CADASTROS", null, 20, null, "Empresas", MenuChave.CADASTROS_EMPRESA, 20));
        itens.add(item("CADASTROS", null, 20, null, "Pessoas", MenuChave.CADASTROS_PESSOA, 21));
        itens.add(item("CADASTROS", null, 20, null, "Usuários", MenuChave.CADASTROS_USUARIO, 22));

        itens.add(item("SEGURANÇA", null, 30, null, "Perfis", MenuChave.SEGURANCA_PERFIL, 30));
        itens.add(item("CONFIGURAÇÕES", null, 40, null, "Banco", MenuChave.CONFIGURACAO_BANCO, 42));
        itens.add(item("CONFIGURAÇÕES", null, 40, null, "Onboarding Tenant", MenuChave.ADMIN_TENANT_CONFIG, 43));

        return MenuPermitidoDTO.ordenarParaExibicao(itens);
    }

    private static MenuPermitidoDTO item(
            String moduloNome,
            String moduloIcone,
            int moduloOrdem,
            Long menuSistemaId,
            String menuNome,
            MenuChave chave,
            int menuOrdem
    ) {
        return new MenuPermitidoDTO(
                moduloNome,
                moduloIcone,
                moduloOrdem,
                menuSistemaId,
                menuNome,
                chave.name(),
                menuOrdem
        );
    }
}
