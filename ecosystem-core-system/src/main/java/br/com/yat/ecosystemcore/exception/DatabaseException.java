package br.com.yat.ecosystemcore.exception;

public class DatabaseException extends RuntimeException {
 
	private static final long serialVersionUID = 5922739861582862252L;

	public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
