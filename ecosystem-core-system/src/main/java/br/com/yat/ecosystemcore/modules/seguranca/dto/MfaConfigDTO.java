package br.com.yat.ecosystemcore.modules.seguranca.dto;

public record MfaConfigDTO(
    Long usuarioId,
    String secretBase32,
    String qrCodeUrl,
    boolean ativo
) {}