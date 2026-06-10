package br.com.yat.ecosystemcore.modules.autorizacao.entity;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class CheckablePermissaoItem {
    private final Permissao permissao;
    private final String nomeExibicao;
    private final BooleanProperty selecionado = new SimpleBooleanProperty(false);

    // Construtor para nós raiz (Módulos/Agrupadores)
    public CheckablePermissaoItem(String nomeExibicao) {
        this.permissao = null;
        this.nomeExibicao = nomeExibicao;
    }

    // Construtor para nós folhas (Ações específicas)
    public CheckablePermissaoItem(Permissao permissao) {
        this.permissao = permissao;
        this.nomeExibicao = permissao.getAcao() + " (" + (permissao.getDescricao() != null ? permissao.getDescricao() : "") + ")";
    }

    public Permissao getPermissao() { return permissao; }
    public boolean isFolha() { return permissao != null; }
    public BooleanProperty selecionadoProperty() { return selecionado; }
    public boolean isSelecionado() { return selecionado.get(); }
    public void setSelecionado(boolean val) { this.selecionado.set(val); }

    @Override
    public String toString() {
        return nomeExibicao;
    }
}
