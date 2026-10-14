package com.gridwatch.repository;

import com.gridwatch.model.ElectricityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface ElectricityReportRepository extends JpaRepository<ElectricityReport, Long> {

    @Query("""
            select r from ElectricityReport r
            where r.createdAt >= :since
              and r.latitude between :minLat and :maxLat
              and r.longitude between :minLon and :maxLon
            order by r.createdAt desc
            """)
    List<ElectricityReport> findInBoundingBox(@Param("since") Instant since,
                                              @Param("minLat") double minLat,
                                              @Param("maxLat") double maxLat,
                                              @Param("minLon") double minLon,
                                              @Param("maxLon") double maxLon);
}