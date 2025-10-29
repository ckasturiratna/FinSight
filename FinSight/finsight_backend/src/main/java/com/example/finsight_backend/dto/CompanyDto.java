package com.example.finsight_backend.dto;

import lombok.Data;

@Data
public class CompanyDto {
    private String ticker;
    private String name;
    private String sector;
    private String country;
    private Long marketCap;
    private String description;
}
