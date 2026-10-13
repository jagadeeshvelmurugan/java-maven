package com.example.equity.dao.impl;

import com.example.equity.dao.EquityCompositionDao;
import com.example.equity.domain.EquityComposition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class EquityCompositionDaoImpl implements EquityCompositionDao {

    private final JdbcTemplate jdbc;

    public EquityCompositionDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String COLUMNS =
            "id, portfolio_id, as_of_date, status, total_chunks, total_bytes, stored_bytes, " +
            "meta_length, document_checksum, validated_at, validation_msg, error_message, " +
            "version_timestamp, latest, created_at, updated_at";

    private static final RowMapper<EquityComposition> ROW_MAPPER =
            EquityCompositionDaoImpl::mapRow;

    private static EquityComposition mapRow(ResultSet rs, int rowNum) throws SQLException {
        int metaLen = rs.getInt("meta_length");
        Integer metaLength = rs.wasNull() ? null : metaLen;
        return EquityComposition.builder()
                .id(rs.getLong("id"))
                .portfolioId(rs.getString("portfolio_id"))
                .asOfDate(toLocalDate(rs.getDate("as_of_date")))
                .status(rs.getString("status"))
                .totalChunks(rs.getInt("total_chunks"))
                .totalBytes(rs.getLong("total_bytes"))
                .storedBytes(rs.getLong("stored_bytes"))
                .metaLength(metaLength)
                .documentChecksum(rs.getString("document_checksum"))
                .validatedAt(toInstant(rs.getTimestamp("validated_at")))
                .validationMsg(rs.getString("validation_msg"))
                .errorMessage(rs.getString("error_message"))
                .versionTimestamp(toInstant(rs.getTimestamp("version_timestamp")))
                .latest(rs.getString("latest"))
                .createdAt(toInstant(rs.getTimestamp("created_at")))
                .updatedAt(toInstant(rs.getTimestamp("updated_at")))
                .build();
    }

    private static LocalDate toLocalDate(Date d) { return d == null ? null : d.toLocalDate(); }
    private static Instant toInstant(Timestamp t) { return t == null ? null : t.toInstant(); }

    @Override
    public long insert(EquityComposition c) {
        Long id = jdbc.queryForObject("SELECT nextval('eq_comp_seq')", Long.class);

        jdbc.update(
                "INSERT INTO equity_composition (" +
                "  id, portfolio_id, as_of_date, status, total_chunks, total_bytes, stored_bytes, " +
                "  latest, created_at, updated_at, version_timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id,
                c.getPortfolioId(),
                c.getAsOfDate() == null ? null : Date.valueOf(c.getAsOfDate()),
                c.getStatus(),
                c.getTotalChunks(),
                c.getTotalBytes(),
                c.getStoredBytes() == null ? 0L : c.getStoredBytes(),
                c.getLatest() == null ? "N" : c.getLatest());
        return id;
    }

    @Override
    public void updateStatus(long id, String status) {
        jdbc.update("UPDATE equity_composition SET status = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ?", status, id);
    }

    @Override
    public void updateMetaLength(long id, int metaLength) {
        jdbc.update("UPDATE equity_composition SET meta_length = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ?", metaLength, id);
    }

    @Override
    public void markFailed(long id, String errorMessage) {
        jdbc.update("UPDATE equity_composition " +
                    "SET status = 'FAILED', latest = 'N', error_message = ?, " +
                    "    updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ?", errorMessage, id);
    }

    @Override
    public int demoteLatestForPortfolio(String portfolioId) {
        return jdbc.update(
                "UPDATE equity_composition SET latest = 'N', updated_at = CURRENT_TIMESTAMP " +
                "WHERE portfolio_id = ? AND latest = 'Y'", portfolioId);
    }

    @Override
    public void finalizeCompleted(long id, String documentChecksum, long storedBytes,
                                  long validationBytes, long validationTokens) {
        jdbc.update(
                "UPDATE equity_composition SET " +
                "  status = 'COMPLETED', " +
                "  latest = 'Y', " +
                "  stored_bytes = ?, " +
                "  document_checksum = ?, " +
                "  validated_at = CURRENT_TIMESTAMP, " +
                "  validation_msg = ?, " +
                "  updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?",
                storedBytes,
                documentChecksum,
                "OK (bytes=" + validationBytes + ", tokens=" + validationTokens + ")",
                id);
    }

    @Override
    public Optional<EquityComposition> findById(long id) {
        List<EquityComposition> list = jdbc.query(
                "SELECT " + COLUMNS + " FROM equity_composition WHERE id = ?",
                ROW_MAPPER, id);
        return list.stream().findFirst();
    }

    @Override
    public Optional<EquityComposition> findLatestCompleted(String portfolioId) {
        List<EquityComposition> list = jdbc.query(
                "SELECT " + COLUMNS + " FROM equity_composition " +
                "WHERE portfolio_id = ? AND latest = 'Y' AND status = 'COMPLETED'",
                ROW_MAPPER, portfolioId);
        return list.stream().findFirst();
    }

    @Override
    public Optional<EquityComposition> findLatestCompletedByPortfolioAndDate(
            String portfolioId, LocalDate asOfDate) {
        List<EquityComposition> list = jdbc.query(
                "SELECT " + COLUMNS + " FROM equity_composition " +
                "WHERE portfolio_id = ? AND as_of_date = ? AND latest = 'Y' AND status = 'COMPLETED'",
                ROW_MAPPER, portfolioId, Date.valueOf(asOfDate));
        return list.stream().findFirst();
    }

    @Override
    public List<EquityComposition> listCompletedByPortfolio(String portfolioId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM equity_composition " +
                "WHERE portfolio_id = ? AND status = 'COMPLETED' " +
                "ORDER BY version_timestamp DESC",
                ROW_MAPPER, portfolioId);
    }

    @Override
    public List<EquityComposition> listVersions(String portfolioId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM equity_composition " +
                "WHERE portfolio_id = ? ORDER BY version_timestamp DESC",
                ROW_MAPPER, portfolioId);
    }

    @Override
    public long sumStoredBytes(long compositionId) {
        Long sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(chunk_size), 0) FROM equity_composition_chunk " +
                "WHERE composition_id = ?", Long.class, compositionId);
        return sum == null ? 0L : sum;
    }
}
