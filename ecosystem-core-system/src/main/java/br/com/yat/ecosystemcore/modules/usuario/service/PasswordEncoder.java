package br.com.yat.ecosystemcore.modules.usuario.service;

public interface PasswordEncoder {
	String encode(char[] password);

	boolean matches(char[] password, String hash);
}