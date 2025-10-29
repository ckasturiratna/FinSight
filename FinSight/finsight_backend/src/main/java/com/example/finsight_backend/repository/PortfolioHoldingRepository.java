package com.example.finsight_backend.repository;

import com.example.finsight_backend.entity.Portfolio;
import com.example.finsight_backend.entity.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {
    List<PortfolioHolding> findByPortfolioId(Long portfolioId);

    List<PortfolioHolding> findByPortfolio(Portfolio portfolio);

    Optional<PortfolioHolding> findByPortfolioAndCompany_Ticker(Portfolio portfolio, String ticker);

    @Query("SELECT DISTINCT h.company.ticker FROM PortfolioHolding h WHERE h.company.ticker IS NOT NULL AND h.company.ticker <> ''")
    List<String> findDistinctTickers();
}
