package br.com.yat.ecosystemcore.shared.context;

import java.util.Collections;
import java.util.Set;

public class UserContext {

	private final Long usuarioId;
	private final String nome;
	private final String email;
	private final Set<String> permissoes;

	public UserContext(Long usuarioId, String nome, String email, Set<String> permissoes) {
		this.usuarioId = usuarioId;
		this.nome = nome;
		this.email = email;
		this.permissoes = permissoes != null ? permissoes : Collections.emptySet();
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public String getNome() {
		return nome;
	}

	public String getEmail() {
		return email;
	}
	
	public boolean temPermissao(String chavePermissao) {
		return permissoes.contains(chavePermissao);
	}
}