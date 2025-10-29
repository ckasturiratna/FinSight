export interface ForecastLog {
  date: string; // ISO date string
  predictedPrice: number;
  actualPrice: number | null;
}
