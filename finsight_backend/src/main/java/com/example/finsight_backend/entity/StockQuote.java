package com.example.finsight_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_quotes", indexes = @Index(name = "idx_ticker_timestamp", columnList = "ticker,timestamp"))
@Data
public class StockQuote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double currentPrice;
    private Long volume;
    private Double percentChange;
    private Double high;
    private Double low;
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker", referencedColumnName = "ticker")
    private Company company;
}