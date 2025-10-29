package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.HoldingValuationDto;
import com.example.finsight_backend.dto.PortfolioValuationDto;
import com.example.finsight_backend.entity.Company;
import com.example.finsight_backend.entity.Portfolio;
import com.example.finsight_backend.entity.PortfolioHolding;
import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.PortfolioHoldingRepository;
import com.example.finsight_backend.repository.PortfolioRepository;
import com.example.finsight_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioValuationServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private PortfolioHoldingRepository holdingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PriceService priceService;

    @InjectMocks
    private PortfolioValuationService valuationService;

    private User user;
    private Portfolio portfolio;
    private PortfolioHolding holding;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);
        user.setEmail("valuation@example.com");

        portfolio = new Portfolio();
        portfolio.setId(101L);
        portfolio.setUser(user);

        Company company = new Company();
        company.setTicker("AAPL");
        company.setName("Apple Inc.");

        holding = new PortfolioHolding();
        holding.setId(1L);
        holding.setPortfolio(portfolio);
        holding.setCompany(company);
        holding.setQuantity(2.0);
        holding.setAveragePrice(100.0);
        holding.setMinThreshold(95.0);
        holding.setMaxThreshold(120.0);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), "pwd"));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void valuationIncludesHoldingThresholds() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(portfolioRepository.findByIdAndUser(portfolio.getId(), user)).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolio(portfolio)).thenReturn(List.of(holding));
        when(priceService.getLastPrice(eq("AAPL"))).thenReturn(Optional.of(new PriceService.PriceQuote(130.0, Instant.now())));

        PortfolioValuationDto dto = valuationService.getValuation(portfolio.getId());

        assertNotNull(dto);
        assertEquals(1, dto.getHoldings().size());
        HoldingValuationDto holdingDto = dto.getHoldings().get(0);
        assertEquals(95.0, holdingDto.getMinThreshold());
        assertEquals(120.0, holdingDto.getMaxThreshold());
        assertEquals(130.0, holdingDto.getLastPrice());
        assertFalse(holdingDto.isStale());
        assertEquals(user.getEmail(), SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
