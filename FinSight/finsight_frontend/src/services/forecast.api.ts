import { Forecast } from '@/types/forecast';
import { AccuracyMetrics } from '@/types/accuracy-metrics';
import { ForecastLog } from '@/types/forecast-log';

const API_BASE_URL = 'http://localhost:8080';

/**
 * Fetches the AI-powered forecast for a given stock ticker.
 */
export const getForecast = async (ticker: string): Promise<Forecast> => {
  const token = localStorage.getItem('finsight_token');
  if (!token) {
    throw new Error('No authentication token found');
  }

  const response = await fetch(`${API_BASE_URL}/api/forecasts/${ticker}/ci`, {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch forecast for ${ticker}`);
  }

  return response.json();
};

/**
 * Fetches the forecast accuracy metrics.
 */
export const getAccuracyMetrics = async (): Promise<AccuracyMetrics> => {
  const token = localStorage.getItem('finsight_token');
  if (!token) {
    throw new Error('No authentication token found');
  }

  const response = await fetch(`${API_BASE_URL}/api/forecasts/accuracy`, {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch accuracy metrics');
  }

  return response.json();
};

/**
 * Fetches the historical prediction logs for accuracy analysis.
 */
export const getPredictionLogs = async (): Promise<ForecastLog[]> => {
  const token = localStorage.getItem('finsight_token');
  if (!token) {
    throw new Error('No authentication token found');
  }

  const response = await fetch(`${API_BASE_URL}/api/forecasts/logs`, {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch prediction logs');
  }

  return response.json();
};
