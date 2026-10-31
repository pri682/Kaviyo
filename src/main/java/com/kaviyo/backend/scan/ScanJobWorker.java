package com.kaviyo.backend.scan;

import com.kaviyo.audit.AuditReport;
import com.kaviyo.audit.CatalogPageAuditService;
import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.crawler.PageScanSnapshot;
import com.kaviyo.crawler.PageScanStatus;
import com.kaviyo.crawler.SafePageFetcher;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** One bounded job per poll; replicas coordinate through PostgreSQL row locks. */
@ApplicationScoped
public class ScanJobWorker {
    private static final Logger LOG = Logger.getLogger(ScanJobWorker.class);
    @Inject ScanJobRepository jobs;
    @Inject ScanRepository scans;
    @Inject SafePageFetcher pageFetcher;

    @ConfigProperty(name = "kaviyo.crawler.enabled", defaultValue = "false")
    boolean crawlerEnabled;
    @ConfigProperty(name = "kaviyo.scan.worker.enabled", defaultValue = "true")
    boolean workerEnabled;
    private volatile long lastQueueWarningMillis;

    @Scheduled(every = "${kaviyo.scan.worker.poll-interval:2s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void processOne() {
        if (!workerEnabled || !crawlerEnabled) return;
        Instant now = Instant.now(Clock.systemUTC());
        Optional<ScanJobRepository.ClaimedScan> claimed;
        try {
            claimed = jobs.claimNext(now);
        } catch (RuntimeException exception) {
            warnQueueUnavailable();
            return;
        }
        ScanJobRepository.ClaimedScan job = claimed.orElse(null);
        if (job == null) return;
        if (job.attempt() > 3) {
            jobs.retryOrFail(job, Instant.now(Clock.systemUTC()));
            return;
        }
        try {
            ScanRunDetail prior = scans.get(job.workspaceId(), job.runId()).orElse(null);
            if (prior != null && (ScanRunStatus.COMPLETED.name().equals(prior.status())
                    || ScanRunStatus.PARTIAL.name().equals(prior.status()))) {
                jobs.complete(job, Instant.now(Clock.systemUTC()));
                return;
            }
            ScanRepository.StartedScan started = scans.activateQueued(job.workspaceId(), job.runId(), job.attempt());
            CatalogPageAuditService audit = new CatalogPageAuditService(Clock.systemUTC());
            boolean partial = false;
            for (CatalogImportRepository.CatalogProductSnapshot product : job.products()) {
                PageScanSnapshot page;
                try {
                    page = pageFetcher.scan(product.productId(), URI.create(product.productUrl()));
                } catch (RuntimeException exception) {
                    page = failedPage(product);
                    partial = true;
                }
                AuditReport report = audit.audit(product, page);
                scans.recordProduct(started, product, page, report);
                jobs.heartbeat(job, Instant.now(Clock.systemUTC()));
            }
            Instant completedAt = Instant.now(Clock.systemUTC());
            scans.finish(started, partial ? ScanRunStatus.PARTIAL : ScanRunStatus.COMPLETED, completedAt);
            jobs.complete(job, completedAt);
        } catch (RuntimeException exception) {
            // Persist retry state; never expose provider, network, or SQL details to the caller.
            try {
                ScanRunDetail current = scans.get(job.workspaceId(), job.runId()).orElse(null);
                if (current == null || !ScanRunStatus.COMPLETED.name().equals(current.status())
                        && !ScanRunStatus.PARTIAL.name().equals(current.status())) {
                    jobs.retryOrFail(job, Instant.now(Clock.systemUTC()));
                }
            } catch (RuntimeException persistenceFailure) {
                warnQueueUnavailable();
            }
        }
    }

    private synchronized void warnQueueUnavailable() {
        long now = System.currentTimeMillis();
        if (now - lastQueueWarningMillis >= 60_000) {
            lastQueueWarningMillis = now;
            LOG.warn("Scan queue persistence is unavailable; the worker will retry automatically.");
        }
    }

    private static PageScanSnapshot failedPage(CatalogImportRepository.CatalogProductSnapshot product) {
        URI url = URI.create(product.productUrl());
        return new PageScanSnapshot(product.productId(), url, url, PageScanStatus.FETCH_FAILED,
                null, null, null, null, null, null, null, null, List.of(), null, null, null,
                Map.of(), Instant.now(Clock.systemUTC()));
    }
}
