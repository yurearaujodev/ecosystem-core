package br.com.yat.ecosystemcore.application.system.dto;

import java.time.LocalDateTime;

public record SchemaVersionDTO(Long id, String versao, String descricao, String executadoPor, LocalDateTime createdAt) {}
