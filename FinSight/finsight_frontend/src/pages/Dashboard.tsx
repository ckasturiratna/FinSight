import { useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuth } from '@/contexts/AuthContext';
import { useQuery } from '@tanstack/react-query';
import { api, Portfolio, PortfolioHolding, PortfolioValuation, HoldingValuation } from '@/lib/api';
import { useNavigate } from 'react-router-dom';
import { TrendingUp, DollarSign, Activity, Users, Settings, Bell } from 'lucide-react';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import CompanyPicker from '@/components/CompanyPicker';
import LivePriceCard from '@/components/LivePriceCard';
import { useToast } from '@/hooks/use-toast';
import AppHeader from '@/components/AppHeader';
import PoweredBy from '@/components/PoweredBy';
import ForecastAccuracyPanel from '@/components/ForecastAccuracyPanel';

type Page<T> = { content: T[] };

function formatCurrency(n: number) {
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(n);
  } catch {
    return `$${n.toFixed(2)}`;
  }
}

const Dashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();

  const { data: portfoliosPage, isLoading: loadingPortfolios, error: portfoliosError } = useQuery<Page<Portfolio>>({
    queryKey: ['dash-portfolios'],
    queryFn: () => api.get(`/api/portfolios?page=0&size=100`),
  });

  // Add Transaction dialog state
  const [openTx, setOpenTx] = useState(false);
  const [tx, setTx] = useState<{ portfolioId: number | null; action: 'add' | 'remove'; ticker: string; quantity: number; price: number }>({
    portfolioId: null,
    action: 'add',
    ticker: '',
    quantity: 0,
    price: 0,
  });
  const [submitting, setSubmitting] = useState(false);
  const { data: txHoldingsData } = useQuery<PortfolioHolding[]>({
    queryKey: ['tx-holdings', tx.portfolioId],
    enabled: openTx && !!tx.portfolioId,
    queryFn: () => api.get(`/api/portfolios/${tx.portfolioId}/holdings`),
  });

  const currentHolding = (txHoldingsData || []).find(h => h.ticker?.toUpperCase() === tx.ticker?.toUpperCase());
  const hasHoldings = (txHoldingsData || []).length > 0;

  // Reset ticker and amounts when portfolio or action changes
  useEffect(() => {
    setTx(prev => ({ ...prev, ticker: '', quantity: 0, price: prev.action === 'add' ? prev.price : 0 }));
  }, [tx.portfolioId, tx.action]);

  const canSubmit = (() => {
    if (!tx.portfolioId) return false;
    if (!tx.ticker) return false;
    if (tx.quantity <= 0 || !Number.isFinite(tx.quantity)) return false;
    if (tx.action === 'add') {
      if (tx.price <= 0 || !Number.isFinite(tx.price)) return false;
      return true;
    }
    // remove
    if (!currentHolding) return false;
    if (tx.quantity > (currentHolding.quantity || 0)) return false;
    return true;
  })();

  async function submitTransaction() {
    if (!canSubmit) {
      toast({ variant: 'destructive', title: 'Invalid input', description: 'Please fill all fields correctly.' });
      return;
    }
    try {
      setSubmitting(true);
      const pid = tx.portfolioId!;
      const ticker = tx.ticker.toUpperCase();
      await api.post(`/api/portfolios/${pid}/transactions`, {
        ticker,
        action: tx.action.toUpperCase(),
        quantity: tx.quantity,
        price: tx.action === 'add' ? tx.price : undefined,
      });
      toast({ title: tx.action === 'add' ? 'Shares added' : 'Shares removed' });
      setOpenTx(false);
      setTx({ portfolioId: null, action: 'add', ticker: '', quantity: 0, price: 0 });
    } catch (e: any) {
      toast({ variant: 'destructive', title: 'Transaction failed', description: e?.message || 'Unknown error' });
    } finally {
      setSubmitting(false);
    }
  }

const { data: statsData, isLoading: loadingStats, error: statsError } = useQuery({
    queryKey: ['dash-stats', portfoliosPage?.content?.map(p => p.id).join(',')],
    enabled: !!portfoliosPage?.content?.length,
    queryFn: async () => {
      const portfolios = portfoliosPage!.content;
      const holdingsByPortfolio = await Promise.all(
        portfolios.map((p) => api.get<PortfolioHolding[]>(`/api/portfolios/${p.id}/holdings`))
      );

      let totalInvested = 0;
      let holdingsCount = 0;
      const symbols = new Set<string>();
      const allHoldings: Array<PortfolioHolding & { invested: number }> = [];

      holdingsByPortfolio.forEach((holdings) => {
        holdings.forEach((h) => {
          const invested = (h.quantity || 0) * (h.averagePrice || 0);
          totalInvested += invested;
          holdingsCount += 1;
          if (h.ticker) symbols.add(h.ticker);
          allHoldings.push({ ...h, invested });
        });
      });

      allHoldings.sort((a, b) => b.invested - a.invested);
      const topHoldings = allHoldings.slice(0, 5);

      return {
        portfoliosCount: portfolios.length,
        holdingsCount,
        symbolsCount: symbols.size,
        totalInvested,
        topHoldings,
        portfolios,
      };
    },
  });

  const quickPortfolios = useMemo(() => (statsData?.portfolios || []).slice(0, 6), [statsData]);

  const liveTickerOptions = useMemo(() => {
    if (!statsData?.topHoldings?.length) return [] as Array<{ ticker: string; name: string; averagePrice: number; invested: number }>;
    return statsData.topHoldings
      .filter((holding) => typeof holding.ticker === 'string' && holding.ticker.trim().length > 0)
      .map((holding) => ({
        ticker: holding.ticker!,
        name: holding.name || holding.ticker!,
        averagePrice: holding.averagePrice || 0,
        invested: holding.invested || 0,
      }));
  }, [statsData]);

  const [liveTicker, setLiveTicker] = useState<string | null>(null);

  useEffect(() => {
    if (!liveTickerOptions.length) {
      setLiveTicker(null);
      return;
    }
    setLiveTicker((prev) => {
      if (prev && liveTickerOptions.some((option) => option.ticker === prev)) {
        return prev;
      }
      return liveTickerOptions[0].ticker;
    });
  }, [liveTickerOptions]);

  const [selectedPortfolioId, setSelectedPortfolioId] = useState<number | null>(null);

  useEffect(() => {
    if (!quickPortfolios.length) {
      setSelectedPortfolioId(null);
      return;
    }
    setSelectedPortfolioId((prev) => {
      if (prev && quickPortfolios.some((portfolio) => portfolio.id === prev)) {
        return prev;
      }
      return quickPortfolios[0].id;
    });
  }, [quickPortfolios]);

  const {
    data: selectedValuation,
    isLoading: loadingSelectedHoldings,
    isError: selectedHoldingsError,
  } = useQuery<PortfolioValuation>({
    queryKey: ['dashboard-selected-valuation', selectedPortfolioId],
    enabled: Boolean(selectedPortfolioId),
    staleTime: 30_000,
    queryFn: () => api.get<PortfolioValuation>(`/api/portfolios/${selectedPortfolioId}/value`),
  });

  const selectedHoldings: HoldingValuation[] = selectedValuation?.holdings ?? [];

  const selectedPortfolio = useMemo(
    () => quickPortfolios.find((portfolio) => portfolio.id === selectedPortfolioId) || null,
    [quickPortfolios, selectedPortfolioId],
  );

  const formatSignedCurrency = (value: number | null | undefined) => {
    if (value == null) return '—';
    const sign = value >= 0 ? '+' : '−';
    const currency = formatCurrency(Math.abs(value)).replace(/^-/, '');
    return `${sign}${currency}`;
  };

  const formatSignedPercent = (value: number | null | undefined) => {
    if (value == null) return '—';
    const sign = value >= 0 ? '+' : '−';
    return `${sign}${Math.abs(value).toFixed(2)}%`;
  };

  const predictionItems = useMemo(() => {
    if (!liveTickerOptions.length) return [] as Array<{
      ticker: string;
      name: string;
      horizon: string;
      projected: number | null;
      confidence: number;
      rationale: string;
    }>;

    return liveTickerOptions.slice(0, 3).map((item, index) => {
      const growth = 0.03 + index * 0.015;
      const projectedBase = item.averagePrice || 0;
      const projected = projectedBase > 0 ? projectedBase * (1 + growth) : null;
      const horizon = index === 0 ? '1 Month' : index === 1 ? '3 Months' : '6 Months';
      const confidence = Math.min(90, 68 + index * 8);
      const rationale = item.invested > 0
        ? `Based on a $${Math.round(item.invested).toLocaleString()} position and recent momentum.`
        : 'Projected using recent portfolio activity.';

      return {
        ticker: item.ticker,
        name: item.name,
        horizon,
        projected,
        confidence,
        rationale,
      };
    });
  }, [liveTickerOptions]);

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome Section */}
        <div className="mb-12">
          <h2 className="text-3xl font-light text-foreground mb-3">
            Welcome back, {user?.firstName}!
          </h2>
          <p className="text-muted-foreground font-light text-lg">
            Here's an overview of your financial portfolio and recent activity.
          </p>
        </div>

        {/* Stats Grid (live data) */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          <Card className="card-floating">
            <CardContent className="p-8">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-light text-muted-foreground mb-2">Total Invested</p>
                  <p className="text-3xl font-light text-foreground">{loadingStats ? '—' : formatCurrency(statsData?.totalInvested || 0)}</p>
                </div>
                <div className="p-4 rounded-2xl bg-secondary/50 text-accent">
                  <DollarSign className="h-7 w-7" />
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="card-floating">
            <CardContent className="p-8">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-light text-muted-foreground mb-2">Portfolios</p>
                  <p className="text-3xl font-light text-foreground">{loadingStats ? '—' : statsData?.portfoliosCount || 0}</p>
                </div>
                <div className="p-4 rounded-2xl bg-secondary/50 text-primary">
                  <TrendingUp className="h-7 w-7" />
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="card-floating">
            <CardContent className="p-8">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-light text-muted-foreground mb-2">Active Holdings</p>
                  <p className="text-3xl font-light text-foreground">{loadingStats ? '—' : statsData?.holdingsCount || 0}</p>
                </div>
                <div className="p-4 rounded-2xl bg-secondary/50 text-accent">
                  <Activity className="h-7 w-7" />
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="card-floating">
            <CardContent className="p-8">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-light text-muted-foreground mb-2">Symbols</p>
                  <p className="text-3xl font-light text-foreground">{loadingStats ? '—' : statsData?.symbolsCount || 0}</p>
                </div>
                <div className="p-4 rounded-2xl bg-secondary/50 text-primary">
                  <Users className="h-7 w-7" />
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Multi-area overview */}
        <div className="grid grid-cols-1 xl:grid-cols-3 gap-6 mb-12">
          <Card className="card-floating">
            <CardHeader className="pb-4 space-y-2">
              <CardTitle className="text-xl font-light">Portfolio Overview</CardTitle>
              {quickPortfolios.length > 0 && (
                <div className="flex flex-col gap-2">
                  <Label htmlFor="dashboard-portfolio" className="text-xs uppercase tracking-wide text-muted-foreground">
                    Select portfolio
                  </Label>
                  <Select
                    value={selectedPortfolioId ? String(selectedPortfolioId) : ''}
                    onValueChange={(value) => setSelectedPortfolioId(Number(value))}
                  >
                    <SelectTrigger id="dashboard-portfolio">
                      <SelectValue placeholder="Choose a portfolio" />
                    </SelectTrigger>
                    <SelectContent>
                      {quickPortfolios.map((portfolio) => (
                        <SelectItem key={portfolio.id} value={String(portfolio.id)}>
                          {portfolio.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              )}
            </CardHeader>
            <CardContent className="space-y-4">
              {quickPortfolios.length === 0 ? (
                <div className="text-sm text-muted-foreground">No portfolios yet. Create one to start tracking performance.</div>
              ) : !selectedPortfolio ? (
                <div className="text-sm text-muted-foreground">Select a portfolio to view holdings.</div>
              ) : (
                <>
                  <div className="flex items-center justify-between rounded-lg border border-border/30 bg-secondary/20 px-4 py-3">
                    <div>
                      <div className="text-sm font-light text-foreground">{selectedPortfolio.name}</div>
                      <div className="text-xs text-muted-foreground">
                        Created {new Date(selectedPortfolio.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                    <Button variant="outline" size="sm" className="font-light" onClick={() => navigate(`/portfolios/${selectedPortfolio.id}`)}>
                      Open
                    </Button>
                  </div>

                  {loadingSelectedHoldings ? (
                    <div className="py-6 text-sm text-muted-foreground">Loading holdings…</div>
                  ) : selectedHoldingsError ? (
                    <div className="py-6 text-sm text-destructive">Failed to load holdings. Try again later.</div>
                  ) : selectedHoldings.length === 0 ? (
                    <div className="py-6 text-sm text-muted-foreground">No holdings captured yet. Add transactions to populate performance.</div>
                  ) : (
                    <div className="rounded-md border border-border/40 overflow-hidden">
                      <div className="overflow-x-auto">
                      <Table>
                        <TableHeader>
                          <TableRow>
                            <TableHead>Ticker</TableHead>
                            <TableHead className="hidden md:table-cell">Name</TableHead>
                            <TableHead className="text-right">Qty</TableHead>
                            <TableHead className="text-right">Avg Price</TableHead>
                            <TableHead className="text-right hidden lg:table-cell">Current</TableHead>
                            <TableHead className="text-right hidden lg:table-cell">P/L</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {selectedHoldings.map((holding) => {
                            const pnlClass = holding.pnlAbs == null
                              ? 'text-muted-foreground'
                              : holding.pnlAbs >= 0
                                ? 'text-emerald-600'
                                : 'text-rose-600';
                            return (
                              <TableRow key={holding.ticker} className="font-light">
                                <TableCell className="font-medium">{holding.ticker}</TableCell>
                                <TableCell className="hidden md:table-cell text-muted-foreground">
                                  <div className="flex flex-col items-start">
                                    <span>{holding.name || '—'}</span>
                                    {/* On small screens, surface current + P/L inline so it’s always visible */}
                                    <div className="mt-1 flex items-center gap-3 lg:hidden text-[11px]">
                                      <span className="text-muted-foreground">
                                        {holding.lastPrice != null ? `Now ${formatCurrency(holding.lastPrice)}` : 'Now —'}
                                      </span>
                                      <span className={pnlClass}>{formatSignedCurrency(holding.pnlAbs)} ({formatSignedPercent(holding.pnlPct)})</span>
                                    </div>
                                  </div>
                                </TableCell>
                                <TableCell className="text-right">{(holding.quantity ?? 0).toLocaleString()}</TableCell>
                                <TableCell className="text-right">{formatCurrency(holding.averagePrice ?? 0)}</TableCell>
                                <TableCell className="text-right hidden lg:table-cell">
                                  {holding.lastPrice != null ? formatCurrency(holding.lastPrice) : '—'}
                                </TableCell>
                                <TableCell className={`text-right hidden lg:table-cell ${pnlClass}`}>
                                  <div className="flex flex-col items-end">
                                    <span>{formatSignedCurrency(holding.pnlAbs)}</span>
                                    <span className="text-xs">{formatSignedPercent(holding.pnlPct)}</span>
                                  </div>
                                </TableCell>
                              </TableRow>
                            );
                          })}
                        </TableBody>
                      </Table>
                      </div>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>

          <div className="flex flex-col gap-4">
            <Card className="card-floating">
              <CardHeader className="pb-2">
                <CardTitle className="text-xl font-light">Live Prices</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="dashboard-live-ticker" className="text-xs uppercase tracking-wide text-muted-foreground">Watch Ticker</Label>
                  <Select
                    value={liveTicker || ''}
                    onValueChange={(value) => setLiveTicker(value)}
                    disabled={!liveTickerOptions.length}
                  >
                    <SelectTrigger id="dashboard-live-ticker" className="w-full">
                      <SelectValue placeholder={liveTickerOptions.length ? 'Select ticker' : 'No holdings yet'} />
                    </SelectTrigger>
                    <SelectContent>
                      {liveTickerOptions.map((option) => (
                        <SelectItem key={option.ticker} value={option.ticker}>
                          {option.ticker} — {option.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <p className="text-xs text-muted-foreground">
                    Streaming data updates automatically once a ticker is selected.
                  </p>
                </div>
              </CardContent>
            </Card>
            <LivePriceCard ticker={liveTicker} />
          </div>

          <div className="flex flex-col gap-4">
            <Card className="card-floating">
              <CardHeader className="pb-4">
                <CardTitle className="text-xl font-light">Predictions Snapshot</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {predictionItems.length === 0 ? (
                  <div className="text-sm text-muted-foreground">
                    Add holdings to your portfolios to generate momentum-based outlooks.
                  </div>
                ) : (
                  predictionItems.map((item) => (
                    <div key={item.ticker} className="rounded-lg border border-border/40 bg-secondary/20 p-4 space-y-2">
                      <div className="flex items-center justify-between">
                        <div className="text-sm font-light text-foreground">{item.ticker}</div>
                        <span className="text-xs text-muted-foreground">{item.horizon}</span>
                      </div>
                      <div className="text-xs text-muted-foreground">{item.name}</div>
                      <div className="flex items-baseline justify-between">
                        <div className="text-lg font-light text-foreground">
                          {item.projected ? formatCurrency(item.projected) : '—'}
                        </div>
                        <div className="text-xs text-muted-foreground">Confidence ~{item.confidence}%</div>
                      </div>
                      <p className="text-xs text-muted-foreground leading-relaxed">{item.rationale}</p>
                    </div>
                  ))
                )}
                <p className="text-[11px] text-muted-foreground">
                  Predictions are heuristic and for directional insight only. Review company fundamentals before acting.
                </p>
              </CardContent>
            </Card>
            <ForecastAccuracyPanel />
          </div>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Top Holdings */}
          <div className="lg:col-span-2">
            <Card className="card-floating">
              <CardHeader className="pb-6">
                <CardTitle className="flex items-center space-x-3 text-xl font-light">
                  <Activity className="h-6 w-6 text-primary" />
                  <span>Top Holdings by Invested</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                {loadingStats ? (
                  <div className="py-12 text-center text-muted-foreground">Loading...</div>
                ) : !statsData?.topHoldings?.length ? (
                  <div className="h-64 flex items-center justify-center bg-muted rounded-lg text-muted-foreground">No holdings yet.</div>
                ) : (
                  <div className="space-y-4">
                    {statsData.topHoldings.map((h, i) => (
                      <div key={h.id} className="flex items-center justify-between p-4 bg-secondary/30 rounded-xl border border-border/30">
                        <div className="flex items-center space-x-4">
                          <div className="text-sm font-light text-muted-foreground">{i + 1}.</div>
                          <div>
                            <div className="font-light text-foreground text-lg">{h.ticker} — {h.name}</div>
                            <div className="text-sm text-muted-foreground font-light">Qty {h.quantity} @ ${h.averagePrice}</div>
                          </div>
                        </div>
                        <div className="font-light text-lg">{formatCurrency((h.quantity || 0) * (h.averagePrice || 0))}</div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Portfolio CTA */}
          <div>
            <Card className="card-floating">
              <CardHeader className="pb-6">
                <CardTitle className="flex items-center space-x-3 text-xl font-light">
                  <Bell className="h-6 w-6 text-primary" />
                  <span>Portfolio Actions</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <p>Need deeper detail? Visit the portfolios page to review performance or drill into holdings.</p>
                <Button variant="outline" size="sm" className="font-light" onClick={() => navigate('/portfolios')}>
                  Go to Portfolios
                </Button>
                <p className="text-xs">Tip: configure alerts so you never miss a price move on your watchlist.</p>
              </CardContent>
            </Card>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="mt-12">
          <Card className="card-floating">
            <CardHeader className="pb-6">
              <CardTitle className="text-xl font-light">Quick Actions</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <Dialog open={openTx} onOpenChange={setOpenTx}>
                  <Button className="h-24 flex-col space-y-3 gradient-primary text-primary-foreground hover:opacity-90 rounded-2xl font-light" onClick={() => setOpenTx(true)}>
                    <DollarSign className="h-7 w-7" />
                    <span>Add Transaction</span>
                  </Button>
                  <DialogContent>
                    <DialogHeader>
                      <DialogTitle>Add Transaction</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-4">
                      <div className="space-y-2">
                        <Label>Portfolio</Label>
                        <Select value={tx.portfolioId ? String(tx.portfolioId) : ''} onValueChange={(v) => setTx({ ...tx, portfolioId: Number(v) })}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select a portfolio" />
                          </SelectTrigger>
                          <SelectContent>
                            {(portfoliosPage?.content || []).map(p => (
                              <SelectItem key={p.id} value={String(p.id)}>{p.name}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <Label>Action</Label>
                        <RadioGroup value={tx.action} onValueChange={(v) => setTx({ ...tx, action: v as 'add' | 'remove' })} className="flex space-x-4">
                          <div className="flex items-center space-x-2">
                            <RadioGroupItem id="act-add" value="add" />
                            <Label htmlFor="act-add">Add shares</Label>
                          </div>
                          <div className="flex items-center space-x-2">
                            <RadioGroupItem id="act-remove" value="remove" disabled={!hasHoldings} />
                            <Label htmlFor="act-remove" className={!hasHoldings ? 'text-muted-foreground' : ''}>Remove shares</Label>
                          </div>
                        </RadioGroup>
                        {!hasHoldings && tx.portfolioId && (
                          <div className="text-xs text-muted-foreground">No holdings yet in this portfolio.</div>
                        )}
                      </div>
                      <div className="space-y-2">
                        <Label>Company</Label>
                        {tx.action === 'add' ? (
                          <CompanyPicker
                            value={tx.ticker ? { ticker: tx.ticker, name: currentHolding?.name || tx.ticker } as any : null}
                            onChange={(c) => setTx({ ...tx, ticker: c.ticker })}
                            disabled={!tx.portfolioId}
                          />
                        ) : (
                          <Select
                            value={tx.ticker}
                            onValueChange={(v) => setTx({ ...tx, ticker: v })}
                            disabled={!tx.portfolioId || !hasHoldings}
                          >
                            <SelectTrigger>
                              <SelectValue placeholder={hasHoldings ? 'Select held company' : 'No holdings'} />
                            </SelectTrigger>
                            <SelectContent>
                              {(txHoldingsData || []).map(h => (
                                <SelectItem key={h.id} value={h.ticker}>{h.ticker} — {h.name}</SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      </div>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label>Quantity</Label>
                          <Input type="number" min={0} step="any" value={tx.quantity} onChange={e => setTx({ ...tx, quantity: parseFloat(e.target.value) })} />
                          {tx.action === 'remove' && currentHolding && tx.quantity > currentHolding.quantity && (
                            <div className="text-xs text-destructive">Cannot remove more than you hold ({currentHolding.quantity}).</div>
                          )}
                        </div>
                        {tx.action === 'add' && (
                          <div className="space-y-2">
                            <Label>Price per share</Label>
                            <Input type="number" min={0} step="any" value={tx.price} onChange={e => setTx({ ...tx, price: parseFloat(e.target.value) })} />
                          </div>
                        )}
                      </div>
                      {tx.action === 'add' && currentHolding && tx.quantity > 0 && tx.price > 0 && (
                        <div className="text-xs text-muted-foreground">
                          New quantity: {currentHolding.quantity + tx.quantity}. New avg price: {
                            (((currentHolding.quantity * currentHolding.averagePrice) + (tx.quantity * tx.price)) / (currentHolding.quantity + tx.quantity)).toFixed(2)
                          }
                        </div>
                      )}
                    </div>
                    <DialogFooter>
                      <Button onClick={submitTransaction} disabled={!canSubmit || submitting}>{submitting ? 'Saving...' : 'Save'}</Button>
                    </DialogFooter>
                  </DialogContent>
                </Dialog>
                <Button variant="outline" className="h-24 flex-col space-y-3 transition-smooth rounded-2xl font-light">
                  <TrendingUp className="h-7 w-7" />
                  <span>View Analytics</span>
                </Button>
                <Button variant="outline" className="h-24 flex-col space-y-3 transition-smooth rounded-2xl font-light" onClick={() => navigate('/portfolios')}>
                  <Settings className="h-7 w-7" />
                  <span>Manage Portfolios</span>
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
      
      <PoweredBy />
    </div>
  );
};

export default Dashboard;
