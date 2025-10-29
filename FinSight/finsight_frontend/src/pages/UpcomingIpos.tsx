import AppHeader from '@/components/AppHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { useQuery } from '@tanstack/react-query';
import { ipoApi, Ipo } from '@/lib/api';
import { CalendarDays, RefreshCw } from 'lucide-react';

export default function UpcomingIpos() {
  const { data, isLoading, isError, refetch, isFetching } = useQuery<Ipo[]>({
    queryKey: ['ipo-upcoming'],
    queryFn: () => ipoApi.upcoming(),
  });

  const onRefresh = async () => {
    try {
      await ipoApi.fetchNow();
    } catch {}
    await refetch();
  };

  const rows = data || [];

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />
      <main className="max-w-7xl mx-auto p-6">
        <Card className="shadow-card border-0">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <CalendarDays className="h-5 w-5" />
              Upcoming IPOs (today & tomorrow)
            </CardTitle>
            <Button variant="outline" size="sm" onClick={onRefresh} disabled={isFetching}>
              <RefreshCw className={`h-4 w-4 mr-2 ${isFetching ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
          </CardHeader>
          <CardContent>
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Symbol</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Exchange</TableHead>
                    <TableHead className="text-right">Shares</TableHead>
                    <TableHead className="text-right">Price</TableHead>
                    <TableHead className="text-right">Total Value</TableHead>
                    <TableHead>Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {isLoading ? (
                    <TableRow><TableCell colSpan={8} className="text-center text-muted-foreground">Loading...</TableCell></TableRow>
                  ) : isError ? (
                    <TableRow><TableCell colSpan={8} className="text-center text-destructive">Failed to load IPOs</TableCell></TableRow>
                  ) : rows.length === 0 ? (
                    <TableRow><TableCell colSpan={8} className="text-center text-muted-foreground">No upcoming IPOs</TableCell></TableRow>
                  ) : (
                    rows.map((i) => (
                      <TableRow key={`${i.date}-${i.symbol}`}>
                        <TableCell>{i.date}</TableCell>
                        <TableCell className="font-medium">{i.symbol || '—'}</TableCell>
                        <TableCell className="truncate max-w-[320px]">{i.name}</TableCell>
                        <TableCell>{i.exchange || '—'}</TableCell>
                        <TableCell className="text-right">{i.numberOfShares ?? '—'}</TableCell>
                        <TableCell className="text-right">{i.price ?? '—'}</TableCell>
                        <TableCell className="text-right">{typeof i.totalSharesValue === 'number' ? i.totalSharesValue.toLocaleString() : '—'}</TableCell>
                        <TableCell>{i.status || '—'}</TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      </main>
    </div>
  );
}


