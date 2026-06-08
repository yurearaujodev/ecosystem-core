package br.com.yat.ecosystemcore.shared.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public final class TokenGenerator {

	private static final SecureRandom RANDOM = new SecureRandom();

	private TokenGenerator() {
	}

	public static String generateRefreshToken() {

		byte[] bytes = new byte[32];

		RANDOM.nextBytes(bytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static String generateSessionId() {

		return UUID.randomUUID().toString().replace("-", "");
	}
}
