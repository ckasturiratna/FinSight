package com.example.finsight_backend.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorCalculatorTest {

    @Test
    void simpleMovingAverageProducesExpectedValues() {
        List<Double> input = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        Double[] result = IndicatorCalculator.simpleMovingAverage(input, 3);
        assertNull(result[0]);
        assertNull(result[1]);
        assertEquals(2.0, result[2]);
        assertEquals(3.0, result[3]);
        assertEquals(4.0, result[4]);
    }

    @Test
    void exponentialMovingAverageProducesExpectedValues() {
        List<Double> input = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        Double[] result = IndicatorCalculator.exponentialMovingAverage(input, 3);
        assertNull(result[0]);
        assertNull(result[1]);
        assertEquals(2.0, result[2]);
        assertEquals(3.0, result[3]);
        assertEquals(4.0, result[4]);
    }

    @Test
    void relativeStrengthIndexHandlesGainsAndLosses() {
        List<Double> input = Arrays.asList(10.0, 11.0, 12.0, 11.0, 13.0, 12.0, 13.0);
        Double[] result = IndicatorCalculator.relativeStrengthIndex(input, 3);
        assertNull(result[0]);
        assertNull(result[1]);
        assertEquals(100.0, result[2]);
        assertApprox(57.1428, result[3]);
        assertApprox(81.25, result[4]);
        assertApprox(57.1428, result[5]);
        assertApprox(70.339, result[6]);
    }

    @Test
    void rejectsPeriodGreaterThanSeries() {
        List<Double> input = Arrays.asList(1.0, 2.0, 3.0);
        assertThrows(IllegalArgumentException.class, () -> IndicatorCalculator.simpleMovingAverage(input, 5));
    }

    private void assertApprox(double expected, Double actual) {
        assertNotNull(actual);
        assertEquals(expected, actual, 1e-2);
    }
}
