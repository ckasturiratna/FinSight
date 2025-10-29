import { useEffect, useMemo, useRef, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { api } from '@/lib/api';

type StockPrice = {
  ticker: string;
  currentPrice?: number;
  volume?: number;
  percentChange?: number;
  high?: number;
  low?: number;
  timestamp?: string;
};

function wsBaseUrl(): string {
  const apiBase = (import.meta as any).env?.VITE_API_BASE || 'http://localhost:8080';
  return apiBase.replace(/^http/, 'ws');
}

type Props = { ticker?: string | null };

export default function LivePriceCard({ ticker }: Props) {
  const [data, setData] = useState<StockPrice | null>(null);
  const [status, setStatus] = useState<'idle' | 'connecting' | 'open' | 'closed' | 'error'>('idle');
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    // Cleanup existing socket when ticker changes
    if (wsRef.current) {
      try { wsRef.current.close(); } catch {}
      wsRef.current = null;
    }
    setData(null);
    setStatus('idle');

    if (!ticker) return;

    let cancelled = false;

    // Seed with latest REST price immediately
    api.get<StockPrice>(`/api/price/${encodeURIComponent(ticker)}`)
      .then((d) => { if (!cancelled) setData(d); })
      .catch(() => {});

    // Open WebSocket for live ticks
    setStatus('connecting');
    const url = `${wsBaseUrl()}/ws/price/${encodeURIComponent(ticker)}`;
    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onopen = () => setStatus('open');
    ws.onerror = () => setStatus('error');
    ws.onclose = () => setStatus('closed');
    ws.onmessage = (evt) => {
      try {
        const payload = JSON.parse(evt.data);
        // Expect StockPriceDto shape; merge partial trade updates
        setData((prev) => ({ ...(prev || {}), ...(payload || {}), ticker }));
      } catch {
        // ignore
      }
    };

    return () => {
      cancelled = true;
      try { ws.close(); } catch {}
      wsRef.current = null;
    };
  }, [ticker]);

  const diff = useMemo(() => {
    if (!data?.currentPrice || data.percentChange == null) return null;
    const sign = data.percentChange >= 0 ? '+' : '';
    return `${sign}${data.percentChange.toFixed(2)}%`;
  }, [data]);

  return (
    <Card className="shadow-card border-0">
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          <span>Live Price</span>
          <div className="flex items-center gap-2">
            {ticker ? (
              <Badge variant="outline">{ticker}</Badge>
            ) : (
              <span className="text-sm text-muted-foreground">Select a company</span>
            )}
            {ticker && (
              <Badge variant={status === 'open' ? 'default' : status === 'error' ? 'destructive' : 'outline'}>
                {status}
              </Badge>
            )}
          </div>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {!ticker ? (
          <div className="h-32 flex items-center justify-center text-muted-foreground">No ticker selected</div>
        ) : !data ? (
          <div className="space-y-3">
            <Skeleton className="h-8 w-40" />
            <Skeleton className="h-4 w-72" />
            <Skeleton className="h-4 w-56" />
          </div>
        ) : (
          <div className="space-y-2">
            <div className="text-3xl font-semibold">
              {typeof data.currentPrice === 'number' ? `$${data.currentPrice.toFixed(2)}` : '—'}
              {diff && (
                <span className={"ml-3 text-base " + (data.percentChange! >= 0 ? 'text-green-600' : 'text-red-600')}>
                  {diff}
                </span>
              )}
            </div>
            <div className="text-sm text-muted-foreground">
              High: {data.high != null ? `$${data.high.toFixed?.(2) ?? data.high}` : '—'} · Low: {data.low != null ? `$${data.low.toFixed?.(2) ?? data.low}` : '—'} · Vol: {data.volume ?? '—'}
            </div>
            <div className="text-xs text-muted-foreground">
              Updated: {data.timestamp ? new Date(data.timestamp).toLocaleTimeString() : '—'}
            </div>
            {status !== 'open' && ticker && (
              <div className="pt-2">
                <Button variant="outline" size="sm" onClick={() => {
                  // retrigger effect by setting same ticker -> close/open
                  const t = wsRef.current; try { t?.close(); } catch {}
                }}>Reconnect</Button>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

