package br.com.yat.ecosystemcore.application.system.dto;

public record MfaConfigDTO(
    Long usuarioId,
    String secretBase32,
    String qrCodeUrl,
    boolean ativo
) {}