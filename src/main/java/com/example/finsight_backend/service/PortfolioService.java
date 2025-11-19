package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.*;
import com.example.finsight_backend.entity.Company;
import com.example.finsight_backend.entity.Portfolio;
import com.example.finsight_backend.entity.PortfolioHolding;
import com.example.finsight_backend.entity.PortfolioHistory;
import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.CompanyRepository;
import com.example.finsight_backend.repository.PortfolioHoldingRepository;
import com.example.finsight_backend.repository.PortfolioHistoryRepository;
import com.example.finsight_backend.repository.PortfolioRepository;
import com.example.finsight_backend.repository.UserRepository;
import com.example.finsight_backend.service.PriceService.PriceQuote;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final PortfolioHistoryRepository historyRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PriceService priceService;
    private final CandleService candleService;

    @Value("${finnhub.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(cron = "${app.portfolio.snapshot-cron:0 0 2 * * *}")
    @Transactional
    public void snapshotPortfolios() {
        LocalDate snapshotDate = LocalDate.now(ZoneOffset.UTC);
        Instant capturedAt = Instant.now();

        List<Portfolio> portfolios = portfolioRepository.findAll();
        if (portfolios.isEmpty()) {
            return;
        }

        for (Portfolio portfolio : portfolios) {
            try {
                if (historyRepository.existsByPortfolioAndSnapshotDate(portfolio, snapshotDate)) {
                    log.debug("Snapshot already exists for portfolio {} on {}", portfolio.getId(), snapshotDate);
                    continue;
                }

                List<PortfolioHolding> holdings = holdingRepository.findByPortfolio(portfolio);
                SnapshotTotals totals = calculateTotals(holdings);

                PortfolioHistory snapshot = new PortfolioHistory();
                snapshot.setPortfolio(portfolio);
                snapshot.setSnapshotDate(snapshotDate);
                snapshot.setCapturedAt(capturedAt);
                snapshot.setInvested(totals.invested());
                snapshot.setMarketValue(totals.marketValue());
                snapshot.setPnlAbs(totals.pnlAbs());
                snapshot.setPnlPct(totals.pnlPct());
                snapshot.setStaleCount(totals.staleCount());

                historyRepository.save(snapshot);
                log.debug("Captured snapshot for portfolio {} with market value {}", portfolio.getId(), totals.marketValue());
            } catch (Exception ex) {
                log.error("Failed to capture snapshot for portfolio {}: {}", portfolio.getId(), ex.getMessage(), ex);
            }
        }
    }

    private SnapshotTotals calculateTotals(List<PortfolioHolding> holdings) {
        double totalInvested = 0.0;
        double totalMarketValue = 0.0;
        int staleCount = 0;

        for (PortfolioHolding holding : holdings) {
            double quantity = toDouble(holding.getQuantity());
            double averagePrice = toDouble(holding.getAveragePrice());
            double invested = quantity * averagePrice;
            totalInvested += invested;

            if (quantity <= 0.0) {
                continue;
            }

            String ticker = holding.getCompany() != null ? holding.getCompany().getTicker() : null;
            if (ticker == null || ticker.isBlank()) {
                staleCount++;
                continue;
            }

            try {
                Optional<PriceQuote> quote = priceService.getLastPrice(ticker);
                if (quote.isPresent()) {
                    double marketValue = quantity * toDouble(quote.get().getPrice());
                    totalMarketValue += marketValue;
                } else {
                    staleCount++;
                }
            } catch (Exception ex) {
                staleCount++;
                log.debug("Price lookup failed for {}: {}", ticker, ex.getMessage());
            }
        }

        double pnlAbs = totalMarketValue - totalInvested;
        double pnlPct = totalInvested != 0.0 ? pnlAbs / totalInvested : 0.0;

        return new SnapshotTotals(
                sanitizeDouble(totalInvested),
                sanitizeDouble(totalMarketValue),
                sanitizeDouble(pnlAbs),
                sanitizeDouble(pnlPct),
                staleCount);
    }

    private double toDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double sanitizeDouble(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private record SnapshotTotals(double invested, double marketValue, double pnlAbs, double pnlPct, int staleCount) {
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional
    public PortfolioDto createPortfolio(@Valid CreatePortfolioRequest request) {
        User user = getCurrentUser();
        Portfolio p = new Portfolio();
        p.setName(request.getName());
        p.setDescription(request.getDescription());
        p.setUser(user);
        Portfolio saved = portfolioRepository.save(p);
        return toDto(saved, true);
    }

    public Page<PortfolioDto> listPortfolios(int page, int size) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        return portfolioRepository.findAllByUser(user, pageable)
                .map(p -> toDto(p, false));
    }

    public PortfolioDto getPortfolio(Long id) {
        User user = getCurrentUser();
        Portfolio p = portfolioRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));
        return toDto(p, true);
    }

    @Transactional
    public PortfolioDto updatePortfolio(Long id, @Valid UpdatePortfolioRequest request) {
        User user = getCurrentUser();
        Portfolio p = portfolioRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));
        p.setName(request.getName());
        p.setDescription(request.getDescription());
        return toDto(p, true);
    }

    @Transactional
    public void deletePortfolio(Long id) {
        User user = getCurrentUser();
        Portfolio p = portfolioRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));
        portfolioRepository.delete(p);
    }

    @Transactional
    public PortfolioHoldingDto addHolding(Long portfolioId, @Valid UpsertHoldingRequest request) {
        User user = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findByIdAndUser(portfolioId, user)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));

        Company company = getOrFetchCompany(request.getTicker());
        validateThresholds(request.getMinThreshold(), request.getMaxThreshold());
        // Upsert: if a holding for this ticker exists in the portfolio, update it
        PortfolioHolding holding = holdingRepository
                .findByPortfolioAndCompany_Ticker(portfolio, company.getTicker())
                .orElseGet(() -> {
                    PortfolioHolding h = new PortfolioHolding();
                    h.setPortfolio(portfolio);
                    h.setCompany(company);
                    return h;
                });

        holding.setQuantity(request.getQuantity());
        holding.setAveragePrice(request.getAveragePrice());
        holding.setMinThreshold(request.getMinThreshold());
        holding.setMaxThreshold(request.getMaxThreshold());

        PortfolioHolding saved = holdingRepository.save(holding);
        return toHoldingDto(saved);
    }

    @Transactional
    public PortfolioHoldingDto updateHolding(Long portfolioId, Long holdingId, @Valid UpsertHoldingRequest request) {
        User user = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findByIdAndUser(portfolioId, user)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));

        PortfolioHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new EntityNotFoundException("Holding not found"));

        if (!holding.getPortfolio().getId().equals(portfolio.getId())) {
            throw new EntityNotFoundException("Holding does not belong to this portfolio");
        }

        if (request.getTicker() != null && !request.getTicker().isBlank()
                && !request.getTicker().equalsIgnoreCase(holding.getCompany().getTicker())) {
            Company company = getOrFetchCompany(request.getTicker());
            holding.setCompany(company);
        }

        validateThresholds(request.getMinThreshold(), request.getMaxThreshold());

        if (request.getQuantity() != null)
            holding.setQuantity(request.getQuantity());
        if (request.getAveragePrice() != null)
            holding.setAveragePrice(request.getAveragePrice());
        holding.setMinThreshold(request.getMinThreshold());
        holding.setMaxThreshold(request.getMaxThreshold());

        return toHoldingDto(holding);
    }

    @Transactional
    public void removeHolding(Long portfolioId, Long holdingId) {
        User user = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findByIdAndUser(portfolioId, user)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));

        PortfolioHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new EntityNotFoundException("Holding not found"));

        if (!holding.getPortfolio().getId().equals(portfolio.getId())) {
            throw new EntityNotFoundException("Holding does not belong to this portfolio");
        }
        holdingRepository.delete(holding);
    }

    public List<PortfolioHoldingDto> listHoldings(Long portfolioId) {
        User user = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findByIdAndUser(portfolioId, user)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));
        return holdingRepository.findByPortfolio(portfolio).stream()
                .map(this::toHoldingDto)
                .collect(Collectors.toList());
    }

    public List<PortfolioHistoryPointDto> listHistory(Long portfolioId, Integer backfillDays) {
        User user = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findByIdAndUser(portfolioId, user)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));
        List<PortfolioHistoryPointDto> existing = historyRepository.findByPortfolioOrderBySnapshotDateAsc(portfolio).stream()
                .map(this::toHistoryDto)
                .collect(Collectors.toList());

        // If caller explicitly asked for backfill or there is no stored history, compute from candles
        boolean mustBackfill = (backfillDays != null && backfillDays > 0) || existing.isEmpty();
        if (!mustBackfill) {
            return existing;
        }
        int days = backfillDays != null && backfillDays > 0 ? backfillDays : 90;
        return computeBackfilledHistory(portfolio, days);
    }

    private List<PortfolioHistoryPointDto> computeBackfilledHistory(Portfolio portfolio, int days) {
        List<PortfolioHolding> holdings = holdingRepository.findByPortfolio(portfolio);
        if (holdings.isEmpty()) return List.of();

        // Pre-calc constant invested across all days based on current positions
        double totalInvested = 0.0;
        for (PortfolioHolding h : holdings) {
            double q = toDouble(h.getQuantity());
            double avg = toDouble(h.getAveragePrice());
            totalInvested += q * avg;
        }

        // Build date -> aggregated MV and stale count across tickers
        Map<java.time.LocalDate, Double> marketValues = new java.util.TreeMap<>();
        Map<java.time.LocalDate, Integer> stalePerDate = new java.util.HashMap<>();

        for (PortfolioHolding h : holdings) {
            double quantity = toDouble(h.getQuantity());
            if (quantity <= 0.0) continue;
            String ticker = (h.getCompany() != null) ? h.getCompany().getTicker() : null;
            if (ticker == null || ticker.isBlank()) continue;
            Map<java.time.LocalDate, Double> closes;
            try {
                closes = candleService.getDailyCloses(ticker, days);
            } catch (Exception e) {
                closes = java.util.Map.of();
            }
            if (closes.isEmpty()) {
                // Add a one-day stale entry using current quote when candles are unavailable
                try {
                    PriceQuote quote = priceService.getPrice(ticker);
                    java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
                    double mv = quantity * (quote != null && quote.getC() != null ? quote.getC() : 0.0);
                    marketValues.merge(today, mv, Double::sum);
                    stalePerDate.merge(today, 1, Integer::sum);
                } catch (Exception ignored) {
                    // If quote also fails, still bump stale on any dates present so far
                    for (java.time.LocalDate d : new java.util.HashSet<>(marketValues.keySet())) {
                        stalePerDate.merge(d, 1, Integer::sum);
                    }
                }
                continue;
            }
            for (Map.Entry<java.time.LocalDate, Double> e : closes.entrySet()) {
                double mv = quantity * (e.getValue() == null ? 0.0 : e.getValue());
                marketValues.merge(e.getKey(), mv, Double::sum);
            }

            // For dates without a close for this ticker (within the date domain built so far), count as stale
            for (java.time.LocalDate d : marketValues.keySet()) {
                if (!closes.containsKey(d)) {
                    stalePerDate.merge(d, 1, Integer::sum);
                }
            }
        }

        // Compose DTOs ordered by date asc
        List<PortfolioHistoryPointDto> out = new java.util.ArrayList<>(marketValues.size());

        // If we still have no market values (no candles, no quotes), generate a single snapshot using live totals
        if (marketValues.isEmpty()) {
            SnapshotTotals totals = calculateTotals(holdings);
            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
            marketValues.put(today, totals.marketValue());
            stalePerDate.put(today, totals.staleCount());
        }

        for (Map.Entry<java.time.LocalDate, Double> entry : marketValues.entrySet()) {
            java.time.LocalDate date = entry.getKey();
            double mv = sanitizeDouble(entry.getValue());
            double pnlAbs = sanitizeDouble(mv - totalInvested);
            double pnlPct = totalInvested != 0.0 ? pnlAbs / totalInvested : 0.0;

            PortfolioHistoryPointDto dto = new PortfolioHistoryPointDto();
            dto.setId(0L);
            dto.setSnapshotDate(date);
            dto.setCapturedAt(date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
            dto.setInvested(sanitizeDouble(totalInvested));
            dto.setMarketValue(mv);
            dto.setPnlAbs(pnlAbs);
            dto.setPnlPct(sanitizeDouble(pnlPct));
            dto.setStaleCount(stalePerDate.getOrDefault(date, 0));
            out.add(dto);
        }

        // Persist backfilled history so the UI can load stored snapshots
        java.time.Instant capturedAt = java.time.Instant.now();
        for (PortfolioHistoryPointDto dto : out) {
            if (historyRepository.existsByPortfolioAndSnapshotDate(portfolio, dto.getSnapshotDate())) {
                continue;
            }
            PortfolioHistory snapshot = new PortfolioHistory();
            snapshot.setPortfolio(portfolio);
            snapshot.setSnapshotDate(dto.getSnapshotDate());
            snapshot.setCapturedAt(capturedAt);
            snapshot.setInvested(dto.getInvested());
            snapshot.setMarketValue(dto.getMarketValue());
            snapshot.setPnlAbs(dto.getPnlAbs());
            snapshot.setPnlPct(dto.getPnlPct());
            snapshot.setStaleCount(dto.getStaleCount());
            historyRepository.save(snapshot);
        }

        return out;
    }

    // Auto-fetching companies search confined to portfolio service
    public Page<CompanyDto> searchCompaniesWithAutoFetch(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        // First try local DB (contains filter on ticker or name if q provided)
        Page<Company> initial = findCompaniesLocal(q, pageable);

        if (!initial.isEmpty()) {
            return initial.map(this::toCompanyDto);
        }

        // If nothing locally, check if database is completely empty
        long totalCompanies = companyRepository.count();
        if (totalCompanies == 0) {
            log.info("Database is empty, seeding default companies from Finnhub...");
            try {
                seedDefaultCompanies();
            } catch (Exception e) {
                log.warn("Failed to seed default companies: {}", e.getMessage());
            }
            // Re-query after seeding
            Page<Company> afterSeed = findCompaniesLocal(q, pageable);
            return afterSeed.map(this::toCompanyDto);
        }

        // If database has companies but none match the query, try Finnhub search
        if (q != null && !q.isBlank()) {
            try {
                seedCompaniesByQuery(q, 10);
            } catch (Exception e) {
                log.warn("Finnhub seed by query failed for '{}': {}", q, e.getMessage());
            }
            Page<Company> afterSeed = findCompaniesLocal(q, pageable);
            return afterSeed.map(this::toCompanyDto);
        }

        return initial.map(this::toCompanyDto);
    }

    private Page<Company> findCompaniesLocal(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return companyRepository.findAll(pageable);
        }
        // Simple contains filter using stream post-load if JPA Specification is not
        // available here
        // but to keep it efficient, we page DB first and widen if needed by querying
        // all then filtering
        Page<Company> pageResult = companyRepository.findAll(pageable);
        List<Company> filtered = pageResult.getContent().stream()
                .filter(c -> c.getTicker() != null && c.getTicker().toLowerCase().contains(q.toLowerCase()) ||
                        c.getName() != null && c.getName().toLowerCase().contains(q.toLowerCase()))
                .collect(Collectors.toList());
        // If current page didn’t match because of paging before filtering, broaden by
        // scanning all
        if (filtered.isEmpty() && pageResult.getTotalElements() > pageResult.getContent().size()) {
            List<Company> all = companyRepository.findAll();
            filtered = all.stream()
                    .filter(c -> (c.getTicker() != null && c.getTicker().toLowerCase().contains(q.toLowerCase())) ||
                            (c.getName() != null && c.getName().toLowerCase().contains(q.toLowerCase())))
                    .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                            Objects.toString(a.getName(), a.getTicker()),
                            Objects.toString(b.getName(), b.getTicker())))
                    .collect(Collectors.toList());
            int start = Math.min(pageable.getPageNumber() * pageable.getPageSize(), filtered.size());
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            List<Company> slice = filtered.subList(start, end);
            return new org.springframework.data.domain.PageImpl<>(slice, pageable, filtered.size());
        }
        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
    }

    private PortfolioDto toDto(Portfolio p, boolean includeHoldings) {
        PortfolioDto dto = new PortfolioDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        if (includeHoldings && p.getHoldings() != null) {
            dto.setHoldings(p.getHoldings().stream().map(this::toHoldingDto).collect(Collectors.toList()));
        }
        return dto;
    }

    private PortfolioHoldingDto toHoldingDto(PortfolioHolding h) {
        PortfolioHoldingDto dto = new PortfolioHoldingDto();
        dto.setId(h.getId());
        dto.setTicker(h.getCompany().getTicker());
        dto.setName(h.getCompany().getName());
        dto.setQuantity(h.getQuantity());
        dto.setAveragePrice(h.getAveragePrice());
        dto.setMinThreshold(h.getMinThreshold());
        dto.setMaxThreshold(h.getMaxThreshold());
        return dto;
    }

    private void validateThresholds(Double minThreshold, Double maxThreshold) {
        if (minThreshold == null || maxThreshold == null) {
            return;
        }
        if (minThreshold > maxThreshold) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Minimum threshold cannot be greater than maximum threshold");
        }
    }

    private PortfolioHistoryPointDto toHistoryDto(PortfolioHistory history) {
        PortfolioHistoryPointDto dto = new PortfolioHistoryPointDto();
        dto.setId(history.getId());
        dto.setSnapshotDate(history.getSnapshotDate());
        dto.setCapturedAt(history.getCapturedAt());
        dto.setInvested(history.getInvested());
        dto.setMarketValue(history.getMarketValue());
        dto.setPnlAbs(history.getPnlAbs());
        dto.setPnlPct(history.getPnlPct());
        dto.setStaleCount(history.getStaleCount());
        return dto;
    }

    private CompanyDto toCompanyDto(Company c) {
        CompanyDto dto = new CompanyDto();
        dto.setTicker(c.getTicker());
        dto.setName(c.getName());
        dto.setSector(c.getSector());
        dto.setCountry(c.getCountry());
        dto.setMarketCap(c.getMarketCap());
        dto.setDescription(c.getDescription());
        return dto;
    }

    private Company getOrFetchCompany(String rawTicker) {
        if (rawTicker == null || rawTicker.isBlank()) {
            throw new EntityNotFoundException("Ticker is required");
        }
        String ticker = rawTicker.trim().toUpperCase();
        return companyRepository.findById(ticker).orElseGet(() -> {
            log.info("Company {} not found locally. Fetching from Finnhub...", ticker);
            Company fetched = fetchCompanyFromFinnhub(ticker);
            if (fetched == null) {
                log.warn("Finnhub returned no profile for ticker {}", ticker);
                throw new EntityNotFoundException("Company not found for ticker: " + ticker);
            }
            Company saved = companyRepository.save(fetched);
            log.info("Saved company {} ({}) to DB", saved.getTicker(), saved.getName());
            return saved;
        });
    }

    private Company fetchCompanyFromFinnhub(String symbol) {
        try {
            String profileUrl = "https://finnhub.io/api/v1/stock/profile2?symbol=" + symbol + "&token=" + apiKey;
            Map<String, Object> profile = restTemplate.getForObject(profileUrl, Map.class);
            if (profile != null && !profile.isEmpty()) {
                Company company = mapProfileToCompany(symbol, profile);
                if (company.getName() != null && !company.getName().isBlank())
                    return company;
            }

            // Fallback: use Finnhub search to resolve the exact symbol and retry profile
            String searchUrl = "https://finnhub.io/api/v1/search?q=" + symbol + "&token=" + apiKey;
            Map<String, Object> searchResponse = restTemplate.getForObject(searchUrl, Map.class);
            if (searchResponse == null || !searchResponse.containsKey("result"))
                return null;

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> results = (java.util.List<Map<String, Object>>) searchResponse
                    .get("result");
            if (results == null || results.isEmpty())
                return null;

            // Prefer US-like symbols (no dot). Then try exact/startsWith match.
            String resolved = results.stream()
                    .map(r -> (String) r.get("symbol"))
                    .filter(Objects::nonNull)
                    .filter(s -> !s.contains("."))
                    .findFirst()
                    .orElseGet(() -> results.stream()
                            .map(r -> (String) r.get("symbol"))
                            .filter(Objects::nonNull)
                            .filter(s -> s.equalsIgnoreCase(symbol) || s.startsWith(symbol + "."))
                            .findFirst()
                            .orElse((String) results.get(0).get("symbol")));

            if (resolved == null)
                return null;
            String resolvedProfileUrl = "https://finnhub.io/api/v1/stock/profile2?symbol=" + resolved + "&token="
                    + apiKey;
            Map<String, Object> resolvedProfile = restTemplate.getForObject(resolvedProfileUrl, Map.class);
            if (resolvedProfile == null || resolvedProfile.isEmpty())
                return null;
            Company company = mapProfileToCompany(resolved, resolvedProfile);
            return (company.getName() == null || company.getName().isBlank()) ? null : company;
        } catch (Exception e) {
            log.error("Error fetching company from Finnhub for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private Company mapProfileToCompany(String symbol, Map<String, Object> profile) {
        Company company = new Company();
        company.setTicker(symbol);
        company.setName((String) profile.get("name"));
        company.setSector((String) profile.get("finnhubIndustry"));
        company.setCountry((String) profile.get("country"));
        Number marketCap = (Number) profile.get("marketCapitalization");
        company.setMarketCap(marketCap != null ? marketCap.longValue() * 1_000_000L : null);
        company.setDescription((String) profile.get("description"));
        return company;
    }

    private void seedCompaniesByQuery(String query, int limit) {
        String searchUrl = "https://finnhub.io/api/v1/search?q=" + query + "&token=" + apiKey;
        Map<String, Object> searchResponse = restTemplate.getForObject(searchUrl, Map.class);
        if (searchResponse == null || !searchResponse.containsKey("result"))
            return;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("result");
        if (results == null || results.isEmpty())
            return;

        List<String> symbols = results.stream()
                .map(r -> (String) r.get("symbol"))
                .filter(Objects::nonNull)
                // Filter out non-US style symbols (contain a dot), since many Finnhub plans
                // block them
                .filter(s -> !s.contains("."))
                .distinct()
                .limit(Math.max(1, Math.min(limit, 20)))
                .collect(Collectors.toList());

        List<Company> fetched = symbols.stream()
                .map(s -> CompletableFuture.supplyAsync(() -> fetchCompanyFromFinnhub(s)))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!fetched.isEmpty()) {
            // Save only new or update existing minimal fields
            for (Company company : fetched) {
                companyRepository.findById(company.getTicker())
                        .ifPresentOrElse(existing -> {
                            existing.setName(company.getName());
                            existing.setSector(company.getSector());
                            existing.setCountry(company.getCountry());
                            existing.setMarketCap(company.getMarketCap());
                            existing.setDescription(company.getDescription());
                            companyRepository.save(existing);
                        }, () -> companyRepository.save(company));
            }
        }
    }

    /**
     * Seed default companies when database is completely empty.
     * Fetches popular companies from Finnhub to populate the database.
     */
    private void seedDefaultCompanies() {
        log.info("Seeding default companies from Finnhub...");

        // List of popular company symbols to seed
        List<String> defaultSymbols = List.of(
                "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
                "META", "NVDA", "BRK.B", "UNH", "JNJ",
                "V", "PG", "JPM", "HD", "MA");

        List<Company> fetched = defaultSymbols.stream()
                .map(symbol -> CompletableFuture.supplyAsync(() -> fetchCompanyFromFinnhub(symbol)))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!fetched.isEmpty()) {
            companyRepository.saveAll(fetched);
            log.info("Successfully seeded {} default companies", fetched.size());
        } else {
            log.warn("No default companies could be fetched from Finnhub");
        }
    }

    @Transactional
    public PortfolioHoldingDto applyTransaction(Long portfolioId, @Valid PortfolioTransactionRequest request) {
        User user = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findByIdAndUser(portfolioId, user)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found"));

        String ticker = (request.getTicker() == null ? "" : request.getTicker()).trim().toUpperCase();
        if (ticker.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticker is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }

        PortfolioTransactionRequest.Action action = request.getAction();
        if (action == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action is required");
        }

        if (action == PortfolioTransactionRequest.Action.ADD) {
            if (request.getPrice() == null || request.getPrice() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be greater than zero for ADD");
            }
            Company company = getOrFetchCompany(ticker);
            PortfolioHolding holding = holdingRepository
                    .findByPortfolioAndCompany_Ticker(portfolio, company.getTicker())
                    .orElseGet(() -> {
                        PortfolioHolding h = new PortfolioHolding();
                        h.setPortfolio(portfolio);
                        h.setCompany(company);
                        h.setQuantity(0.0);
                        h.setAveragePrice(0.0);
                        return h;
                    });

            double currQty = holding.getQuantity() == null ? 0.0 : holding.getQuantity();
            double currAvg = holding.getAveragePrice() == null ? 0.0 : holding.getAveragePrice();
            double addQty = request.getQuantity();
            double price = request.getPrice();
            double newQty = currQty + addQty;
            double newAvg = newQty > 0 ? ((currQty * currAvg) + (addQty * price)) / newQty : 0.0;

            holding.setQuantity(newQty);
            holding.setAveragePrice(newAvg);
            PortfolioHolding saved = holdingRepository.save(holding);
            return toHoldingDto(saved);
        }

        // REMOVE
        PortfolioHolding holding = holdingRepository
                .findByPortfolioAndCompany_Ticker(portfolio, ticker)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Holding not found in portfolio for ticker: " + ticker));

        double currQty = holding.getQuantity() == null ? 0.0 : holding.getQuantity();
        double removeQty = request.getQuantity();
        if (removeQty > currQty) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove more shares than currently held");
        }

        double newQty = currQty - removeQty;
        if (newQty == 0.0) {
            holdingRepository.delete(holding);
            // Return the last state (qty 0) for client awareness
            PortfolioHoldingDto dto = toHoldingDto(holding);
            dto.setQuantity(0.0);
            return dto;
        } else {
            holding.setQuantity(newQty);
            // Keep averagePrice unchanged on removal
            PortfolioHolding saved = holdingRepository.save(holding);
            return toHoldingDto(saved);
        }
    }
}
