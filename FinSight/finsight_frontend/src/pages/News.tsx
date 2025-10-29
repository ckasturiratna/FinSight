import { useEffect, useMemo, useRef, useState } from 'react';
import AppHeader from '@/components/AppHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Pagination, PaginationContent, PaginationItem, PaginationNext, PaginationPrevious } from '@/components/ui/pagination';
import { useQuery } from '@tanstack/react-query';
import { api, Company, Page } from '@/lib/api';
import { Search, ExternalLink, Calendar, Newspaper } from 'lucide-react';

const pageSize = 10;

type NewsArticle = {
  id: number;
  headline: string;
  summary: string;
  url: string;
  source: string;
  datetime: number;
  image?: string;
  category: string;
  related: string[];
};

export default function News() {
  const [q, setQ] = useState('');
  const ALL = '__ALL__';
  const [sector, setSector] = useState<string | undefined>(undefined);
  const [country, setCountry] = useState<string | undefined>(undefined);
  const [sort, setSort] = useState<string>('name.asc');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [newsDays, setNewsDays] = useState<number>(30);

  const seedAttemptedRef = useRef(false);

  // Fetch companies with auto-seed fallback when DB is empty
  const { data, isLoading, isError, refetch } = useQuery<Page<Company>>({
    queryKey: ['companies-list', q, sector, country, sort, page],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (q) params.set('q', q);
      if (sector) params.set('sector', sector);
      if (country) params.set('country', country);
      if (sort) params.set('sort', sort);
      params.set('page', String(page));
      params.set('size', String(pageSize));
      // 1) Try DB-backed endpoint first
      const primary = await api.get<Page<Company>>(`/api/companies?${params.toString()}`);
      if ((primary?.content?.length ?? 0) > 0) return primary;

      // 2) If empty, attempt one-time auto-fetch via portfolio endpoint
      if (!seedAttemptedRef.current) {
        seedAttemptedRef.current = true;
        const seedParams = new URLSearchParams();
        if (q) seedParams.set('q', q);
        seedParams.set('page', '0');
        seedParams.set('size', String(pageSize));
        try {
          await api.get<Page<Company>>(`/api/portfolios/companies?${seedParams.toString()}`);
        } catch {
          // ignore seeding errors and return the primary result
          return primary;
        }
        // 3) Re-query DB after seeding
        return api.get<Page<Company>>(`/api/companies?${params.toString()}`);
      }

      return primary;
    },
  });

  // Fetch news for selected company
  const { data: newsData, isLoading: newsLoading, isError: newsError } = useQuery<NewsArticle[]>({
    queryKey: ['company-news', selected, newsDays],
    queryFn: async () => {
      if (!selected) return [];
      const response = await api.get<NewsArticle[]>(`/api/companies/${selected}/news?days=${newsDays}`);
      return response || [];
    },
    enabled: !!selected,
  });

  // Derive sector/country options from current result page (fallback when no taxonomy API)
  const sectors = useMemo(() => Array.from(new Set((data?.content || []).map(c => c.sector).filter(Boolean) as string[])), [data]);
  const countries = useMemo(() => Array.from(new Set((data?.content || []).map(c => c.country).filter(Boolean) as string[])), [data]);

  // Reset page when filters change
  useEffect(() => { setPage(0); }, [q, sector, country, sort]);

  const formatDate = (timestamp: number) => {
    return new Date(timestamp * 1000).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />
      <main className="max-w-7xl mx-auto p-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-4">
            <Card className="shadow-card border-0">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Newspaper className="h-5 w-5" />
                  Company News
                </CardTitle>
              </CardHeader>
              <CardContent>
                {!selected ? (
                  <div className="h-64 flex items-center justify-center text-muted-foreground">
                    <div className="text-center">
                      <Newspaper className="h-12 w-12 mx-auto mb-4 opacity-50" />
                      <p>Select a company to view news</p>
                    </div>
                  </div>
                ) : newsLoading ? (
                  <div className="h-64 flex items-center justify-center text-muted-foreground">
                    Loading news for {selected}...
                  </div>
                ) : newsError ? (
                  <div className="h-64 flex items-center justify-center text-destructive">
                    Failed to load news for {selected}
                  </div>
                ) : !newsData?.length ? (
                  <div className="h-64 flex items-center justify-center text-muted-foreground">
                    <div className="text-center">
                      <Newspaper className="h-12 w-12 mx-auto mb-4 opacity-50" />
                      <p>No news found for {selected}</p>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <h3 className="text-lg font-semibold">Latest News for {selected}</h3>
                      <Select value={newsDays.toString()} onValueChange={(v) => setNewsDays(Number(v))}>
                        <SelectTrigger className="w-32">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="7">Last 7 days</SelectItem>
                          <SelectItem value="30">Last 30 days</SelectItem>
                          <SelectItem value="90">Last 90 days</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-3">
                      {newsData.map((article) => (
                        <Card key={article.id} className="border-l-4 border-l-blue-500">
                          <CardContent className="p-4">
                            <div className="flex items-start gap-3">
                              {article.image && (
                                <img 
                                  src={article.image} 
                                  alt={article.headline}
                                  className="w-16 h-16 object-cover rounded"
                                  onError={(e) => {
                                    (e.target as HTMLImageElement).style.display = 'none';
                                  }}
                                />
                              )}
                              <div className="flex-1 min-w-0">
                                <h4 className="font-semibold text-sm leading-tight mb-2 line-clamp-2">
                                  {article.headline}
                                </h4>
                                <p className="text-xs text-muted-foreground mb-2 line-clamp-2">
                                  {article.summary}
                                </p>
                                <div className="flex items-center gap-4 text-xs text-muted-foreground">
                                  <span className="flex items-center gap-1">
                                    <Calendar className="h-3 w-3" />
                                    {formatDate(article.datetime)}
                                  </span>
                                  <span>{article.source}</span>
                                  <span className="capitalize">{article.category}</span>
                                </div>
                              </div>
                              <Button
                                variant="ghost"
                                size="sm"
                                asChild
                                className="shrink-0"
                              >
                                <a href={article.url} target="_blank" rel="noopener noreferrer">
                                  <ExternalLink className="h-4 w-4" />
                                </a>
                              </Button>
                            </div>
                          </CardContent>
                        </Card>
                      ))}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="space-y-4">
            <Card className="shadow-card border-0">
              <CardHeader>
                <CardTitle>Companies</CardTitle>
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
                      {sectors.map(s => <SelectItem key={s} value={s}>{s}</SelectItem>)}
                    </SelectContent>
                  </Select>
                  <Select value={country ?? ALL} onValueChange={(v) => setCountry(v === ALL ? undefined : v)}>
                    <SelectTrigger className="w-full"><SelectValue placeholder="Country" /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value={ALL}>All Countries</SelectItem>
                      {countries.map(c => <SelectItem key={c} value={c}>{c}</SelectItem>)}
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
                        <TableHead className="text-right">Market Cap</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {isLoading ? (
                        <TableRow><TableCell colSpan={3} className="text-center text-muted-foreground">Loading...</TableCell></TableRow>
                      ) : isError ? (
                        <TableRow><TableCell colSpan={3} className="text-center text-destructive">Failed to load companies</TableCell></TableRow>
                      ) : !data?.content?.length ? (
                        <TableRow><TableCell colSpan={3} className="text-center text-muted-foreground">No results</TableCell></TableRow>
                      ) : (
                        data!.content.map((c) => (
                          <TableRow 
                            key={c.ticker} 
                            className={`cursor-pointer ${selected === c.ticker ? 'bg-muted' : ''}`}
                            onClick={() => setSelected(c.ticker)}
                          >
                            <TableCell className="font-medium">{c.ticker}</TableCell>
                            <TableCell className="truncate">{c.name}</TableCell>
                            <TableCell className="text-right">{typeof c.marketCap === 'number' ? `$${Math.round(c.marketCap / 1_000_000_000)}B` : '—'}</TableCell>
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
                        <PaginationPrevious href="#" onClick={(e) => { e.preventDefault(); if (page > 0) setPage(page - 1); }} />
                      </PaginationItem>
                      <PaginationItem>
                        <div className="px-3 py-2 text-sm text-muted-foreground">Page {data ? data.number + 1 : page + 1} of {data?.totalPages ?? '—'}</div>
                      </PaginationItem>
                      <PaginationItem>
                        <PaginationNext href="#" onClick={(e) => { e.preventDefault(); if ((data?.number ?? 0) < ((data?.totalPages ?? 1) - 1)) setPage(page + 1); }} />
                      </PaginationItem>
                    </PaginationContent>
                  </Pagination>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
}
