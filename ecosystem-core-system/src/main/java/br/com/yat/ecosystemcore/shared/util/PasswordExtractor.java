package br.com.yat.ecosystemcore.shared.util;

import javafx.scene.control.PasswordField;
import java.util.Objects;

public final class PasswordExtractor {
    
    private PasswordExtractor() {}

    public static char[] extrair(PasswordField passwordField) {
        Objects.requireNonNull(passwordField, "O componente PasswordField não pode ser nulo.");
        
        CharSequence seq = passwordField.getCharacters();
        if (seq == null || seq.length() == 0) {
            return new char[0];
        }

        char[] senha = new char[seq.length()];
        for (int i = 0; i < seq.length(); i++) {
            senha[i] = seq.charAt(i);
        }
        return senha;
    }
}
