package com.gridwatch.repository;

import com.gridwatch.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByApiKeyHash(String apiKeyHash);

    List<Device> findAllByOrderByCreatedAtDesc();
}