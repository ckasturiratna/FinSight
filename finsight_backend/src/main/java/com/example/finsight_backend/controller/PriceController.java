package com.example.finsight_backend.controller;

import com.example.finsight_backend.dto.StockPriceDto;
import com.example.finsight_backend.service.StockQuoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PriceController {

    private final StockQuoteService service;

    @Autowired
    public PriceController(StockQuoteService service) {
        this.service = service;
    }

    @GetMapping("/price/{ticker}")
    @PreAuthorize("isAuthenticated()")
    public StockPriceDto getPrice(@PathVariable String ticker) {
        return service.getPrice(ticker);
    }
}