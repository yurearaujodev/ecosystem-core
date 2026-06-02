module br.com.yat.ecosystemcore {
    
    // Exige os módulos vitais do JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    
    // Exige as outras dependências do seu pom.xml
    requires java.sql;
    requires com.zaxxer.hikari;
    requires org.slf4j;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
    requires googleauth;

    // EXPORTS: Garante acesso à classe App que está no bootstrap
    exports br.com.yat.ecosystemcore.bootstrap;
    // Adicionado para permitir que outros módulos vejam as classes de banco se necessário
    exports br.com.yat.ecosystemcore.ui.modules.banco; 
    
    exports br.com.yat.ecosystemcore.ui.core;
    
    exports br.com.yat.ecosystemcore.domain.enums;
    
    // OPENS: Abre APENAS as pastas que realmente contêm arquivos e controllers
    opens br.com.yat.ecosystemcore.bootstrap to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.menu to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.core to javafx.fxml;
    opens br.com.yat.ecosystemcore.ui.modules.empresa to javafx.fxml;
    
    // 👇 ADICIONE ESTA LINHA AQUI: Liberando a nova tela de banco para o FXMLLoader funcionar
    opens br.com.yat.ecosystemcore.ui.modules.banco to javafx.fxml;
}