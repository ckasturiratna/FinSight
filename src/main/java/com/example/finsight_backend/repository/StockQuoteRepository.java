package com.example.finsight_backend.repository;

import com.example.finsight_backend.entity.StockQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StockQuoteRepository extends JpaRepository<StockQuote, Long> {
    Optional<StockQuote> findTopByCompanyTickerOrderByTimestampDesc(String ticker);
}