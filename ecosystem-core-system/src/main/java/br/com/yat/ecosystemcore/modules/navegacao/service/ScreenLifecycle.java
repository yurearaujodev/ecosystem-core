package br.com.yat.ecosystemcore.modules.navegacao.service;

public interface ScreenLifecycle {
	void onShow();
    void onHide();
    void onDestroy();
}
