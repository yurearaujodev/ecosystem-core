package br.com.yat.ecosystemcore.repository.base;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    
    public List<T> findAllByTenant(Connection conn, String tenantId) throws SQLException {
        String sql = "SELECT * FROM " + tableName + " WHERE tenant_id = ? AND deleted_at IS NULL";
        return executeQuery(conn, sql, tenantId);
    }

    /**
     * Busca no máximo um registro mapeado pela entidade do DAO ({@link #mapResultSetToEntity}).
     * Preferir este método em vez de {@link #executeQuerySingle} com lambda que engole SQLException.
     */
    protected Optional<T> executeQuerySingleEntity(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEntity(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Busca genérica com mapper customizado. O mapper deve propagar {@link SQLException};
     * não retornar {@code null} para mascarar erro de leitura (use {@link #executeQuerySingleEntity} para entidades).
     */
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

    protected LocalDateTime readLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }
    
    protected Long executeInsertReturningId(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParameters(stmt, params);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return null;
    }
    
    protected <R> R executeQueryForScalar(Connection conn, String sql, Class<R> clazz, Object... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Object value = rs.getObject(1);
                    return convertValue(value, clazz);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <R> R convertValue(Object value, Class<R> clazz) {
        if (value == null) return null;
        if (clazz.isInstance(value)) return clazz.cast(value);

        // Lida com conversões numéricas usando Pattern Matching
        return switch (value) {
            case Number n -> (R) switch (clazz.getSimpleName()) {
                case "Long"    -> Long.valueOf(n.longValue());
                case "Integer" -> Integer.valueOf(n.intValue());
                case "Double"  -> Double.valueOf(n.doubleValue());
                default        -> value;
            };
            default -> clazz.cast(value);
        };
    }
    

    // =========================================================================
    // MÉTODOS CORE MULTI-TENANT (Com chaves genéricas e segurança de isolamento)
    // =========================================================================

    /**
     * Busca um registro garantindo isolamento estrito de Tenant.
     */
    public Optional<T> searchById(Connection conn, PK id, String tenantId) throws SQLException {
        String sql = "SELECT * FROM " + tableName + " WHERE " + pkName + " = ? AND tenant_id = ? AND deleted_at IS NULL";
        return executeQuerySingleEntity(conn, sql, id, tenantId);
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