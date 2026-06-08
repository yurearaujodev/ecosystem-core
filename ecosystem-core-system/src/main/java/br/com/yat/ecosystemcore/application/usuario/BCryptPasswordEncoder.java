package br.com.yat.ecosystemcore.application.usuario;

import org.mindrot.jbcrypt.BCrypt;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BCryptPasswordEncoder implements PasswordEncoder {

	@Override
	public String encode(char[] password) {
		if (password == null || password.length == 0) {
			throw new IllegalArgumentException("Senha não pode estar vazia.");
		}

		byte[] passwordBytes = toBytesDirect(password);
		try {
			String salt = BCrypt.gensalt(12);
			String passwordStrTmp = new String(passwordBytes, StandardCharsets.UTF_8);
			String hash = BCrypt.hashpw(passwordStrTmp, salt);

			passwordStrTmp = null;

			return hash;
		} finally {
			Arrays.fill(passwordBytes, (byte) 0);
		}
	}

	@Override
	public boolean matches(char[] password, String hash) {
		if (hash == null || hash.isBlank() || password == null || password.length == 0) {
			return false;
		}

		byte[] passwordBytes = toBytesDirect(password);
		try {
			String passwordStrTmp = new String(passwordBytes, StandardCharsets.UTF_8);
			boolean result = BCrypt.checkpw(passwordStrTmp, hash);

			passwordStrTmp = null;
			return result;
		} finally {
			Arrays.fill(passwordBytes, (byte) 0);
		}
	}

	private byte[] toBytesDirect(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);

		byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);

		if (byteBuffer.hasArray()) {
			Arrays.fill(byteBuffer.array(), (byte) 0);
		}
		return bytes;
	}
}