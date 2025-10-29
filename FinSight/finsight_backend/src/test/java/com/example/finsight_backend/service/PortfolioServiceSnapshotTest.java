package com.example.finsight_backend.service;

import com.example.finsight_backend.entity.Company;
import com.example.finsight_backend.entity.Portfolio;
import com.example.finsight_backend.entity.PortfolioHolding;
import com.example.finsight_backend.entity.PortfolioHistory;
import com.example.finsight_backend.repository.CompanyRepository;
import com.example.finsight_backend.repository.PortfolioHoldingRepository;
import com.example.finsight_backend.repository.PortfolioHistoryRepository;
import com.example.finsight_backend.repository.PortfolioRepository;
import com.example.finsight_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceSnapshotTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private PortfolioHoldingRepository holdingRepository;
    @Mock
    private PortfolioHistoryRepository historyRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PriceService priceService;

    @InjectMocks
    private PortfolioService portfolioService;

    private Portfolio portfolio;
    private PortfolioHolding holdingWithQuote;
    private PortfolioHolding holdingWithoutQuote;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio();
        portfolio.setId(42L);

        Company apple = new Company();
        apple.setTicker("AAPL");

        Company microsoft = new Company();
        microsoft.setTicker("MSFT");

        holdingWithQuote = new PortfolioHolding();
        holdingWithQuote.setPortfolio(portfolio);
        holdingWithQuote.setCompany(apple);
        holdingWithQuote.setQuantity(10.0);
        holdingWithQuote.setAveragePrice(90.0);

        holdingWithoutQuote = new PortfolioHolding();
        holdingWithoutQuote.setPortfolio(portfolio);
        holdingWithoutQuote.setCompany(microsoft);
        holdingWithoutQuote.setQuantity(5.0);
        holdingWithoutQuote.setAveragePrice(20.0);
    }

    @Test
    void snapshotPortfolios_persistsAggregatedValues() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        when(portfolioRepository.findAll()).thenReturn(List.of(portfolio));
        when(holdingRepository.findByPortfolio(portfolio)).thenReturn(List.of(holdingWithQuote, holdingWithoutQuote));
        when(historyRepository.existsByPortfolioAndSnapshotDate(eq(portfolio), any(LocalDate.class))).thenReturn(false);
        when(priceService.getLastPrice("AAPL"))
                .thenReturn(Optional.of(new PriceService.PriceQuote(150.0, Instant.now())));
        when(priceService.getLastPrice("MSFT"))
                .thenReturn(Optional.empty());

        portfolioService.snapshotPortfolios();

        ArgumentCaptor<PortfolioHistory> snapshotCaptor = ArgumentCaptor.forClass(PortfolioHistory.class);
        verify(historyRepository).save(snapshotCaptor.capture());

        PortfolioHistory saved = snapshotCaptor.getValue();
        assertEquals(portfolio, saved.getPortfolio());
        assertEquals(todayUtc, saved.getSnapshotDate());
        assertNotNull(saved.getCapturedAt());
        assertEquals(1000.0, saved.getInvested());
        assertEquals(1500.0, saved.getMarketValue());
        assertEquals(500.0, saved.getPnlAbs());
        assertEquals(0.5, saved.getPnlPct(), 1e-6);
        assertEquals(1, saved.getStaleCount());
    }
}
