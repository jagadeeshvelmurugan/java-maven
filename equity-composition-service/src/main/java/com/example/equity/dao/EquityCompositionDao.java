package com.example.equity.dao;

import com.example.equity.domain.EquityComposition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EquityCompositionDao {

    long insert(EquityComposition c);
    void updateStatus(long id, String status);
    void updateMetaLength(long id, int metaLength);
    void markFailed(long id, String errorMessage);
    int  demoteLatestForPortfolio(String portfolioId);
    void finalizeCompleted(long id, String documentChecksum, long storedBytes,
                           long validationBytes, long validationTokens);

    Optional<EquityComposition> findById(long id);
    Optional<EquityComposition> findLatestCompleted(String portfolioId);
    Optional<EquityComposition> findLatestCompletedByPortfolioAndDate(String portfolioId, LocalDate asOfDate);
    List<EquityComposition> listCompletedByPortfolio(String portfolioId);
    List<EquityComposition> listVersions(String portfolioId);
    long sumStoredBytes(long compositionId);
}
