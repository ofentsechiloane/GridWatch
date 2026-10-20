package com.gridwatch.service;

import com.gridwatch.dto.OutageResponse;
import com.gridwatch.exception.ResourceNotFoundException;
import com.gridwatch.model.OutageStatus;
import com.gridwatch.repository.OutageRepository;
import com.gridwatch.util.GeoUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class OutageService {

    private final OutageRepository repository;

    public OutageService(OutageRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<OutageResponse> findActive() {
        return repository.findByStatusNot(OutageStatus.RESTORED).stream()
                .map(OutageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OutageResponse findById(Long id) {
        return repository.findById(id).map(OutageResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Outage " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<OutageResponse> findNearby(double lat, double lon, double radiusMeters) {
        return repository.findByStatusNot(OutageStatus.RESTORED).stream()
                .filter(o -> GeoUtils.haversineMeters(lat, lon,
                        o.getCenterLatitude(), o.getCenterLongitude())
                        <= radiusMeters + o.getRadiusMeters())
                .map(OutageResponse::from).toList();
    }
}