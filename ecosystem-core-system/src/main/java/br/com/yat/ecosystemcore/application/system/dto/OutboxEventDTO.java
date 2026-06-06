package br.com.yat.ecosystemcore.application.system.dto;

import java.time.LocalDateTime;

public record OutboxEventDTO(Long id, String tenantId, String eventoTipo, String payload, Integer processado, LocalDateTime criadoEm) {}
