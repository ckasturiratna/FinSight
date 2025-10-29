package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.IndicatorDefinitionDto;
import com.example.finsight_backend.dto.IndicatorResponseDto;
import com.example.finsight_backend.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndicatorServiceTest {

    private CompanyRepository companyRepository;
    private TestableIndicatorService indicatorService;

    @BeforeEach
    void setUp() throws Exception {
        companyRepository = mock(CompanyRepository.class);
        when(companyRepository.existsById(anyString())).thenReturn(true);

        IndicatorService.FinnhubCandleResponse candles = new IndicatorService.FinnhubCandleResponse();
        List<Double> closes = java.util.stream.IntStream.rangeClosed(1, 400)
                .mapToDouble(value -> 100.0 + value)
                .boxed()
                .toList();
        List<Long> timestamps = java.util.stream.IntStream.rangeClosed(1, 400)
                .mapToObj(Long::valueOf)
                .toList();
        setField(candles, "close", closes);
        setField(candles, "timestamp", timestamps);
        setField(candles, "status", "ok");

        indicatorService = new TestableIndicatorService(companyRepository, candles);
    }

    @Test
    void sanitisesPeriodsAndProvidesDefaults() {
        IndicatorResponseDto response = indicatorService.getIndicators(
                "AAPL",
                "D",
                50,
                List.of(1, -5, 20, 20),
                List.of(0, 400, 8),
                List.of()
        );

        Set<String> overlayKeys = response.getOverlays().stream()
                .map(IndicatorDefinitionDto::getKey)
                .collect(Collectors.toSet());

        assertAll(
                () -> assertEquals(Set.of("sma-5", "sma-20", "ema-8", "rsi-14"), overlayKeys),
                () -> assertEquals("D", indicatorService.lastResolution),
                () -> assertTrue(indicatorService.lastCount >= 30)
        );
    }

    @Test
    void increasesRequestedCountToCoverLargestPeriod() {
        IndicatorResponseDto response = indicatorService.getIndicators(
                "AAPL",
                "W",
                40,
                List.of(50),
                List.of(),
                List.of(30)
        );

        assertAll(
                () -> assertEquals("W", response.getResolution()),
                () -> assertTrue(indicatorService.lastCount >= 60)
        );
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class TestableIndicatorService extends IndicatorService {
        private final FinnhubCandleResponse canned;
        private int lastCount;
        private String lastResolution;

        TestableIndicatorService(CompanyRepository companyRepository, FinnhubCandleResponse canned) {
            super(companyRepository);
            this.canned = canned;
        }

        @Override
        protected FinnhubCandleResponse fetchCandles(String ticker, String resolution, int count) {
            this.lastCount = count;
            this.lastResolution = resolution;
            return canned;
        }
    }
}
