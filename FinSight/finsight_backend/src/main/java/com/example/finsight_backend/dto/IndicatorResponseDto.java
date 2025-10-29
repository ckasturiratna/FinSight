package com.example.finsight_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorResponseDto {
    private String ticker;
    private String resolution;
    private List<IndicatorDefinitionDto> overlays;
    private List<IndicatorPointDto> points;
}
