package br.com.yat.ecosystemcore.shared.exception;

public final class CryptoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final CryptoErrorType errorType;

	/**
	 * Constrói exceção com tipo de erro.
	 * 
	 * @param errorType tipo de erro criptográfico
	 */
	public CryptoException(CryptoErrorType errorType) {
		super(resolveMessage(errorType));
		this.errorType = resolveErrorType(errorType);
	}

	/**
	 * Constrói exceção com tipo de erro e mensagem.
	 * 
	 * @param errorType tipo de ero criptográfico
	 * @param message mensagem descritiva
	 */
	public CryptoException(CryptoErrorType errorType, String message) {
		super(message != null ? message : resolveMessage(errorType));
		this.errorType = resolveErrorType(errorType);
	}

	/**
	 * Constrói exceção com tipo de erro e causa.
	 * 
	 * @param errorType tipo de erro criptográfico
	 * @param cause causa original
	 */
	public CryptoException(CryptoErrorType errorType, Throwable cause) {
		super(resolveMessage(errorType), cause);
		this.errorType = resolveErrorType(errorType);
	}

	/**
	 * Contrói exceção com tipo de erro, mensagem e causa.
	 * 
	 * @param errorType tipo de erro criptográfico
	 * @param message mensagem descritiva
	 * @param cause causa original
	 */
	public CryptoException(CryptoErrorType errorType, String message, Throwable cause) {
		super(message != null ? message : resolveMessage(errorType), cause);
		this.errorType = resolveErrorType(errorType);
	}

	/**
	 * Contrói exceção com mensagem, assumindo tipo {@link CryptoErrorType#INTERNAL_ERROR}.
	 * 
	 * @param message mensagem descritiva
	 */
	public CryptoException(String message) {
		super(message);
		this.errorType = CryptoErrorType.INTERNAL_ERROR;
	}

	/**
	 * Contrói exceção com mensagem e causa, assumindo tipo {@link CryptoErrorType#INTERNAL_ERROR}.
	 * 
	 * @param message mensagem descritiva
	 * @param cause causa original
	 */
	public CryptoException(String message, Throwable cause) {
		super(message, cause);
		this.errorType = CryptoErrorType.INTERNAL_ERROR;
	}

	/**
	 * Constrói exceção com causa, assumindo tipo {@link CryptoErrorType#INTERNAL_ERROR}.
	 * 
	 * @param cause causa original
	 */
	public CryptoException(Throwable cause) {
		super(cause);
		this.errorType = CryptoErrorType.INTERNAL_ERROR;
	}

	/**
	 * Retorna o tipo de erro criptográfico associado.
	 * 
	 * @return tipo de erro
	 */
	public CryptoErrorType getErrorType() {
		return errorType;
	}

	/**
	 * Retorna o código do erro criptográfico.
	 * 
	 * @return código do erro
	 */
	public String getErrorCode() {
		return errorType.getCode();
	}

	/**
	 * Indica se o erro é crítico.
	 * 
	 * @return {@code true} se crítico, {@code false} caso contrário
	 */
	public boolean isCritical() {
		return errorType.isCritical();
	}

	private static CryptoErrorType resolveErrorType(final CryptoErrorType type) {
		return type != null ? type : CryptoErrorType.INTERNAL_ERROR;
	}

	private static String resolveMessage(final CryptoErrorType type) {
		return type != null ? type.getMessage() : CryptoErrorType.INTERNAL_ERROR.getMessage();
	}

}
