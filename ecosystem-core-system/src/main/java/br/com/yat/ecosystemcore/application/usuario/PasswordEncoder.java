package br.com.yat.ecosystemcore.application.usuario;

public interface PasswordEncoder {
	String encode(char[] password);

	boolean matches(char[] password, String hash);
}