import { useQuery } from '@tanstack/react-query';
import { getAccuracyMetrics, getPredictionLogs } from '@/services/forecast.api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { AlertTriangle, Target } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { format, parseISO } from 'date-fns';

const ForecastAccuracyPanel = () => {
  const { data: metrics, isLoading: isLoadingMetrics, error: errorMetrics } = useQuery({
    queryKey: ['forecast-accuracy'],
    queryFn: getAccuracyMetrics,
    staleTime: 60 * 60 * 1000, // Refetch every hour
  });

  const { data: logs, isLoading: isLoadingLogs, error: errorLogs } = useQuery({
    queryKey: ['prediction-logs'],
    queryFn: getPredictionLogs,
    staleTime: 60 * 60 * 1000, // Refetch every hour
  });

  const chartData = logs?.map(log => ({
    date: format(parseISO(log.date), 'MMM d'),
    Predicted: log.predictedPrice,
    Actual: log.actualPrice,
  }));

  return (
    <Card className="shadow-card border-0">
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <Target className="h-5 w-5 text-primary" />
          <span>Forecast Accuracy</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {isLoadingMetrics ? (
          <div className="py-4 text-center text-muted-foreground">Loading metrics...</div>
        ) : errorMetrics ? (
          <div className="py-4 text-center text-destructive flex items-center justify-center gap-2">
            <AlertTriangle className="h-4 w-4" />
            <span>Could not load accuracy data.</span>
          </div>
        ) : metrics ? (
          <div className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
            <div className="text-muted-foreground">MAPE</div>
            <div className="text-right font-semibold">{(metrics.meanAbsolutePercentageError * 100).toFixed(2)}%</div>

            <div className="text-muted-foreground">MAE</div>
            <div className="text-right font-semibold">{metrics.meanAbsoluteError.toFixed(2)}</div>

            <div className="text-muted-foreground">RMSE</div>
            <div className="text-right font-semibold">{metrics.rootMeanSquaredError.toFixed(2)}</div>

            <div className="text-muted-foreground">Observations</div>
            <div className="text-right font-semibold">{metrics.observationCount}</div>
          </div>
        ) : null}

        <div className="pt-4">
          {isLoadingLogs ? (
            <div className="py-4 text-center text-muted-foreground">Loading chart data...</div>
          ) : errorLogs ? (
            <div className="py-4 text-center text-destructive flex items-center justify-center gap-2">
              <AlertTriangle className="h-4 w-4" />
              <span>Could not load chart data.</span>
            </div>
          ) : chartData && chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} domain={['dataMin - 10', 'dataMax + 10']} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="Predicted" stroke="#8884d8" strokeWidth={2} />
                <Line type="monotone" dataKey="Actual" stroke="#82ca9d" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <div className="py-4 text-center text-muted-foreground">No prediction history to display.</div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

export default ForecastAccuracyPanel;
