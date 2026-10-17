package com.gridwatch.repository;

import com.gridwatch.model.Outage;
import com.gridwatch.model.OutageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutageRepository extends JpaRepository<Outage, Long> {
    List<Outage> findByStatusNot(OutageStatus status);
}