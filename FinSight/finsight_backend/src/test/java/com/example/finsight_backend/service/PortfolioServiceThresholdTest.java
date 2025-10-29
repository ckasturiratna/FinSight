package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.PortfolioHoldingDto;
import com.example.finsight_backend.dto.UpsertHoldingRequest;
import com.example.finsight_backend.entity.Company;
import com.example.finsight_backend.entity.Portfolio;
import com.example.finsight_backend.entity.PortfolioHolding;
import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.CompanyRepository;
import com.example.finsight_backend.repository.PortfolioHoldingRepository;
import com.example.finsight_backend.repository.PortfolioHistoryRepository;
import com.example.finsight_backend.repository.PortfolioRepository;
import com.example.finsight_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceThresholdTest {

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

    private User user;
    private Portfolio portfolio;
    private Company company;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("threshold-test@example.com");

        portfolio = new Portfolio();
        portfolio.setId(99L);
        portfolio.setUser(user);

        company = new Company();
        company.setTicker("AAPL");
        company.setName("Apple Inc.");

        org.springframework.security.core.context.SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), "pwd"));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addHolding_persistsThresholdsOnSave() {
        UpsertHoldingRequest request = new UpsertHoldingRequest();
        request.setTicker("AAPL");
        request.setQuantity(5.0);
        request.setAveragePrice(20.0);
        request.setMinThreshold(18.0);
        request.setMaxThreshold(25.0);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(portfolioRepository.findByIdAndUser(portfolio.getId(), user)).thenReturn(Optional.of(portfolio));
        when(companyRepository.findById("AAPL")).thenReturn(Optional.of(company));
        when(holdingRepository.findByPortfolioAndCompany_Ticker(portfolio, "AAPL")).thenReturn(Optional.empty());
        when(holdingRepository.save(any(PortfolioHolding.class))).thenAnswer(invocation -> {
            PortfolioHolding saved = invocation.getArgument(0);
            saved.setId(123L);
            return saved;
        });

        PortfolioHoldingDto dto = portfolioService.addHolding(portfolio.getId(), request);

        ArgumentCaptor<PortfolioHolding> captor = ArgumentCaptor.forClass(PortfolioHolding.class);
        verify(holdingRepository).save(captor.capture());
        PortfolioHolding saved = captor.getValue();
        assertEquals(18.0, saved.getMinThreshold());
        assertEquals(25.0, saved.getMaxThreshold());
        assertEquals(18.0, dto.getMinThreshold());
        assertEquals(25.0, dto.getMaxThreshold());
    }

    @Test
    void addHolding_rejectsInvalidThresholdWindow() {
        UpsertHoldingRequest request = new UpsertHoldingRequest();
        request.setTicker("AAPL");
        request.setQuantity(10.0);
        request.setAveragePrice(15.0);
        request.setMinThreshold(30.0);
        request.setMaxThreshold(20.0);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(portfolioRepository.findByIdAndUser(portfolio.getId(), user)).thenReturn(Optional.of(portfolio));
        when(companyRepository.findById("AAPL")).thenReturn(Optional.of(company));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> portfolioService.addHolding(portfolio.getId(), request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateHolding_allowsClearingThresholds() {
        PortfolioHolding holding = new PortfolioHolding();
        holding.setId(555L);
        holding.setPortfolio(portfolio);
        holding.setCompany(company);
        holding.setQuantity(4.0);
        holding.setAveragePrice(12.0);
        holding.setMinThreshold(10.0);
        holding.setMaxThreshold(18.0);

        UpsertHoldingRequest request = new UpsertHoldingRequest();
        request.setTicker("AAPL");
        request.setQuantity(holding.getQuantity());
        request.setAveragePrice(holding.getAveragePrice());
        request.setMinThreshold(null);
        request.setMaxThreshold(null);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(portfolioRepository.findByIdAndUser(portfolio.getId(), user)).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findById(holding.getId())).thenReturn(Optional.of(holding));

        PortfolioHoldingDto dto = portfolioService.updateHolding(portfolio.getId(), holding.getId(), request);

        assertNull(holding.getMinThreshold());
        assertNull(holding.getMaxThreshold());
        assertNull(dto.getMinThreshold());
        assertNull(dto.getMaxThreshold());
    }
}
