package br.com.yat.ecosystemcore.ui.modules.home;

import br.com.yat.ecosystemcore.ui.core.ScreenLifecycle;
import br.com.yat.ecosystemcore.infrastructure.security.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController implements ScreenLifecycle {

    @FXML private Label lblMensagemHome; // Garanta que tem esse ID no seu HomeView.fxml se quiser testar o texto

    @Override
    public void onShow() {
        System.out.println("====== VERIFICAÇÃO DE SESSÃO ESTÁVEL ======");
        if (SessionManager.getUsuarioLogado() != null) {
            System.out.println("Usuário Conectado: " + SessionManager.getUsuarioLogado().getEmail());
            System.out.println("Tenant ID Ativo: " + SessionManager.getTenantAtual().getId());
            
            if (lblMensagemHome != null) {
                lblMensagemHome.setText("Sessão ativa para: " + SessionManager.getUsuarioLogado().getEmail());
            }
        } else {
            System.err.println("⚠️ Alerta: Sessão perdida ou nula!");
        }
        System.out.println("===========================================");
    }

    @Override
    public void onHide() {}

    @Override
    public void onDestroy() {}
}