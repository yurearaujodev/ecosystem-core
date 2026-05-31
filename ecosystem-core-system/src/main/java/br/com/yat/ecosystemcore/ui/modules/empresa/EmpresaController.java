package br.com.yat.ecosystemcore.ui.modules.empresa;


import br.com.yat.ecosystemcore.ui.core.ScreenLifecycle;

/**
 * Controlador de escopo local para o módulo de Empresas (antigo TenantController).
 */
public class EmpresaController implements ScreenLifecycle {

    @Override
    public void onShow() {
        System.out.println("Tela de Empresas ganhou foco! Rodando SELECT automático...");
    }

    @Override
    public void onHide() {
        System.out.println("O usuário mudou de tela. Liberando listeners locais...");
    }

    @Override
    public void onDestroy() {
        System.out.println("Tela removida do cache. Memória limpa com sucesso.");
    }
}
