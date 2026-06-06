package br.com.yat.ecosystemcore.domain.dto;

import java.time.LocalDateTime;

public record SessaoAtivaProjecaoDTO(
    String sessaoId,
    String nomeUsuario,
    String emailUsuario,
    String nomeEmpresa,
    String ipOrigem,
    String dispositivoInfo,
    LocalDateTime criadoEm,
    LocalDateTime expiraEm
) {}
