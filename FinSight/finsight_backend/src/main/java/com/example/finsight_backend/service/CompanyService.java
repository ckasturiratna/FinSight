package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.CompanyDto;
import com.example.finsight_backend.entity.Company;
import com.example.finsight_backend.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CompanyService {

    private final CompanyRepository repository;
    private final RestTemplate restTemplate;

    @Value("${finnhub.api.key}")
    private String apiKey;

    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Search companies from the local database only.
     * No Finnhub fetch on-demand.
     */
    public Page<CompanyDto> searchCompanies(String q, String sector, String country, String marketCap, String sort, int page, int size) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Specification<Company> spec = buildSpecification(q, sector, country, marketCap);

        Page<Company> companies = repository.findAll(spec, pageable);
        return companies.map(this::toDto);
    }

    private Sort parseSort(String sort) {
        Sort sortObj = Sort.by("name").ascending(); // default
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split("\\.");
            String property = sortParts[0];
            String direction = sortParts.length > 1 ? sortParts[1].toLowerCase() : "asc";
            sortObj = Sort.by(new Sort.Order(
                    "asc".equals(direction) ? Sort.Direction.ASC : Sort.Direction.DESC, property
            ));
        }
        return sortObj;
    }

    private Specification<Company> buildSpecification(String q, String sector, String country, String marketCap) {
        Specification<Company> spec = Specification.where(null);

        if (q != null && !q.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("ticker")), "%" + q.toLowerCase() + "%")
            ));
        }

        if (sector != null && !sector.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sector"), sector));
        }

        if (country != null && !country.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("country"), country));
        }

        if (marketCap != null && !marketCap.isEmpty()) {
            try {
                String[] range = marketCap.split("-");
                long min = Long.parseLong(range[0]) * 1_000_000L;
                long max = Long.parseLong(range[1]) * 1_000_000L;
                spec = spec.and((root, query, cb) -> cb.between(root.get("marketCap"), min, max));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Invalid marketCap format");
            }
        }

        return spec;
    }

    /**
     * DB Seeding: Fetch companies from Finnhub and save to local DB.
     * Can be called on startup or via a scheduler.
     */
    public void seedCompanies() {
        List<String> searchTerms = List.of("a", "b", "c", "d", "e"); // can expand for more coverage
        var executor = Executors.newFixedThreadPool(10);

        for (String term : searchTerms) {
            String searchUrl = "https://finnhub.io/api/v1/search?q=" + term + "&token=" + apiKey;
            Map<String, Object> searchResponse = restTemplate.getForObject(searchUrl, Map.class);

            if (searchResponse == null || !searchResponse.containsKey("result")) continue;

            List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("result");

            List<Company> companies = results.stream()
                    .map(result -> (String) result.get("symbol"))
                    .filter(symbol -> symbol != null && !symbol.contains(".")) // only US tickers
                    .limit(50)
                    .map(symbol -> CompletableFuture.supplyAsync(() -> fetchProfileSafe(symbol), executor))
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            repository.saveAll(companies);
        }

        System.out.println("DB seeding completed. Total companies: " + repository.count());
    }

    private Company fetchProfileSafe(String symbol) {
        try {
            return fetchProfile(symbol);
        } catch (Exception e) {
            // Skip failures
            return null;
        }
    }

    private Company fetchProfile(String symbol) {
        String profileUrl = "https://finnhub.io/api/v1/stock/profile2?symbol=" + symbol + "&token=" + apiKey;
        Map<String, Object> profile = restTemplate.getForObject(profileUrl, Map.class);

        if (profile == null || profile.isEmpty()) return null;

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

    private CompanyDto toDto(Company company) {
        CompanyDto dto = new CompanyDto();
        dto.setTicker(company.getTicker());
        dto.setName(company.getName());
        dto.setSector(company.getSector());
        dto.setCountry(company.getCountry());
        dto.setMarketCap(company.getMarketCap());
        dto.setDescription(company.getDescription());
        return dto;
    }

    public CompanyRepository getRepository() {
        return repository;
    }

    /**
     * Fetch company news from Finnhub API.
     */
    public List<Map<String, Object>> getCompanyNews(String symbol, int days) {
        try {
            // Calculate date range
            LocalDate toDate = LocalDate.now();
            LocalDate fromDate = toDate.minusDays(days);
            
            String newsUrl = "https://finnhub.io/api/v1/company-news?symbol=" + symbol + 
                           "&from=" + fromDate + "&to=" + toDate + "&token=" + apiKey;
            
            List<Map<String, Object>> news = restTemplate.getForObject(newsUrl, List.class);
            
            if (news == null) {
                return new ArrayList<>();
            }
            
            // Sort by datetime (newest first)
            return news.stream()
                    .filter(article -> article.get("datetime") != null)
                    .sorted((a, b) -> {
                        Long timeA = ((Number) a.get("datetime")).longValue();
                        Long timeB = ((Number) b.get("datetime")).longValue();
                        return timeB.compareTo(timeA); // Descending order
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Failed to fetch news for symbol {}: {}", symbol, e.getMessage());
            return new ArrayList<>();
        }
    }
}
