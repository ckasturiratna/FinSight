package com.example.finsight_backend.repository;

import com.example.finsight_backend.entity.PredictionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PredictionLogRepository extends JpaRepository<PredictionLog, Long> {
    List<PredictionLog> findByTickerAndDateBetween(String ticker, LocalDate startDate, LocalDate endDate);

    List<PredictionLog> findAllByActualPriceIsNotNull();
}
