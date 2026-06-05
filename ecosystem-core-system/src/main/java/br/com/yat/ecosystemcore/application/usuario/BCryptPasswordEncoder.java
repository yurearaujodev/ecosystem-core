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
            // 💡 Correção: Convertemos os bytes limpos em uma string temporária apenas no milissegundo do hash,
            // ou usamos o método nativo se a sua biblioteca suportar.
            // Para garantir compatibilidade universal com jBCrypt:
            String passwordStrTmp = new String(passwordBytes, StandardCharsets.UTF_8);
            String hash = BCrypt.hashpw(passwordStrTmp, salt);
            
            // Forçamos a perda de referência da string temporária para o Garbage Collector recolher
            passwordStrTmp = null; 
            
            return hash;
        } finally {
            // 🛡️ ENCODER É O DONO DA LIMPEZA: Destrói os buffers imediatamente
            Arrays.fill(passwordBytes, (byte) 0);
            Arrays.fill(password, '\0');
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
            // 🛡️ ENCODER É O DONO DA LIMPEZA: Garante a higienização no fluxo de login
            Arrays.fill(passwordBytes, (byte) 0);
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Converte char[] para byte[] via Charset sem gerar objetos String persistentes na Heap.
     */
    private byte[] toBytesDirect(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        
        // Captura apenas os bytes válidos gerados
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        
        // Limpa o array interno do ByteBuffer temporário se ele for acessível
        if (byteBuffer.hasArray()) {
            Arrays.fill(byteBuffer.array(), (byte) 0);
        }
        return bytes;
    }
}
//package br.com.yat.ecosystemcore.application.usuario;
//
//import org.mindrot.jbcrypt.BCrypt;
//
//public final class BCryptPasswordEncoder implements PasswordEncoder {
//
//    private static final int WORKLOAD = 12;
//
//    @Override
//    public String encode(String rawPassword) {
//        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(WORKLOAD));
//    }
//
//    @Override
//    public boolean matches(String rawPassword, String encodedPassword) {
//        return BCrypt.checkpw(rawPassword, encodedPassword);
//    }
//}