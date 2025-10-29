package com.example.finsight_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "companies", indexes = {
        @Index(name = "idx_name", columnList = "name"),
        @Index(name = "idx_sector", columnList = "sector"),
        @Index(name = "idx_country", columnList = "country")
})
@Data
public class Company {
    @Id
    private String ticker;  // e.g., "AAPL"

    private String name;    // e.g., "Apple Inc."
    private String sector;  // e.g., "Technology"
    private String country; // e.g., "US"
    private Long marketCap; // e.g., 2000000000000L
    private String description; // e.g., "Consumer electronics company"

    // For future relations (lazy to avoid loading in searches)
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StockQuote> stockQuotes;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HistoricalStockData> historicalData;
}