package br.com.yat.ecosystemcore.application.system.dto;

import br.com.yat.ecosystemcore.domain.entity.EmpresaUsuarioDetalheDTO;
import java.util.List;

public record AtualizarDetalhesUsuarioCommand(
    Long usuarioId,
    List<EmpresaUsuarioDetalheDTO> vinculosEmpresas,
    List<Long> idsPermissoesExtras,
    UsuarioSegurancaConfigDTO segurancaConfig
) {}