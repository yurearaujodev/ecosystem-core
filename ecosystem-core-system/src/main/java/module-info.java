module br.com.yat.ecosystemcore {
    
    // Exige os módulos vitais do JavaFX
    requires javafx.controls;
    requires javafx.fxml;
   // requires javafx.graphics;
    requires transitive javafx.graphics;
    
    // Exige as outras dependências do seu pom.xml
    requires java.sql;
    requires com.zaxxer.hikari;
    requires org.slf4j;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
    requires googleauth;
    requires jbcrypt;
    requires java.net.http;
    
    // 🌟 REQUISITO DO MOTOR DO QR CODE ADICIONADO PERFEITAMENTE
    requires com.google.zxing;
	requires com.github.benmanes.caffeine;
	requires javafx.base;

    // EXPORTS: Garante acesso à classe App que está no bootstrap
    exports br.com.yat.ecosystemcore.app;
    exports br.com.yat.ecosystemcore.modules.navegacao.entity;
    exports br.com.yat.ecosystemcore.modules.autenticacao.service;
    
    // LIBERA O PACOTE SYSTEM PARA COMPILAÇÃO
    exports br.com.yat.ecosystemcore.modules.system;
    
    // OPENS: Abre as pastas que contêm arquivos e controllers para o JavaFX
  //  opens br.com.yat.ecosystemcore.bootstrap to javafx.fxml;
    opens br.com.yat.ecosystemcore.modules.cadastro.ui to javafx.fxml;
    opens br.com.yat.ecosystemcore.modules.autenticacao.ui to javafx.fxml;
    opens br.com.yat.ecosystemcore.modules.tenant to javafx.fxml;
    opens br.com.yat.ecosystemcore.modules.seguranca.ui to javafx.fxml;
    
    // LIBERA A REFLEXÃO DO FXMLLOADER PARA OS SEUS CONTROLADORES
    opens br.com.yat.ecosystemcore.modules.system to javafx.fxml;
    opens br.com.yat.ecosystemcore.modules.home.ui to javafx.fxml;
    opens br.com.yat.ecosystemcore.modules.sistema.ui to javafx.fxml;
    opens br.com.yat.ecosystemcore.modules.usuario.ui to javafx.fxml;
    opens br.com.yat.ecosystemcore.modules.navegacao.ui to javafx.fxml;
    opens br.com.yat.ecosystemcore.modules.autorizacao.ui to javafx.fxml;    

    // =========================================================================
    // 🔑 CORREÇÃO DO ERRO: ABRE O DOMÍNIO PARA A TABLEVIEW DO JAVAFX RENDEREZAR
    // =========================================================================
    opens br.com.yat.ecosystemcore.domain.entity to javafx.base;
    opens br.com.yat.ecosystemcore.modules.usuario.entity to javafx.base;
    opens br.com.yat.ecosystemcore.modules.cadastro.entity to javafx.base;
    opens br.com.yat.ecosystemcore.modules.usuario.dto to javafx.base;
    opens br.com.yat.ecosystemcore.modules.seguranca.entity to javafx.base;
}