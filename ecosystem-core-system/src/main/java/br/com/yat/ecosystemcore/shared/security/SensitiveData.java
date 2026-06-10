package br.com.yat.ecosystemcore.shared.security;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Classe utilitária para manipulação segura de dados sensíveis em memória.
 * <p>
 * Responsável por:
 * <ul>
 * <li>Limpar buffers de dados sensíveis ({@code byte[]}, {@code char[]}, {@link ByteBuffer}).</li>
 * <li>Converter {@link String} em {@code char[]} para manipulação segura.</li>
 * </ul>
 * </p>
 * 
 * <p>Integra:
 * <ul>
 * <li>{@link Arrays} para sobrescrever dados..</li>
 * <li>{@link ByteBuffer} para manipulação de buffers diretos.</li>
 * <li><b>SLF4J</b> para logging.</li>
 * </ul>
 * </p>
 * 
 * <p>Não deve ser instanciada.</p>
 */
public final class SensitiveData {
	private static final Logger logger = LoggerFactory.getLogger(SensitiveData.class);

	/**
	 * Construtor privado para evitar instanciação.
	 */
	private SensitiveData() {
		throw new AssertionError("Classe Utilitária não deve ser instanciada");
	}

	/**
	 * Limpa de forma segura um array de bytes, sobrescrevendo todos os elementos com zero.
	 * 
	 * @param data array de bytes a ser limpo
	 */
	public static void safeClear(byte[] data) {
		if (data != null && data.length > 0) {
			Arrays.fill(data, (byte) 0);
			logger.debug("Buffer sensível (byte[]) limpo com sucesso");
		}
	}

	/**
	 * Limpa de forma segura um array de caracteres, sobrescrevendo todos os elementos com {@code '0'}.
	 * 
	 * @param data array de caracteres a ser limpo
	 */
	public static void safeClear(char[] data) {
		if (data != null && data.length > 0) {
			Arrays.fill(data, '\0');
			logger.debug("Buffer sensível (char[]) limpo com sucesso");
		}
	}

	/**
	 * Limpa de forma segura um {@link ByteBuffer}, sobrescrevendo todos os elementos com zero.
	 * 
	 * @param buffer buffer a ser limpo
	 */
	public static void safeClear(ByteBuffer buffer) {
		if (buffer == null) {
			return;
		}
		buffer.clear();

		while (buffer.hasRemaining()) {
			buffer.put((byte) 0);
		}

		buffer.clear();
		logger.debug("Buffer sensível (ByteBuffer) limpo com sucesso");
	}

	/**
	 * Converte uma {@link String} em um array de caracteres.
	 * <p>
	 * Retorna um array vazio se a string for {@code null}.
	 * </p>
	 * 
	 * @param input string de entrada
	 * @return array de caracteres correspondente ou vazio se nulo
	 */
	public static char[] toCharArray(String input) {
		return input != null ? input.toCharArray() : new char[0];
	}

}
