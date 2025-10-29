package com.example.finsight_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorDefinitionDto {
    private String key;
    private String label;
    private String type;
    private int period;
}
