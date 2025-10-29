package com.example.finsight_backend.controller;

import com.example.finsight_backend.dto.CompanyDto;
import com.example.finsight_backend.entity.Company;
import com.example.finsight_backend.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CompanyController {

    private final CompanyService service;

    @Autowired
    public CompanyController(CompanyService service) {
        this.service = service;
    }

    /**
     * Fetch companies from the local DB with optional filters.
     * Works without a query string now.
     */
    @GetMapping("/companies")
    @PreAuthorize("isAuthenticated()")
    public Page<CompanyDto> getCompanies(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String marketCap,
            @RequestParam(defaultValue = "name.asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.searchCompanies(q, sector, country, marketCap, sort, page, size);
    }

    /**
     * Manually add companies to the DB.
     */
    @PostMapping("/companies")
    @PreAuthorize("hasRole('ADMIN')")
    public void addCompanies(@RequestBody List<Company> companies) {
        service.getRepository().saveAll(companies);
    }

    /**
     * Admin endpoint to trigger Finnhub seeding manually.
     */
    @PostMapping("/companies/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public String seedCompanies() {
        service.seedCompanies();
        return "Seeding completed successfully!";
    }

    /**
     * Get company news from Finnhub API.
     */
    @GetMapping("/companies/{symbol}/news")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> getCompanyNews(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "30") int days) {
        return service.getCompanyNews(symbol, days);
    }
}
