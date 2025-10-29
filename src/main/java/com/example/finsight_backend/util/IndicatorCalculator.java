package com.example.finsight_backend.util;

import java.util.List;
import java.util.Objects;

public final class IndicatorCalculator {

    private IndicatorCalculator() {
    }

    public static Double[] simpleMovingAverage(List<Double> closes, int period) {
        validateInput(closes, period);
        Double[] result = new Double[closes.size()];
        double sum = 0.0;
        for (int i = 0; i < closes.size(); i++) {
            double price = require(closes.get(i));
            sum += price;
            if (i >= period) {
                sum -= require(closes.get(i - period));
            }
            if (i >= period - 1) {
                result[i] = sum / period;
            }
        }
        return result;
    }

    public static Double[] exponentialMovingAverage(List<Double> closes, int period) {
        validateInput(closes, period);
        Double[] result = new Double[closes.size()];
        double multiplier = 2.0 / (period + 1);
        double ema = 0.0;
        for (int i = 0; i < closes.size(); i++) {
            double price = require(closes.get(i));
            if (i < period - 1) {
                ema += price;
                continue;
            }
            if (i == period - 1) {
                ema = cumulativeAverage(closes, period);
                result[i] = ema;
            } else {
                ema = ((price - ema) * multiplier) + ema;
                result[i] = ema;
            }
        }
        return result;
    }

    public static Double[] relativeStrengthIndex(List<Double> closes, int period) {
        validateInput(closes, period);
        if (period < 2) {
            throw new IllegalArgumentException("RSI period must be >= 2");
        }
        Double[] result = new Double[closes.size()];
        double avgGain = 0.0;
        double avgLoss = 0.0;
        for (int i = 1; i < closes.size(); i++) {
            double change = require(closes.get(i)) - require(closes.get(i - 1));
            double gain = Math.max(change, 0.0);
            double loss = Math.max(-change, 0.0);

            if (i < period) {
                avgGain += gain;
                avgLoss += loss;
                if (i == period - 1) {
                    avgGain /= period;
                    avgLoss /= period;
                    result[i] = computeRsi(avgGain, avgLoss);
                }
            } else {
                avgGain = ((avgGain * (period - 1)) + gain) / period;
                avgLoss = ((avgLoss * (period - 1)) + loss) / period;
                result[i] = computeRsi(avgGain, avgLoss);
            }
        }
        return result;
    }

    private static double cumulativeAverage(List<Double> closes, int period) {
        double sum = 0.0;
        for (int i = 0; i < period; i++) {
            sum += require(closes.get(i));
        }
        return sum / period;
    }

    private static double require(Double value) {
        return Objects.requireNonNull(value, "Price series must not contain nulls");
    }

    private static void validateInput(List<Double> closes, int period) {
        if (closes == null || closes.isEmpty()) {
            throw new IllegalArgumentException("Price series is empty");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be greater than zero");
        }
        if (period > closes.size()) {
            throw new IllegalArgumentException("Period " + period + " exceeds data length " + closes.size());
        }
    }

    private static double computeRsi(double avgGain, double avgLoss) {
        if (avgLoss == 0.0) {
            return 100.0;
        }
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
}
