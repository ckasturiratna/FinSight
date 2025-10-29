import { useEffect, useMemo, useRef, useState } from 'react';
import AppHeader from '@/components/AppHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Pagination, PaginationContent, PaginationItem, PaginationNext, PaginationPrevious } from '@/components/ui/pagination';
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group';
import { Label } from '@/components/ui/label';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, Company, Page } from '@/lib/api';
import LivePriceCard from '@/components/LivePriceCard';
import TradingViewWidget from '@/components/TradingViewWidget';
import { useToast } from '@/hooks/use-toast';
import { Database, Search } from 'lucide-react';

const pageSize = 10;

const indicatorOptions = [
  { id: 'sma', label: 'SMA' },
  { id: 'ema', label: 'EMA' },
  { id: 'rsi', label: 'RSI' },
] as const;

const indicatorDefaultValues = {
  sma: '20',
  ema: '20',
  rsi: '14',
} as const;

type IndicatorToggle = (typeof indicatorOptions)[number]['id'];

const resolutionOptions = ['1', '5', '15', '30', '60', 'D', 'W', 'M'];

export default function Companies() {
  const [q, setQ] = useState('');
  const ALL = '__ALL__';
  const [sector, setSector] = useState<string | undefined>();
  const [country, setCountry] = useState<string | undefined>();
  const [sort, setSort] = useState<string>('name.asc');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [indicatorToggles, setIndicatorToggles] = useState<IndicatorToggle[]>([]);
  const [indicatorInputs, setIndicatorInputs] = useState<Record<IndicatorToggle, string>>(() => ({ ...indicatorDefaultValues }));
  const [resolution, setResolution] = useState<string>('D');

  const seedAttemptedRef = useRef(false);
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const parsedIndicatorPeriods = useMemo(() => {
    const parse = (value: string): number[] => {
      const seen = new Set<number>();
      const result: number[] = [];
      value.split(',').forEach((segment) => {
        const trimmed = segment.trim();
        if (!trimmed) return;
        const numeric = Number.parseInt(trimmed, 10);
        if (!Number.isInteger(numeric)) return;
        const bounded = Math.abs(numeric);
        if (bounded < 2 || bounded > 365 || seen.has(bounded)) return;
        seen.add(bounded);
        result.push(bounded);
      });
      return result;
    };

    return {
      sma: parse(indicatorInputs.sma),
      ema: parse(indicatorInputs.ema),
      rsi: parse(indicatorInputs.rsi),
    } as Record<IndicatorToggle, number[]>;
  }, [indicatorInputs]);

  const seedMutation = useMutation({
    mutationFn: () => api.post('/api/companies/seed'),
    onSuccess: () => {
      toast({
        title: 'Success',
        description: 'Companies seeded successfully from Finnhub API!',
      });
      queryClient.invalidateQueries({ queryKey: ['companies-list'] });
    },
    onError: (error: any) => {
      toast({
        title: 'Error',
        description: error?.response?.data?.message || 'Failed to seed companies',
        variant: 'destructive',
      });
    },
  });

  const { data, isLoading, isError } = useQuery<Page<Company>>({
    queryKey: ['companies-list', q, sector, country, sort, page],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (q) params.set('q', q);
      if (sector) params.set('sector', sector);
      if (country) params.set('country', country);
      if (sort) params.set('sort', sort);
      params.set('page', String(page));
      params.set('size', String(pageSize));

      const primary = await api.get<Page<Company>>(`/api/companies?${params.toString()}`);
      if ((primary?.content?.length ?? 0) > 0) return primary;

      if (!seedAttemptedRef.current) {
        seedAttemptedRef.current = true;
        const seedParams = new URLSearchParams();
        if (q) seedParams.set('q', q);
        seedParams.set('page', '0');
        seedParams.set('size', String(pageSize));
        try {
          await api.get<Page<Company>>(`/api/portfolios/companies?${seedParams.toString()}`);
        } catch {
          return primary;
        }
        return api.get<Page<Company>>(`/api/companies?${params.toString()}`);
      }

      return primary;
    },
  });

  const sectors = useMemo(
    () => Array.from(new Set((data?.content || []).map((c) => c.sector).filter(Boolean) as string[])),
    [data],
  );
  const countries = useMemo(
    () => Array.from(new Set((data?.content || []).map((c) => c.country).filter(Boolean) as string[])),
    [data],
  );

  useEffect(() => { setPage(0); }, [q, sector, country, sort]);

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />
      <main className="max-w-7xl mx-auto p-6">
        <div className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-[minmax(0,2.1fr)_minmax(0,1fr)]">
            <Card className="shadow-card border-0">
              <CardHeader>
                <CardTitle>Price Chart</CardTitle>
              </CardHeader>
              <CardContent>
                <TradingViewWidget
                  ticker={selected || undefined}
                  indicators={indicatorToggles}
                  periods={parsedIndicatorPeriods}
                  interval={resolution}
                  height={640}
                />
              </CardContent>
            </Card>

            <div className="flex flex-col gap-6">
              <LivePriceCard ticker={selected} />
              <Card className="shadow-card border-0">
                <CardHeader className="py-3">
                  <CardTitle className="text-base flex items-center justify-between">
                    <span>Indicators</span>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setIndicatorInputs({ ...indicatorDefaultValues })}
                    >
                      Reset
                    </Button>
                  </CardTitle>
                </CardHeader>
                <CardContent className="p-4 space-y-4">
                  <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                    <span className="font-medium text-foreground">Overlays</span>
                    <ToggleGroup
                      type="multiple"
                      className="flex gap-1"
                      value={indicatorToggles}
                      onValueChange={(value) => setIndicatorToggles(value as IndicatorToggle[])}
                    >
                      {indicatorOptions.map((option) => (
                        <ToggleGroupItem
                          key={option.id}
                          value={option.id}
                          className="h-7 rounded-full border px-3 text-xs font-medium uppercase"
                        >
                          {option.label}
                        </ToggleGroupItem>
                      ))}
                    </ToggleGroup>
                    {selected ? (
                      <span className="ml-auto hidden text-foreground md:block">{selected}</span>
                    ) : null}
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 items-end text-xs">
                    <div className="space-y-1">
                      <Label htmlFor="indicator-resolution" className="text-[11px] font-medium text-foreground">
                        Resolution
                      </Label>
                      <Select value={resolution} onValueChange={setResolution}>
                        <SelectTrigger id="indicator-resolution" className="h-8 w-full text-xs">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {resolutionOptions.map((option) => (
                            <SelectItem key={option} value={option}>{option}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <span className="text-[11px] text-muted-foreground">Matches Finnhub candle resolution.</span>
                    </div>

                    {indicatorOptions.map((option) => {
                      const applied = parsedIndicatorPeriods[option.id];
                      const helper = applied.length > 0
                        ? `Using ${applied.join(', ')}`
                        : `Using default ${indicatorDefaultValues[option.id]}`;
                      return (
                        <div key={option.id} className="space-y-1">
                          <Label htmlFor={`${option.id}-periods`} className="text-[11px] font-medium text-foreground">
                            {option.label}
                          </Label>
                          <Input
                            id={`${option.id}-periods`}
                            value={indicatorInputs[option.id]}
                            onChange={(event) => setIndicatorInputs((prev) => ({ ...prev, [option.id]: event.target.value }))}
                            placeholder={indicatorDefaultValues[option.id]}
                            inputMode="numeric"
                            className="h-8 text-xs"
                          />
                          <span className="text-[11px] text-muted-foreground">{helper}</span>
                        </div>
                      );
                    })}
                  </div>

                  <div className="text-[11px] text-muted-foreground">
                    Enter comma separated periods between 2 and 365. Duplicates and invalid values are ignored.
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>

          <Card className="shadow-card border-0">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Companies</CardTitle>
                <Button
                  onClick={() => seedMutation.mutate()}
                  disabled={seedMutation.isPending}
                  variant="outline"
                  size="sm"
                  className="flex items-center gap-2"
                >
                  <Database className="h-4 w-4" />
                  {seedMutation.isPending ? 'Seeding…' : 'Seed Companies'}
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="flex flex-col gap-3 mb-4">
                <div className="flex items-center gap-2">
                  <Search className="h-4 w-4 text-muted-foreground" />
                  <Input placeholder="Search by name or ticker" value={q} onChange={(e) => setQ(e.target.value)} />
                </div>
                <Select value={sector ?? ALL} onValueChange={(v) => setSector(v === ALL ? undefined : v)}>
                  <SelectTrigger className="w-full"><SelectValue placeholder="Sector" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value={ALL}>All Sectors</SelectItem>
                    {sectors.map((s) => (
                      <SelectItem key={s} value={s}>{s}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Select value={country ?? ALL} onValueChange={(v) => setCountry(v === ALL ? undefined : v)}>
                  <SelectTrigger className="w-full"><SelectValue placeholder="Country" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value={ALL}>All Countries</SelectItem>
                    {countries.map((c) => (
                      <SelectItem key={c} value={c}>{c}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Select value={sort} onValueChange={setSort}>
                  <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="name.asc">Name ↑</SelectItem>
                    <SelectItem value="name.desc">Name ↓</SelectItem>
                    <SelectItem value="marketCap.desc">Mkt Cap ↓</SelectItem>
                    <SelectItem value="marketCap.asc">Mkt Cap ↑</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Ticker</TableHead>
                      <TableHead>Name</TableHead>
                      <TableHead>Sector</TableHead>
                      <TableHead>Country</TableHead>
                      <TableHead className="text-right">Market Cap</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {isLoading ? (
                      <TableRow><TableCell colSpan={5} className="text-center text-muted-foreground">Loading…</TableCell></TableRow>
                    ) : isError ? (
                      <TableRow><TableCell colSpan={5} className="text-center text-destructive">Failed to load companies</TableCell></TableRow>
                    ) : !data?.content?.length ? (
                      <TableRow><TableCell colSpan={5} className="text-center text-muted-foreground">No results</TableCell></TableRow>
                    ) : (
                      data.content.map((company) => (
                        <TableRow key={company.ticker} className="cursor-pointer" onClick={() => setSelected(company.ticker)}>
                          <TableCell className="font-medium">{company.ticker}</TableCell>
                          <TableCell className="truncate">{company.name}</TableCell>
                          <TableCell>{company.sector || '—'}</TableCell>
                          <TableCell>{company.country || '—'}</TableCell>
                          <TableCell className="text-right">
                            {typeof company.marketCap === 'number'
                              ? `$${Math.round(company.marketCap / 1_000_000_000)}B`
                              : '—'}
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>

              <div className="mt-4">
                <Pagination>
                  <PaginationContent>
                    <PaginationItem>
                      <PaginationPrevious
                        href="#"
                        onClick={(event) => {
                          event.preventDefault();
                          if (page > 0) setPage(page - 1);
                        }}
                      />
                    </PaginationItem>
                    <PaginationItem>
                      <div className="px-3 py-2 text-sm text-muted-foreground">
                        Page {data ? data.number + 1 : page + 1} of {data?.totalPages ?? '—'}
                      </div>
                    </PaginationItem>
                    <PaginationItem>
                      <PaginationNext
                        href="#"
                        onClick={(event) => {
                          event.preventDefault();
                          if ((data?.number ?? 0) < ((data?.totalPages ?? 1) - 1)) {
                            setPage(page + 1);
                          }
                        }}
                      />
                    </PaginationItem>
                  </PaginationContent>
                </Pagination>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}
