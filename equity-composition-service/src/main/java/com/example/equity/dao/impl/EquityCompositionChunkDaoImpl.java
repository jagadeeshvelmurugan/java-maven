package com.example.equity.dao.impl;

import com.example.equity.dao.EquityCompositionChunkDao;
import com.example.equity.domain.EquityCompositionChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class EquityCompositionChunkDaoImpl implements EquityCompositionChunkDao {

    private final JdbcTemplate jdbc;

    public EquityCompositionChunkDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String META_COLUMNS =
            "composition_id, chunk_seq, chunk_size, uncompressed_size, status, " +
            "checksum, compressed, compression, created_at";

    private static final RowMapper<EquityCompositionChunk> META_MAPPER =
            EquityCompositionChunkDaoImpl::mapMeta;

    private static EquityCompositionChunk mapMeta(ResultSet rs, int rowNum) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return EquityCompositionChunk.builder()
                .compositionId(rs.getLong("composition_id"))
                .chunkSeq(rs.getInt("chunk_seq"))
                .chunkSize(rs.getInt("chunk_size"))
                .uncompressedSize((Integer) rs.getObject("uncompressed_size"))
                .status(rs.getString("status"))
                .checksum(rs.getString("checksum"))
                .compressed("Y".equals(rs.getString("compressed")))
                .compression(rs.getString("compression"))
                .createdAt(created == null ? null : created.toInstant())
                .build();
    }

    @Override
    public void insert(long compositionId, int seq, int chunkSize, int uncompressedSize,
                       byte[] data, String status, String checksum,
                       boolean compressed, String compression) {
        jdbc.update(
                "INSERT INTO equity_composition_chunk (" +
                "  composition_id, chunk_seq, chunk_size, uncompressed_size, " +
                "  chunk_data, status, checksum, compressed, compression, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                compositionId, seq, chunkSize, uncompressedSize,
                data,
                status,
                checksum,
                compressed ? "Y" : "N",
                compression);
    }

    @Override
    public List<EquityCompositionChunk> listMetadata(long compositionId) {
        return jdbc.query(
                "SELECT " + META_COLUMNS + " FROM equity_composition_chunk " +
                "WHERE composition_id = ? ORDER BY chunk_seq",
                META_MAPPER, compositionId);
    }

    @Override
    public Optional<EquityCompositionChunk> findMetadata(long compositionId, int seq) {
        List<EquityCompositionChunk> list = jdbc.query(
                "SELECT " + META_COLUMNS + " FROM equity_composition_chunk " +
                "WHERE composition_id = ? AND chunk_seq = ?",
                META_MAPPER, compositionId, seq);
        return list.stream().findFirst();
    }

    @Override
    public Optional<ChunkWithData> findWithData(long compositionId, int seq) {
        List<ChunkWithData> list = jdbc.query(
                "SELECT composition_id, chunk_seq, chunk_data, checksum, " +
                "       compressed, uncompressed_size " +
                "FROM equity_composition_chunk " +
                "WHERE composition_id = ? AND chunk_seq = ? AND status = 'COMPLETED'",
                (rs, rowNum) -> new ChunkWithData(
                        rs.getLong("composition_id"),
                        rs.getInt("chunk_seq"),
                        rs.getBytes("chunk_data"),
                        rs.getString("checksum"),
                        "Y".equals(rs.getString("compressed")),
                        rs.getInt("uncompressed_size")),
                compositionId, seq);
        return list.stream().findFirst();
    }

    @Override
    public long sumStoredBytes(long compositionId) {
        Long sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(chunk_size), 0) FROM equity_composition_chunk " +
                "WHERE composition_id = ?", Long.class, compositionId);
        return sum == null ? 0L : sum;
    }
}
