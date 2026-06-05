//package br.com.yat.ecosystemcore.application.system.dto;
//
//public record SetupEcosystemCommand(
//    String nomePlataforma,
//    String ambiente,
//    String versaoCore
//) {}
package br.com.yat.ecosystemcore.application.system.dto;

public record SetupEcosystemCommand(
    String nomePlataforma,
    String ambiente,
    String versaoCore,
    String razaoSocial,
    String nomeFantasia,
    String cnpj,
    String nomeAdmin,
    String emailAdmin,
    String senhaPura,
    String chavePerfil
) {}