//package br.com.yat.ecosystemcore.application.usuario;
//
//public interface PasswordEncoder {
//    String encode(String rawPassword);
//    boolean matches(String rawPassword, String encodedPassword);
//}
package br.com.yat.ecosystemcore.application.usuario;

public interface PasswordEncoder {
    String encode(char[] password);
    boolean matches(char[] password, String hash);
}