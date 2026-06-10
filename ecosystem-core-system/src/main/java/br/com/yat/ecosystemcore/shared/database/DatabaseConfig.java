package br.com.yat.ecosystemcore.shared.database;

import java.net.URI;
import java.util.Objects;

import br.com.yat.ecosystemcore.shared.exception.DatabaseValidationException;

public final class DatabaseConfig {

    private final String ip;
    private final String porta;
    private final String banco;
    private final String usuario;

    private DatabaseConfig(String ip, String porta, String banco, String usuario) {
        this.ip = ip;
        this.porta = porta;
        this.banco = banco;
        this.usuario = usuario;
    }

    public static DatabaseConfig criarAPartirDeFormulario(String ip, String porta, String banco, String usuario) {
        Objects.requireNonNull(ip, "IP obrigatório.");
        Objects.requireNonNull(porta, "Porta obrigatória.");
        Objects.requireNonNull(banco, "Nome do banco obrigatório.");
        Objects.requireNonNull(usuario, "Usuário obrigatório.");

        validarFormatos(ip, porta, banco, usuario);
        return new DatabaseConfig(ip.trim(), porta.trim(), banco.trim(), usuario.trim());
    }

    public static DatabaseConfig reconstruirDoArquivo(String url, String usuario) {
        Objects.requireNonNull(url, "URL não pode ser nula.");
        Objects.requireNonNull(usuario, "Usuário não pode ser nulo.");

        try {
            // Remove o prefixo "jdbc:" temporariamente para que a classe URI consiga processar
            String uriString = url.substring(5);
            URI uri = URI.create(uriString);

            String ip = uri.getHost();
            String porta = String.valueOf(uri.getPort());

            String path = uri.getPath();
            String banco = (path != null && path.startsWith("/")) ? path.substring(1) : path;
            if (banco != null && banco.endsWith("/")) {
                banco = banco.substring(0, banco.length() - 1);
            }

            if (ip == null || uri.getPort() == -1 || banco == null || banco.isBlank()) {
                throw new DatabaseValidationException("Formato de URL JDBC inválido no arquivo de configuração.");
            }

            validarFormatos(ip, porta, banco, usuario);
            return new DatabaseConfig(ip, porta, banco, usuario.trim());
        } catch (Exception e) {
            throw new DatabaseValidationException("Falha ao processar a URL do banco: " + e.getMessage(), e);
        }
    }

    private static void validarFormatos(String ip, String porta, String banco, String usuario) {
        if (ip.isBlank()) throw new DatabaseValidationException("O endereço IP não pode estar vazio.");
        if (banco.isBlank()) throw new DatabaseValidationException("O nome do banco não pode estar vazio.");
        if (usuario.isBlank()) throw new DatabaseValidationException("O usuário não pode estar vazio.");
        
        try {
            int p = Integer.parseInt(porta.trim());
            if (p < 1 || p > 65535) {
                throw new DatabaseValidationException("Porta deve estar no intervalo válido entre 1 e 65535.");
            }
        } catch (NumberFormatException e) {
            throw new DatabaseValidationException("Porta deve conter um valor numérico válido.");
        }
    }

    public String gerarJdbcUrl() {
        return "jdbc:mysql://" + ip + ":" + porta + "/" + banco;
    }

    public String ip() { return ip; }
    public String porta() { return porta; }
    public String banco() { return banco; }
    public String usuario() { return usuario; }
}

