export interface Forecast {
  ticker: string;
  forecastPoints: ForecastPoint[];
}

export interface ForecastPoint {
  date: string; // ISO date string
  mean: number;
  upperBound: number;
  lowerBound: number;
}
