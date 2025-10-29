package com.example.finsight_backend.controller;

import com.example.finsight_backend.dto.*;
import com.example.finsight_backend.service.PortfolioService;
import com.example.finsight_backend.service.PortfolioValuationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioValuationService valuationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PortfolioDto> create(@Valid @RequestBody CreatePortfolioRequest request) {
        return new ResponseEntity<>(portfolioService.createPortfolio(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<PortfolioDto> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return portfolioService.listPortfolios(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PortfolioDto get(@PathVariable Long id) {
        return portfolioService.getPortfolio(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PortfolioDto update(@PathVariable Long id, @Valid @RequestBody UpdatePortfolioRequest request) {
        return portfolioService.updatePortfolio(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/holdings")
    @PreAuthorize("isAuthenticated()")
    public List<PortfolioHoldingDto> listHoldings(@PathVariable Long id) {
        return portfolioService.listHoldings(id);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public List<PortfolioHistoryPointDto> listHistory(@PathVariable Long id,
                                                      @RequestParam(required = false) Integer backfillDays) {
        return portfolioService.listHistory(id, backfillDays);
    }

    @PostMapping("/{id}/holdings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PortfolioHoldingDto> addHolding(@PathVariable Long id,
                                                          @Valid @RequestBody UpsertHoldingRequest request) {
        return new ResponseEntity<>(portfolioService.addHolding(id, request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/holdings/{holdingId}")
    @PreAuthorize("isAuthenticated()")
    public PortfolioHoldingDto updateHolding(@PathVariable Long id,
                                             @PathVariable Long holdingId,
                                             @Valid @RequestBody UpsertHoldingRequest request) {
        return portfolioService.updateHolding(id, holdingId, request);
    }

    @DeleteMapping("/{id}/holdings/{holdingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteHolding(@PathVariable Long id, @PathVariable Long holdingId) {
        portfolioService.removeHolding(id, holdingId);
        return ResponseEntity.noContent().build();
    }

    // Portfolio-scoped companies search with auto-fetch, to avoid changing Company* files
    @GetMapping("/companies")
    @PreAuthorize("isAuthenticated()")
    public Page<CompanyDto> searchCompanies(@RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return portfolioService.searchCompaniesWithAutoFetch(q, page, size);
    }

    @PostMapping("/{id}/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PortfolioHoldingDto> applyTransaction(@PathVariable Long id,
                                                                @Valid @RequestBody PortfolioTransactionRequest request) {
        return new ResponseEntity<>(portfolioService.applyTransaction(id, request), HttpStatus.OK);
    }

    @GetMapping("/{id}/value")
    @PreAuthorize("isAuthenticated()")
    public PortfolioValuationDto getValuation(@PathVariable Long id) {
        return valuationService.getValuation(id);
    }
}
