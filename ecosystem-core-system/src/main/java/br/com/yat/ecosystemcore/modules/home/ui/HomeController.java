package br.com.yat.ecosystemcore.modules.home.ui;

import br.com.yat.ecosystemcore.modules.navegacao.service.ScreenLifecycle;
import br.com.yat.ecosystemcore.shared.context.Sessao;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController implements ScreenLifecycle {

	@FXML 
	private Label lblMensagemHome;

	@Override
	public void onShow() {
		System.out.println("====== VERIFICAÇÃO DE SESSÃO ESTÁVEL ======");
		
		// 🔒 Validação centralizada e segura através da Facade de Sessão
		if (Sessao.isActive() && Sessao.user() != null) {
			
			System.out.println("Usuário Conectado: " + Sessao.user().getEmail());
			System.out.println("Nome do Usuário: " + Sessao.user().getNome());
			System.out.println("Tenant ID Ativo: " + Sessao.tenantId());
			
			if (Sessao.empresaId() != null) {
				System.out.println("Empresa Ativa ID: " + Sessao.empresaId());
			} else {
				System.out.println("Empresa Ativa ID: [Nenhuma Empresa Padrão Selecionada]");
			}
			
			if (lblMensagemHome != null) {
				// ✅ Corrigido o código incompleto/cortado
				lblMensagemHome.setText("Sessão ativa para: " + Sessao.user().getEmail());
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