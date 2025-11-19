package com.example.finsight_backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StockHistoryPointDto {
    private LocalDate date;
    private Double close;
}
