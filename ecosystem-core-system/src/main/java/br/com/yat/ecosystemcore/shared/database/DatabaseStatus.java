package br.com.yat.ecosystemcore.shared.database;

public record DatabaseStatus(boolean available, String message, String details) {

	public static DatabaseStatus ok() {
		return new DatabaseStatus(true, "BANCO DE DADOS CONECTADO!", "POOL ATIVO E SAUDÁVEL");
	}

	public static DatabaseStatus error(String userMessage) {
		return new DatabaseStatus(false, userMessage, "INDISPONÍVEL");
	}
	
	public boolean isAvailable() {
        return available;
    }

    public String getMessage() {
        return message;
    }
}
