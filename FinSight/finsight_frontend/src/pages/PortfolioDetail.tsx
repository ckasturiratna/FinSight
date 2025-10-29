import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, Portfolio, PortfolioHolding, PortfolioHistoryPoint, portfolioApi, valuationApi, PortfolioValuation, HoldingValuation } from '@/lib/api';
import { getForecast } from '@/services/forecast.api';
import { Forecast } from '@/types/forecast';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { ArrowLeft, Pencil, Plus, Trash2 } from 'lucide-react';
import AppHeader from '@/components/AppHeader';
import CompanyPicker from '@/components/CompanyPicker';
import { Area, AreaChart, CartesianGrid, Line, XAxis, YAxis } from 'recharts';
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '@/components/ui/chart';
import { format, parseISO } from 'date-fns';

type HoldingFormState = {
  ticker: string;
  quantity: string;
  averagePrice: string;
  minThreshold: string;
  maxThreshold: string;
};

type UpsertHoldingPayload = {
  ticker: string;
  quantity: number;
  averagePrice: number;
  minThreshold: number | null;
  maxThreshold: number | null;
};

type ThresholdStatus = {
  label: string;
  badgeVariant: 'default' | 'secondary' | 'destructive' | 'outline';
  badgeClass: string;
  rowClass: string;
};

const createEmptyHoldingForm = (): HoldingFormState => ({
  ticker: '',
  quantity: '',
  averagePrice: '',
  minThreshold: '',
  maxThreshold: '',
});

const toNullableNumber = (value: string): number | null => {
  const trimmed = value.trim();
  if (!trimmed) return null;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : null;
};

const formatPriceDisplay = (value?: number | null): string => (value != null ? `$${value.toFixed(2)}` : '—');

const PortfolioDetail = () => {
  const { id } = useParams();
  const pid = Number(id);
  const navigate = useNavigate();
  const { toast } = useToast();
  const qc = useQueryClient();

  const { data: portfolio, isLoading } = useQuery<Portfolio>({
    queryKey: ['portfolio', pid],
    queryFn: () => api.get(`/api/portfolios/${pid}`),
    enabled: Number.isFinite(pid),
  });

  const { data: holdings, isLoading: isLoadingHoldings } = useQuery<PortfolioHolding[]>({
    queryKey: ['holdings', pid],
    queryFn: () => api.get(`/api/portfolios/${pid}/holdings`),
    enabled: Number.isFinite(pid),
  });
  const { data: valuation, isLoading: isLoadingValuation } = useQuery<PortfolioValuation>({
    queryKey: ['valuation', pid],
    queryFn: () => valuationApi.getPortfolioValue(pid),
    enabled: Number.isFinite(pid),
    refetchInterval: 10000,
  });
  const { data: history, isLoading: isLoadingHistory } = useQuery<PortfolioHistoryPoint[]>({
    queryKey: ['portfolio-history', pid],
    queryFn: () => portfolioApi.getHistory(pid),
    enabled: Number.isFinite(pid),
    staleTime: 5 * 60 * 1000,
  });

  // AI Forecast state
  const [selectedTicker, setSelectedTicker] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedTicker && holdings && holdings.length > 0) {
      setSelectedTicker(holdings[0].ticker);
    }
  }, [holdings, selectedTicker]);

  const { data: forecast, isLoading: isLoadingForecast } = useQuery<Forecast>({
    queryKey: ['forecast', selectedTicker],
    queryFn: () => getForecast(selectedTicker!),
    enabled: !!selectedTicker,
    staleTime: 5 * 60 * 1000,
  });

  // Edit portfolio dialog
  const [openEdit, setOpenEdit] = useState(false);
  const [pForm, setPForm] = useState({ name: '', description: '' });
  useEffect(() => {
    if (portfolio) setPForm({ name: portfolio.name, description: portfolio.description || '' });
  }, [portfolio]);

  const updateP = useMutation({
    mutationFn: () => api.put(`/api/portfolios/${pid}`, pForm),
    onSuccess: () => {
      toast({ title: 'Portfolio updated' });
      qc.invalidateQueries({ queryKey: ['portfolio', pid] });
      setOpenEdit(false);
    },
    onError: (e: any) => toast({ variant: 'destructive', title: 'Update failed', description: e.message }),
  });

  const deleteP = useMutation({
    mutationFn: () => api.del(`/api/portfolios/${pid}`),
    onSuccess: () => {
      toast({ title: 'Portfolio deleted' });
      navigate('/portfolios');
    },
    onError: (e: any) => toast({ variant: 'destructive', title: 'Delete failed', description: e.message }),
  });

  // Add/edit holding dialog
  const [openHolding, setOpenHolding] = useState(false);
  const [editingHolding, setEditingHolding] = useState<PortfolioHolding | null>(null);
  const [hForm, setHForm] = useState<HoldingFormState>(() => createEmptyHoldingForm());

  const parsedQuantity = toNullableNumber(hForm.quantity);
  const parsedAveragePrice = toNullableNumber(hForm.averagePrice);
  const parsedMinThreshold = toNullableNumber(hForm.minThreshold);
  const parsedMaxThreshold = toNullableNumber(hForm.maxThreshold);
  const thresholdsValid =
    parsedMinThreshold == null || parsedMaxThreshold == null || parsedMinThreshold <= parsedMaxThreshold;

  const buildHoldingPayload = (): UpsertHoldingPayload | null => {
    if (!hForm.ticker || parsedQuantity == null || parsedAveragePrice == null) return null;
    if (parsedQuantity < 0 || parsedAveragePrice < 0) return null;
    if (!thresholdsValid) return null;
    return {
      ticker: hForm.ticker,
      quantity: parsedQuantity,
      averagePrice: parsedAveragePrice,
      minThreshold: parsedMinThreshold,
      maxThreshold: parsedMaxThreshold,
    };
  };

  const addHolding = useMutation({
    mutationFn: (payload: UpsertHoldingPayload) => api.post(`/api/portfolios/${pid}/holdings`, payload),
    onSuccess: () => {
      toast({ title: 'Holding added' });
      qc.invalidateQueries({ queryKey: ['holdings', pid] });
      qc.invalidateQueries({ queryKey: ['valuation', pid] });
      setOpenHolding(false);
      setEditingHolding(null);
      setHForm(createEmptyHoldingForm());
    },
    onError: (e: any) => toast({ variant: 'destructive', title: 'Save failed', description: e.message }),
  });

  const updateHolding = useMutation({
    mutationFn: ({ holdingId, payload }: { holdingId: number; payload: UpsertHoldingPayload }) =>
      api.put(`/api/portfolios/${pid}/holdings/${holdingId}`, payload),
    onSuccess: () => {
      toast({ title: 'Holding updated' });
      qc.invalidateQueries({ queryKey: ['holdings', pid] });
      qc.invalidateQueries({ queryKey: ['valuation', pid] });
      setOpenHolding(false);
      setEditingHolding(null);
      setHForm(createEmptyHoldingForm());
    },
    onError: (e: any) => toast({ variant: 'destructive', title: 'Update failed', description: e.message }),
  });

  const canSaveHolding = Boolean(buildHoldingPayload());
  const isSavingHolding = addHolding.isPending || updateHolding.isPending;

  const saveHolding = () => {
    const payload = buildHoldingPayload();
    if (!payload) return;
    if (editingHolding) {
      updateHolding.mutate({ holdingId: editingHolding.id, payload });
    } else {
      addHolding.mutate(payload);
    }
  };

  const beginEditHolding = (holding: PortfolioHolding) => {
    setEditingHolding(holding);
    setHForm({
      ticker: holding.ticker,
      quantity: holding.quantity != null ? String(holding.quantity) : '',
      averagePrice: holding.averagePrice != null ? String(holding.averagePrice) : '',
      minThreshold: holding.minThreshold != null ? String(holding.minThreshold) : '',
      maxThreshold: holding.maxThreshold != null ? String(holding.maxThreshold) : '',
    });
    setOpenHolding(true);
  };

  const deleteHolding = useMutation({
    mutationFn: (hid: number) => api.del(`/api/portfolios/${pid}/holdings/${hid}`),
    onSuccess: () => {
      toast({ title: 'Holding deleted' });
      qc.invalidateQueries({ queryKey: ['holdings', pid] });
      qc.invalidateQueries({ queryKey: ['valuation', pid] });
    },
    onError: (e: any) => toast({ variant: 'destructive', title: 'Delete failed', description: e.message }),
  });

  const historyChartData = useMemo(
    () =>
      (history || []).map((point) => ({
        date: point.snapshotDate,
        marketValue: point.marketValue ?? 0,
        invested: point.invested ?? 0,
        pnlAbs: point.pnlAbs ?? 0,
      })),
    [history],
  );

  const latestHistoryPoint = historyChartData.at(-1);

  const formatCurrency = (value: number) => {
    if (!Number.isFinite(value)) return '';
    const absolute = Math.abs(value);
    if (absolute >= 1_000_000_000) return `${(value / 1_000_000_000).toFixed(1)}B`;
    if (absolute >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
    if (absolute >= 1_000) return `${(value / 1_000).toFixed(1)}K`;
    return value.toFixed(0);
  };

  const resolveThresholdStatus = (holding: HoldingValuation): ThresholdStatus => {
    const { lastPrice, minThreshold, maxThreshold } = holding;
    if (minThreshold == null && maxThreshold == null) {
      return {
        label: 'No threshold',
        badgeVariant: 'outline',
        badgeClass: 'text-muted-foreground border-muted-foreground/40',
        rowClass: '',
      };
    }
    if (lastPrice == null) {
      return {
        label: 'No price',
        badgeVariant: 'outline',
        badgeClass: '',
        rowClass: '',
      };
    }
    if (minThreshold != null && lastPrice < minThreshold) {
      return {
        label: `Below ${formatPriceDisplay(minThreshold)}`,
        badgeVariant: 'destructive',
        badgeClass: '',
        rowClass: 'bg-red-50/80 dark:bg-red-950/30',
      };
    }
    if (maxThreshold != null && lastPrice > maxThreshold) {
      return {
        label: `Above ${formatPriceDisplay(maxThreshold)}`,
        badgeVariant: 'destructive',
        badgeClass: '',
        rowClass: 'bg-red-50/80 dark:bg-red-950/30',
      };
    }
    return {
      label: 'In range',
      badgeVariant: 'outline',
      badgeClass: 'border-emerald-500 text-emerald-600 bg-emerald-500/10',
      rowClass: 'bg-emerald-50/60 dark:bg-emerald-950/25',
    };
  };

  if (!Number.isFinite(pid)) return null;

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-3">
            <Button variant="outline" onClick={() => navigate('/portfolios')}><ArrowLeft className="h-4 w-4 mr-2"/>Back</Button>
            <h2 className="text-2xl font-bold text-foreground">Portfolio</h2>
          </div>
          <div className="space-x-2">
            <Dialog open={openEdit} onOpenChange={setOpenEdit}>
              <DialogTrigger asChild>
                <Button variant="outline"><Pencil className="h-4 w-4 mr-2"/> Edit</Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Edit Portfolio</DialogTitle>
                </DialogHeader>
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label>Name</Label>
                    <Input value={pForm.name} onChange={e => setPForm({ ...pForm, name: e.target.value })} />
                  </div>
                  <div className="space-y-2">
                    <Label>Description</Label>
                    <Input value={pForm.description} onChange={e => setPForm({ ...pForm, description: e.target.value })} />
                  </div>
                </div>
                <DialogFooter>
                  <Button onClick={() => updateP.mutate()} disabled={!pForm.name || updateP.isPending}>Save</Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
            <Button variant="outline" className="text-destructive" onClick={() => { if (confirm('Delete this portfolio?')) deleteP.mutate(); }}><Trash2 className="h-4 w-4 mr-2"/>Delete</Button>
          </div>
        </div>

        <Card className="shadow-card border-0 mb-8">
          <CardHeader>
            <CardTitle>Overview</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading || !portfolio ? (
              <div className="py-4 text-muted-foreground">Loading...</div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div>
                  <div className="text-sm text-muted-foreground">Name</div>
                  <div className="text-lg font-semibold">{portfolio.name}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">Description</div>
                  <div className="text-lg">{portfolio.description || '-'}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">Created</div>
                  <div className="text-lg">{new Date(portfolio.createdAt).toLocaleString()}</div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="shadow-card border-0 mb-8">
          <CardHeader>
            <CardTitle>Performance History</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoadingHistory ? (
              <div className="py-4 text-muted-foreground">Loading portfolio history...</div>
            ) : historyChartData.length === 0 ? (
              <div className="py-8 text-center text-muted-foreground">
                No history captured yet. Snapshots are recorded each day after markets close.
              </div>
            ) : (
              <div className="space-y-6">
                {latestHistoryPoint && (
                  <div className="grid grid-cols-1 gap-4 text-sm text-muted-foreground sm:grid-cols-2 lg:grid-cols-3">
                    <div>
                      <div>Latest Market Value</div>
                      <div className="text-lg font-semibold text-foreground">
                        ${latestHistoryPoint.marketValue.toLocaleString(undefined, { maximumFractionDigits: 2 })}
                      </div>
                    </div>
                    <div>
                      <div>Invested Capital</div>
                      <div className="text-lg font-semibold text-foreground">
                        ${latestHistoryPoint.invested.toLocaleString(undefined, { maximumFractionDigits: 2 })}
                      </div>
                    </div>
                    <div>
                      <div>Total P/L</div>
                      <div className={`text-lg font-semibold ${latestHistoryPoint.pnlAbs > 0 ? 'text-green-600' : latestHistoryPoint.pnlAbs < 0 ? 'text-red-600' : 'text-foreground'}`}>
                        {latestHistoryPoint.pnlAbs >= 0 ? '+' : ''}$
                        {latestHistoryPoint.pnlAbs.toLocaleString(undefined, { maximumFractionDigits: 2 })}
                      </div>
                    </div>
                  </div>
                )}
                <ChartContainer
                  config={{
                    marketValue: { label: 'Market Value', color: 'hsl(var(--primary))' },
                    invested: { label: 'Invested', color: 'hsl(var(--muted-foreground))' },
                  }}
                >
                  <AreaChart data={historyChartData}>
                    <defs>
                      <linearGradient id="fill-marketValue" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="var(--color-marketValue)" stopOpacity={0.35} />
                        <stop offset="95%" stopColor="var(--color-marketValue)" stopOpacity={0} />
                      </linearGradient>
                      <linearGradient id="fill-invested" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="var(--color-invested)" stopOpacity={0.2} />
                        <stop offset="95%" stopColor="var(--color-invested)" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="4 4" vertical={false} className="stroke-border/60" />
                    <XAxis
                      dataKey="date"
                      tickLine={false}
                      axisLine={false}
                      tickFormatter={(value) => {
                        try {
                          return format(parseISO(value), 'MMM d');
                        } catch {
                          return value;
                        }
                      }}
                      minTickGap={24}
                      className="text-xs"
                    />
                    <YAxis
                      tickLine={false}
                      axisLine={false}
                      tickFormatter={(value) => `$${formatCurrency(value)}`}
                      className="text-xs"
                      width={60}
                    />
                    <ChartTooltip
                      cursor={{ strokeDasharray: '3 3' }}
                      content={
                        <ChartTooltipContent
                          labelFormatter={(value) => {
                            try {
                              return format(parseISO(String(value)), 'MMMM d, yyyy');
                            } catch {
                              return value;
                            }
                          }}
                          formatter={(value, name) => (
                            <div className="flex w-full items-center justify-between">
                              <span>{name === 'marketValue' ? 'Market Value' : 'Invested'}</span>
                              <span className="font-mono font-medium">${Number(value).toLocaleString()}</span>
                            </div>
                          )}
                        />
                      }
                    />
                    <Area
                      type="monotone"
                      dataKey="marketValue"
                      stroke="var(--color-marketValue)"
                      strokeWidth={2}
                      fill="url(#fill-marketValue)"
                      dot={false}
                      activeDot={{ r: 4 }}
                      name="Market Value"
                    />
                    <Area
                      type="monotone"
                      dataKey="invested"
                      stroke="var(--color-invested)"
                      strokeWidth={2}
                      fill="url(#fill-invested)"
                      dot={false}
                      activeDot={{ r: 4 }}
                      name="Invested"
                    />
                  </AreaChart>
                </ChartContainer>
              </div>
            )}
          </CardContent>
        </Card>

        {/* AI Forecast */}
        <Card className="shadow-card border-0 mb-8">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>AI-Powered Forecast</CardTitle>
              {holdings && holdings.length > 0 && (
                <Select value={selectedTicker || ''} onValueChange={setSelectedTicker}>
                  <SelectTrigger className="w-[180px]">
                    <SelectValue placeholder="Select a Ticker" />
                  </SelectTrigger>
                  <SelectContent>
                    {holdings.map(h => (
                      <SelectItem key={h.ticker} value={h.ticker}>
                        {h.ticker}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {isLoadingForecast ? (
              <div className="py-4 text-muted-foreground">Loading forecast...</div>
            ) : !forecast ? (
              <div className="py-8 text-center text-muted-foreground">
                Select a holding to view its forecast.
              </div>
            ) : (
              <ChartContainer
                config={{
                  mean: { label: 'Forecast', color: 'hsl(var(--primary))' },
                  confidence: { label: 'Confidence', color: 'hsl(var(--primary))' },
                }}
                className="h-[300px]"
              >
                <AreaChart data={forecast.forecastPoints}>
                  <defs>
                    <linearGradient id="fill-confidence" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="var(--color-confidence)" stopOpacity={0.25} />
                      <stop offset="95%" stopColor="var(--color-confidence)" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="4 4" vertical={false} className="stroke-border/60" />
                  <XAxis
                    dataKey="date"
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) => format(parseISO(value), 'MMM d')}
                    minTickGap={24}
                    className="text-xs"
                  />
                  <YAxis
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) => `$${formatCurrency(value)}`}
                    className="text-xs"
                    width={60}
                    domain={['dataMin - 10', 'dataMax + 10']}
                  />
                  <ChartTooltip
                    cursor={{ strokeDasharray: '3 3' }}
                    content={
                      <ChartTooltipContent
                        labelFormatter={(value) => format(parseISO(String(value)), 'MMMM d, yyyy')}
                        formatter={(value, name, props) => {
                          const { payload } = props;
                          if (!payload) return null;
                          return (
                            <div className="space-y-1">
                              <div className="flex w-full items-center justify-between">
                                <span>Forecast</span>
                                <span className="font-mono font-medium">${Number(payload.mean).toFixed(2)}</span>
                              </div>
                              <div className="flex w-full items-center justify-between text-muted-foreground text-xs">
                                <span>Confidence</span>
                                <span className="font-mono font-medium">
                                  ${Number(payload.lowerBound).toFixed(2)} - ${Number(payload.upperBound).toFixed(2)}
                                </span>
                              </div>
                            </div>
                          );
                        }}
                      />
                    }
                  />
                  <Area
                    dataKey="upperBound"
                    type="monotone"
                    stroke="var(--color-confidence)"
                    strokeWidth={1.5}
                    strokeOpacity={0.5}
                    strokeDasharray="4 4"
                    fill="url(#fill-confidence)"
                    name="Confidence Interval"
                    dot={false}
                  />
                   <Area
                    dataKey="lowerBound"
                    type="monotone"
                    stroke="var(--color-confidence)"
                    strokeWidth={1.5}
                    strokeOpacity={0.5}
                    strokeDasharray="4 4"
                    fill="url(#fill-confidence)"
                    name="Confidence Interval"
                    dot={false}
                  />
                  <Line
                    type="monotone"
                    dataKey="mean"
                    stroke="var(--color-mean)"
                    strokeWidth={2}
                    dot={false}
                    activeDot={{ r: 4 }}
                    name="Forecast"
                  />
                </AreaChart>
              </ChartContainer>
            )}
          </CardContent>
        </Card>

        {/* Valuation (live) */}
        <Card className="shadow-card border-0 mt-8">
          <CardHeader>
            <CardTitle>Valuation (live)</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoadingValuation ? (
              <div className="py-4 text-muted-foreground">Loading latest prices...</div>
            ) : !valuation ? (
              <div className="py-4 text-muted-foreground">No valuation data.</div>
            ) : (
              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                  <div>
                    <div className="text-sm text-muted-foreground">Total Invested</div>
                    <div className="text-lg font-semibold">${valuation.totals.invested.toFixed(2)}</div>
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">Market Value</div>
                    <div className="text-lg font-semibold">${valuation.totals.marketValue.toFixed(2)}</div>
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">P/L</div>
                    <div className={`text-lg font-semibold ${valuation.totals.pnlAbs > 0 ? 'text-green-600' : valuation.totals.pnlAbs < 0 ? 'text-red-600' : 'text-foreground'}`}>
                      {valuation.totals.pnlAbs >= 0 ? '+' : ''}${valuation.totals.pnlAbs.toFixed(2)} ({(valuation.totals.pnlPct*100).toFixed(2)}%)
                    </div>
                  </div>
                  <div>
                    <div className="text-sm text-muted-foreground">Stale Quotes</div>
                    <div className="text-lg font-semibold">{valuation.totals.staleCount}</div>
                  </div>
                </div>

                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Ticker</TableHead>
                      <TableHead>Name</TableHead>
                      <TableHead className="text-right">Qty</TableHead>
                      <TableHead className="text-right">Avg</TableHead>
                      <TableHead className="text-right">Last</TableHead>
                      <TableHead className="text-right">Thresholds</TableHead>
                      <TableHead className="text-right">Status</TableHead>
                      <TableHead className="text-right">Market Value</TableHead>
                      <TableHead className="text-right">P/L</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {valuation.holdings.map((v) => {
                      const status = resolveThresholdStatus(v);
                      return (
                        <TableRow key={v.ticker} className={status.rowClass}>
                          <TableCell className="font-medium">{v.ticker}</TableCell>
                          <TableCell>{v.name}</TableCell>
                          <TableCell className="text-right">{v.quantity}</TableCell>
                          <TableCell className="text-right">{formatPriceDisplay(v.averagePrice)}</TableCell>
                          <TableCell className="text-right">{formatPriceDisplay(v.lastPrice)}</TableCell>
                          <TableCell className="text-right">
                            <div className="flex flex-col items-end gap-1 text-xs text-muted-foreground">
                              <span>Min: {formatPriceDisplay(v.minThreshold)}</span>
                              <span>Max: {formatPriceDisplay(v.maxThreshold)}</span>
                            </div>
                          </TableCell>
                          <TableCell className="text-right">
                            <Badge variant={status.badgeVariant} className={status.badgeClass}>
                              {status.label}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">{formatPriceDisplay(v.marketValue)}</TableCell>
                          <TableCell className={`text-right ${v.pnlAbs != null && v.pnlAbs > 0 ? 'text-green-600' : v.pnlAbs != null && v.pnlAbs < 0 ? 'text-red-600' : 'text-foreground'}`}>
                            {v.pnlAbs != null ? `${v.pnlAbs >= 0 ? '+' : ''}$${v.pnlAbs.toFixed(2)} (${((v.pnlPct || 0) * 100).toFixed(2)}%)` : '—'}
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="shadow-card border-0">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>Holdings</CardTitle>
            <Dialog
              open={openHolding}
              onOpenChange={(o) => {
                setOpenHolding(o);
                if (!o) {
                  setEditingHolding(null);
                  setHForm(createEmptyHoldingForm());
                }
              }}
            >
              <DialogTrigger asChild>
                <Button
                  className="gradient-primary text-primary-foreground"
                  onClick={() => {
                    setEditingHolding(null);
                    setHForm(createEmptyHoldingForm());
                  }}
                >
                  <Plus className="h-4 w-4 mr-2"/> Add Holding
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>{editingHolding ? 'Edit Holding' : 'Add Holding'}</DialogTitle>
                </DialogHeader>
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label>Company</Label>
                    <CompanyPicker
                      disabled={!!editingHolding}
                      value={hForm.ticker ? { ticker: hForm.ticker, name: (holdings?.find(h => h.ticker === hForm.ticker)?.name) || hForm.ticker } as any : null}
                      onChange={(c) => setHForm({ ...hForm, ticker: c.ticker })}
                    />
                  </div>
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div className="space-y-2">
                      <Label>Quantity</Label>
                      <Input
                        type="number"
                        min="0"
                        step="any"
                        value={hForm.quantity}
                        onChange={(e) => setHForm({ ...hForm, quantity: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Average Price</Label>
                      <Input
                        type="number"
                        min="0"
                        step="any"
                        value={hForm.averagePrice}
                        onChange={(e) => setHForm({ ...hForm, averagePrice: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Min Threshold (optional)</Label>
                      <Input
                        type="number"
                        min="0"
                        step="any"
                        placeholder="e.g. 80"
                        value={hForm.minThreshold}
                        onChange={(e) => setHForm({ ...hForm, minThreshold: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Max Threshold (optional)</Label>
                      <Input
                        type="number"
                        min="0"
                        step="any"
                        placeholder="e.g. 140"
                        value={hForm.maxThreshold}
                        onChange={(e) => setHForm({ ...hForm, maxThreshold: e.target.value })}
                      />
                    </div>
                  </div>
                  {!thresholdsValid ? (
                    <p className="text-xs text-destructive">
                      Minimum threshold cannot be higher than the maximum threshold.
                    </p>
                  ) : null}
                </div>
                <DialogFooter>
                  <Button type="button" onClick={saveHolding} disabled={!canSaveHolding || isSavingHolding}>
                    {isSavingHolding ? 'Saving...' : 'Save'}
                 </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </CardHeader>
          <CardContent>
            {isLoadingHoldings ? (
              <div className="py-4 text-muted-foreground">Loading...</div>
            ) : !holdings || holdings.length === 0 ? (
              <div className="py-12 text-center text-muted-foreground">No holdings yet. Add positions to start tracking.</div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Ticker</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead className="text-right">Quantity</TableHead>
                    <TableHead className="text-right">Avg Price</TableHead>
                    <TableHead className="text-right">Min</TableHead>
                    <TableHead className="text-right">Max</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {holdings.map(h => (
                    <TableRow key={h.id}>
                      <TableCell className="font-medium">{h.ticker}</TableCell>
                      <TableCell>{h.name}</TableCell>
                      <TableCell className="text-right">{h.quantity}</TableCell>
                      <TableCell className="text-right">{formatPriceDisplay(h.averagePrice)}</TableCell>
                      <TableCell className="text-right">{formatPriceDisplay(h.minThreshold)}</TableCell>
                      <TableCell className="text-right">{formatPriceDisplay(h.maxThreshold)}</TableCell>
                      <TableCell className="text-right space-x-2">
                        <Button variant="outline" size="sm" onClick={() => beginEditHolding(h)}>
                          <Pencil className="h-3.5 w-3.5 mr-1"/> Edit
                        </Button>
                        <Button variant="outline" size="sm" className="text-destructive" onClick={() => { if (confirm(`Remove ${h.ticker}?`)) deleteHolding.mutate(h.id); }}>
                          <Trash2 className="h-3.5 w-3.5 mr-1"/> Delete
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  );
};

export default PortfolioDetail;
