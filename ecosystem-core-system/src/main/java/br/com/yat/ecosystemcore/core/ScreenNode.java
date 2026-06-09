package br.com.yat.ecosystemcore.core;

import javafx.scene.Parent;

public final class ScreenNode {

    private final Parent view;
    private final Object controller;

    public ScreenNode(Parent view, Object controller) {
        this.view = view;
        this.controller = controller;
    }

    public Parent getView() {
        return view;
    }

    public Object getController() {
        return controller;
    }
}