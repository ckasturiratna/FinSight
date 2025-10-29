import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api, Company, Page } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command';

type Props = {
  value?: Company | null;
  onChange: (company: Company) => void;
  disabled?: boolean;
};

export default function CompanyPicker({ value, onChange, disabled }: Props) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Company | null>(value ?? null);

  useEffect(() => {
    setSelected(value ?? null);
  }, [value]);

  const { data, isLoading, isError, error } = useQuery<Page<Company>>({
    queryKey: ['companies', search],
    enabled: open,
    queryFn: () => {
      const q = search ? `&q=${encodeURIComponent(search)}` : '';
      // Use portfolio-scoped endpoint that auto-fetches from Finnhub
      return api.get(`/api/portfolios/companies?page=0&size=10${q}`);
    },
  });

  const label = selected ? `${selected.ticker} — ${selected.name}` : 'Select company...';
  const companies = data?.content || [];

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button type="button" variant="outline" role="combobox" aria-expanded={open} className="justify-between w-full" disabled={disabled}>
          {label}
          <span className="ml-2 text-muted-foreground">▾</span>
        </Button>
      </PopoverTrigger>
      <PopoverContent className="p-0 w-[420px]" align="start">
        <Command>
          <CommandInput placeholder="Search company/ticker..." value={search} onValueChange={setSearch} />
          <ScrollArea className="h-64">
            <CommandList>
              <CommandEmpty>
                {isLoading ? 'Loading...' : isError ? (error as any)?.message || 'Error loading companies' : 'No company found'}
              </CommandEmpty>
              <CommandGroup heading="Companies">
                {companies.map((c) => (
                  <CommandItem key={c.ticker} value={c.ticker} onSelect={() => { setSelected(c); onChange(c); setOpen(false); }}>
                    <div className="flex items-center justify-between w-full">
                      <div>
                        <div className="font-medium text-foreground">{c.ticker} — {c.name}</div>
                        <div className="text-xs text-muted-foreground">{c.sector || '—'} · {c.country || '—'}</div>
                      </div>
                      {typeof c.marketCap === 'number' && (
                        <div className="text-xs text-muted-foreground">MC: ${Math.round(c.marketCap / 1_000_000_000)}B</div>
                      )}
                    </div>
                  </CommandItem>
                ))}
              </CommandGroup>
            </CommandList>
          </ScrollArea>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
