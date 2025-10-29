package com.example.finsight_backend.repository;

import com.example.finsight_backend.entity.Portfolio;
import com.example.finsight_backend.entity.PortfolioHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioHistoryRepository extends JpaRepository<PortfolioHistory, Long> {
    Optional<PortfolioHistory> findByPortfolioAndSnapshotDate(Portfolio portfolio, LocalDate snapshotDate);
    boolean existsByPortfolioAndSnapshotDate(Portfolio portfolio, LocalDate snapshotDate);
    List<PortfolioHistory> findByPortfolioOrderBySnapshotDateAsc(Portfolio portfolio);
}
