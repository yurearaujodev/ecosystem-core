package br.com.yat.ecosystemcore.application.usuario;

import org.mindrot.jbcrypt.BCrypt;

public final class BCryptPasswordEncoder implements PasswordEncoder {

    private static final int WORKLOAD = 12;

    @Override
    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(WORKLOAD));
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}