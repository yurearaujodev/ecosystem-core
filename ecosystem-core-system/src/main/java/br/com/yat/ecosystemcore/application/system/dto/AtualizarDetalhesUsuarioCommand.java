package br.com.yat.ecosystemcore.application.system.dto;

import java.util.List;

import br.com.yat.ecosystemcore.modules.usuario.dto.EmpresaUsuarioDetalheDTO;

public record AtualizarDetalhesUsuarioCommand(
    Long usuarioId,
    List<EmpresaUsuarioDetalheDTO> vinculosEmpresas,
    List<Long> idsPermissoesExtras,
    UsuarioSegurancaConfigDTO segurancaConfig
) {}