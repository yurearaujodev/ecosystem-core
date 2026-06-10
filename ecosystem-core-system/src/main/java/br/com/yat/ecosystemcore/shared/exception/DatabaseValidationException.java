package br.com.yat.ecosystemcore.shared.exception;

public class DatabaseValidationException extends RuntimeException {
    
	private static final long serialVersionUID = 2596793868998971634L;

	public DatabaseValidationException(String message) {
        super(message);
    }

    public DatabaseValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
