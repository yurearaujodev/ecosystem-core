package br.com.yat.ecosystemcore.application.system.dto;

public record SetupEcosystemCommand(
    String nomePlataforma,
    String ambiente,
    String versaoCore
) {}