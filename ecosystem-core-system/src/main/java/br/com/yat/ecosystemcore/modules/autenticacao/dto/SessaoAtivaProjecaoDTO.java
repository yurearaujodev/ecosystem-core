package br.com.yat.ecosystemcore.modules.autenticacao.dto;

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
