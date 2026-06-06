package br.com.yat.ecosystemcore.application.system.dto;

import java.time.LocalDateTime;

public record JobExecucaoDTO(Long id, String tipoJob, String status, LocalDateTime inicio, LocalDateTime fim, String erroMensagem) {}
