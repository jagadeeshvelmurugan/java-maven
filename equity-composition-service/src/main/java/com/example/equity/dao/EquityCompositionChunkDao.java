package com.example.equity.dao;

import com.example.equity.domain.EquityCompositionChunk;

import java.util.List;
import java.util.Optional;

public interface EquityCompositionChunkDao {

    void insert(long compositionId, int seq, int chunkSize, int uncompressedSize,
                byte[] data, String status, String checksum,
                boolean compressed, String compression);

    List<EquityCompositionChunk> listMetadata(long compositionId);
    Optional<EquityCompositionChunk> findMetadata(long compositionId, int seq);
    Optional<ChunkWithData> findWithData(long compositionId, int seq);
    long sumStoredBytes(long compositionId);

    record ChunkWithData(long compositionId, int seq, byte[] data,
                         String checksum, boolean compressed, int uncompressedSize) {}
}
