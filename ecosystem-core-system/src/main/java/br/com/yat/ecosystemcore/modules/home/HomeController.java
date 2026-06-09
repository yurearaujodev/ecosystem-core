package br.com.yat.ecosystemcore.modules.home;

import br.com.yat.ecosystemcore.shared.context.SessionScope;
import br.com.yat.ecosystemcore.core.ScreenLifecycle;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController implements ScreenLifecycle {

    @FXML private Label lblMensagemHome;

    @Override
    public void onShow() {
        System.out.println("====== VERIFICAÇÃO DE SESSÃO ESTÁVEL ======");
        
        // 🔒 Verificação baseada no novo escopo central de segurança
        if (SessionScope.isActive() && SessionScope.usuario() != null) {
            System.out.println("Usuário Conectado: " + SessionScope.usuario().getEmail());
            System.out.println("Tenant ID Ativo: " + SessionScope.tenant().getId());
            
            if (SessionScope.empresa() != null) {
                System.out.println("Empresa Ativa ID: " + SessionScope.empresa().getId());
            } else {
                System.out.println("Empresa Ativa ID: [Nenhuma Empresa Padrão Selecionada]");
            }
            
            if (lblMensagemHome != null) {
                lblMensagemHome.setText("Sessão ativa para: " + SessionScope.usuario().getEmail());
            }
        } else {
            System.err.println("⚠️ Alerta Crítico: Sessão perdida, nula ou inválida no SessionScope!");
        }
        System.out.println("===========================================");
    }

    @Override
    public void onHide() {}

    @Override
    public void onDestroy() {}
}