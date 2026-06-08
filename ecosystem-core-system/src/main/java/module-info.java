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

    // EXPORTS: Garante acesso à classe App que está no bootstrap
    exports br.com.yat.ecosystemcore.app;
    exports br.com.yat.ecosystemcore.ui.modules.banco; 
    exports br.com.yat.ecosystemcore.ui.core;
    exports br.com.yat.ecosystemcore.domain.enums;
    
    // LIBERA O PACOTE SYSTEM PARA COMPILAÇÃO
    exports br.com.yat.ecosystemcore.ui.modules.system;
    
    // OPENS: Abre as pastas que contêm arquivos e controllers para o JavaFX
  //  opens br.com.yat.ecosystemcore.bootstrap to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.menu to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.core to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.modules.empresa to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.modules.login to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.modules.tenant to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.modules.banco to javafx.fxml;
    
    // LIBERA A REFLEXÃO DO FXMLLOADER PARA OS SEUS CONTROLADORES
    opens br.com.yat.ecosystemcore.ui.modules.system to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.modules.home to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.modules.pessoa to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.modules.usuario to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.modules.perfil to javafx.fxml;

    // =========================================================================
    // 🔑 CORREÇÃO DO ERRO: ABRE O DOMÍNIO PARA A TABLEVIEW DO JAVAFX RENDEREZAR
    // =========================================================================
    opens br.com.yat.ecosystemcore.domain.entity to javafx.base;
}