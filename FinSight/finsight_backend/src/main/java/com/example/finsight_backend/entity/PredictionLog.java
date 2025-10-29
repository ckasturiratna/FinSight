package com.example.finsight_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "prediction_logs")
@Data
public class PredictionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private double predictedPrice;

    @Column
    private Double actualPrice;
}
