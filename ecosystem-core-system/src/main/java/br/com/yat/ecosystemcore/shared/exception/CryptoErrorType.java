package br.com.yat.ecosystemcore.shared.exception;
/**
 * Enumeração que representa os diferentes tipos de erros criptográaficos.
 * <p>
 * Cada constante possui:
 * <ul>
 * <li>Um código único (<code>CRYPTO-XXX</code>).</li>
 * <li>Uma mensagem descritiva.</li>
 * <li>Um indicador de criticidade.</li>
 * </ul>
 * </p>
 * 
 * <p>Utilizada para padronizar tratamento de erros em operações de criptografia.</p>
 */
public enum CryptoErrorType {
	KEY_NOT_FOUND("CRYPTO-001", "Chave criptográfica não encontrada", true),
	KEY_INVALID("CRYPTO-002", "Chave criptográfica inválida", true),
	DATA_CORRUPTED("CRYPTO-003", "Dados criptografados corrompidos", true),
	ENCRYPTION_FAILED("CRYPTO-004", "Falha ao criptografar os dados", true),
	DECRYPTION_FAILED("CRYPTO-005", "Falha ao descriptografar os dados", true),
	SIGNATURE_FAILED("CRYPTO-006", "Falha na validação da assinatura digital", true),
	IO_ERROR("CRYPTO-007", "Erro de entrada/saída durante operação criptográfica", false),
	CONFIG_ERROR("CRYPTO-008", "Erro de configuração criptográfica", false),
	INTERNAL_ERROR("CRYPTO-999", "Erro interno inesperado", true);

	private final String code;
	private final String message;
	private final boolean critical;

	CryptoErrorType(String code, String message, boolean critical) {
		this.code = code;
		this.message = message;
		this.critical = critical;
	}

	/**
	 * Retorna o código único do erro criptográfico.
	 * 
	 * @return código do erro
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Retorna a mensagem descritiva do erro criptográfico.
	 * 
	 * @return mensagem do erro
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Indica se o erro é crítico.
	 * 
	 * @return {@code true} se crítico, {@code false} caso contrário
	 */
	public boolean isCritical() {
		return critical;
	}

	/**
	 * Retorna representação textual do erro no formato:
	 * {@code CODIGO - MENSAGEM}.
	 * 
	 * @return string representando o erro
	 */
	@Override
	public String toString() {
		return code + " - " + message;
	}

}
