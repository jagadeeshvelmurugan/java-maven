package com.example.equity.service;

import com.example.equity.dao.EquityCompositionDao;
import com.example.equity.domain.EquityComposition;
import com.example.equity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompositionStateManager {

    private static final Logger log = LoggerFactory.getLogger(CompositionStateManager.class);

    public static final String STATUS_STARTED    = "STARTED";
    public static final String STATUS_WRITING    = "WRITING";
    public static final String STATUS_VALIDATING = "VALIDATING";
    public static final String STATUS_COMPLETED  = "COMPLETED";
    public static final String STATUS_FAILED     = "FAILED";

    private final EquityCompositionDao dao;

    public CompositionStateManager(EquityCompositionDao dao) {
        this.dao = dao;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transition(long id, String status) {
        dao.updateStatus(id, status);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMetaLength(long id, int metaLength) {
        dao.updateMetaLength(id, metaLength);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeCompleted(long id, JsonValidationService.ValidationResult result) {
        EquityComposition e = dao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Composition not found: " + id));

        if (e.getPortfolioId() != null) {
            int demoted = dao.demoteLatestForPortfolio(e.getPortfolioId());
            if (demoted > 0) {
                log.info("Demoted {} previous latest version(s) for portfolio {}",
                        demoted, e.getPortfolioId());
            }
        }

        long stored = dao.sumStoredBytes(id);
        dao.finalizeCompleted(id, result.checksum(), stored, result.bytes(), result.tokens());
        log.info("Composition {} promoted to latest='Y' for portfolio {}",
                id, e.getPortfolioId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(long id, Throwable t) {
        String msg = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        if (msg.length() > 1900) msg = msg.substring(0, 1900);
        dao.markFailed(id, msg);
    }
}
