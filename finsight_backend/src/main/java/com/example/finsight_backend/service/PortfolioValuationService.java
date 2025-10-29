package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.HoldingValuationDto;
import com.example.finsight_backend.dto.PortfolioValuationDto;
import com.example.finsight_backend.entity.Portfolio;
import com.example.finsight_backend.entity.PortfolioHolding;
import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.PortfolioHoldingRepository;
import com.example.finsight_backend.repository.PortfolioRepository;
import com.example.finsight_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortfolioValuationService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final PriceService priceService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public PortfolioValuationDto getValuation(Long portfolioId) {
        User user = getCurrentUser();
        Portfolio p = portfolioRepository.findByIdAndUser(portfolioId, user)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));

        List<PortfolioHolding> holdings = holdingRepository.findByPortfolio(p);

        double totalInvested = 0.0;
        double totalMarketValue = 0.0;
        int staleCount = 0;
        List<HoldingValuationDto> rows = new ArrayList<>();

        for (PortfolioHolding h : holdings) {
            HoldingValuationDto dto = new HoldingValuationDto();
            dto.setTicker(h.getCompany().getTicker());
            dto.setName(h.getCompany().getName());
            dto.setQuantity(h.getQuantity());
            dto.setAveragePrice(h.getAveragePrice());
            dto.setMinThreshold(h.getMinThreshold());
            dto.setMaxThreshold(h.getMaxThreshold());

            double invested = safeMul(h.getQuantity(), h.getAveragePrice());
            dto.setInvested(invested);

            Optional<PriceService.PriceQuote> q = priceService.getLastPrice(h.getCompany().getTicker());
            if (q.isPresent()) {
                double last = q.get().getPrice();
                dto.setLastPrice(last);
                dto.setPriceAsOf(q.get().getAsOf());
                double mv = safeMul(h.getQuantity(), last);
                dto.setMarketValue(mv);
                dto.setPnlAbs(mv - invested);
                dto.setPnlPct(invested != 0.0 ? (mv - invested) / invested : 0.0);
                totalMarketValue += mv;
                dto.setStale(false);
            } else {
                dto.setLastPrice(null);
                dto.setMarketValue(null);
                dto.setPnlAbs(null);
                dto.setPnlPct(null);
                dto.setPriceAsOf(null);
                dto.setStale(true);
                staleCount++;
            }

            totalInvested += invested;
            rows.add(dto);
        }

        PortfolioValuationDto out = new PortfolioValuationDto();
        out.setPortfolioId(p.getId());
        out.setUpdatedAt(Instant.now());
        PortfolioValuationDto.Totals t = new PortfolioValuationDto.Totals();
        t.setInvested(totalInvested);
        t.setMarketValue(totalMarketValue);
        t.setPnlAbs(totalMarketValue - totalInvested);
        t.setPnlPct(totalInvested != 0.0 ? (totalMarketValue - totalInvested) / totalInvested : 0.0);
        t.setStaleCount(staleCount);
        out.setTotals(t);
        out.setHoldings(rows);
        return out;
    }

    private double safeMul(Double a, Double b) {
        double x = a == null ? 0.0 : a;
        double y = b == null ? 0.0 : b;
        return x * y;
    }
}
