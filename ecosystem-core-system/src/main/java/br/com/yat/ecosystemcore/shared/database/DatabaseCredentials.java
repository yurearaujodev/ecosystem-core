package br.com.yat.ecosystemcore.shared.database;

import java.util.Arrays;
import java.util.Objects;

import br.com.yat.ecosystemcore.shared.security.SensitiveData;

public final class DatabaseCredentials implements AutoCloseable {
	private char[] senha;
	private boolean destruido = false;

	public DatabaseCredentials(char[] senhaOriginal) {
		Objects.requireNonNull(senhaOriginal, "A senha não pode ser nula.");

		this.senha = senhaOriginal.clone();

		SensitiveData.safeClear(senhaOriginal);
	}

	public void executarComSenha(SenhaConsumer consumer) throws Exception {
		if (destruido) {
			throw new IllegalStateException("Credenciais já foram destruídas.");
		}
		consumer.accept(this.senha);
	}

	@Override
	public void close() {
		if (!destruido) {
			if (this.senha != null) {
				Arrays.fill(this.senha, '\0');
				this.senha = null;
			}
			this.destruido = true;
		}
	}

	@Override
	public String toString() {
		return "DatabaseCredentials[PROTECTED]";
	}

	@FunctionalInterface
	public interface SenhaConsumer {
		void accept(char[] senhaAtiva) throws Exception;
	}
}
