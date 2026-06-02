package br.com.yat.ecosystemcore.repository.base;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class GenericDao<T, PK> {
    
    protected final String tableName;
    protected final String pkName;

    // Construtor Stateless: O DAO agora é leve e seguro para múltiplas threads
    public GenericDao(String tableName, String pkName) {
        this.tableName = tableName;
        this.pkName = pkName;
    }

    // Método abstrato obrigatório que cada filho implementará
    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;

    // =========================================================================
    // EXECUTORES DE SQL (Recebem a conexão obrigatoriamente para suportar transações)
    // =========================================================================

    protected void executeInsert(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParameters(stmt, params);
            stmt.executeUpdate();
        }
    }

    protected int executeUpdate(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParameters(stmt, params);
            return stmt.executeUpdate();
        }
    }

    protected List<T> executeQuery(Connection conn, String sql, Object... params) throws SQLException {
        List<T> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToEntity(rs));
                }
            }
        }
        return lista;
    }

    protected <R> Optional<R> executeQuerySingle(Connection conn, String sql, Function<ResultSet, R> mapper, Object... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(mapper.apply(rs));
                }
            }
        }
        return Optional.empty();
    }

    // =========================================================================
    // MÉTODOS CORE MULTI-TENANT (Com chaves genéricas e segurança de isolamento)
    // =========================================================================

    /**
     * Busca um registro garantindo isolamento estrito de Tenant.
     */
    public Optional<T> searchById(Connection conn, PK id, String tenantId) throws SQLException {
        String sql = "SELECT * FROM " + tableName + " WHERE " + pkName + " = ? AND tenant_id = ? AND deleted_at IS NULL";
        List<T> resultados = executeQuery(conn, sql, id, tenantId);
        return resultados.isEmpty() ? Optional.empty() : Optional.of(resultados.get(0));
    }

    /**
     * Soft Delete comercial atualizando a coluna padrão do seu novo script (deleted_at)
     */
    public boolean softDeleteById(Connection conn, PK id, String tenantId) throws SQLException {
        String sql = "UPDATE " + tableName + " SET deleted_at = CURRENT_TIMESTAMP WHERE " + pkName + " = ? AND tenant_id = ? AND deleted_at IS NULL";
        return executeUpdate(conn, sql, id, tenantId) > 0;
    }

    // =========================================================================
    // ENCADERNAMENTO INTELIGENTE DE PARÂMETROS (Evoluído com suporte a UUID)
    // =========================================================================
    protected void bindParameters(PreparedStatement stmt, Object... params) throws SQLException {
        if (params == null) return;
        
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            int idx = i + 1;
            
            if (value == null) {
                stmt.setNull(idx, Types.NULL);
                continue;
            }
            
            switch (value) {
                case String s -> stmt.setString(idx, s);
                case Integer iVal -> stmt.setInt(idx, iVal);
                case Long l -> stmt.setLong(idx, l);
                case Double d -> stmt.setDouble(idx, d);
                case BigDecimal db -> stmt.setBigDecimal(idx, db);
                case Boolean b -> stmt.setBoolean(idx, b);
                case LocalDate ld -> stmt.setDate(idx, Date.valueOf(ld));
                case LocalDateTime ldt -> stmt.setTimestamp(idx, Timestamp.valueOf(ldt));
                case UUID uuid -> stmt.setString(idx, uuid.toString()); // Fundamental para o novo banco!
                case Enum<?> e -> stmt.setString(idx, e.name());
                default -> stmt.setObject(idx, value);
            }
        }
    }

    // =========================================================================
    // UTILITÁRIO DE SINCRONIZAÇÃO EM LOTE (Mantido, pois é excelente)
    // =========================================================================
    protected <E, ID_TYPE> void syncByParentId(List<E> novos, List<E> atuais, Function<E, ID_TYPE> getId, 
                                               Consumer<E> inserir, Consumer<E> alterar, Consumer<E> softDelete) {
        var mapaAtuais = atuais.stream()
                .filter(e -> getId.apply(e) != null)
                .collect(Collectors.toMap(getId, e -> e));

        for (E novo : novos) {
            ID_TYPE id = getId.apply(novo);

            if (id == null || (id instanceof Number n && n.longValue() == 0)) {
                inserir.accept(novo);
            } else {
                alterar.accept(novo);
                mapaAtuais.remove(id);
            }
        }
        mapaAtuais.values().forEach(softDelete);
    }
}