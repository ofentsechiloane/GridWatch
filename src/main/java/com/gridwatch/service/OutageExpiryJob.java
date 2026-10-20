package com.gridwatch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OutageExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OutageExpiryJob.class);

    private final OutageDetectionService detectionService;

    public OutageExpiryJob(OutageDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    // Waits 5 minutes between runs by default; override with gridwatch.expiry.interval-ms
    @Scheduled(fixedDelayString = "${gridwatch.expiry.interval-ms:300000}")
    public void closeStaleOutages() {
        int closed = detectionService.expireStale(Instant.now());
        if (closed > 0) {
            log.info("Closed {} stale outage(s)", closed);
        }
    }
}