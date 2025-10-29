package com.example.finsight_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "historical_stock_data")
@Data
public class HistoricalStockData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private Double open;
    private Double close;
    private Double high;
    private Double low;
    private Long volume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker", referencedColumnName = "ticker")
    private Company company;
}