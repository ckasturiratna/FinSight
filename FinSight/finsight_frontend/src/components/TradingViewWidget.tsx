import { memo, useEffect, useMemo, useRef } from 'react';

type IndicatorToggle = 'sma' | 'ema' | 'rsi';

type Props = {
  ticker?: string | null;
  indicators?: IndicatorToggle[];
  periods?: Partial<Record<IndicatorToggle, number[]>>;
  height?: number;
  interval?: string;
};

const INDICATOR_ORDER: IndicatorToggle[] = ['sma', 'ema', 'rsi'];
const STUDY_MAP: Record<IndicatorToggle, string> = {
  sma: 'MASimple@tv-basicstudies',
  ema: 'MAExp@tv-basicstudies',
  rsi: 'RSI@tv-basicstudies',
};

function TradingViewWidget({ ticker, indicators, periods, height = 380, interval = 'D' }: Props) {
  const container = useRef<HTMLDivElement | null>(null);

  const symbol = ticker ? ticker : 'NASDAQ:AAPL';
  const containerHeight = Math.max(height, 320);

  const studyConfigs = useMemo(() => {
    return INDICATOR_ORDER.filter((id) => indicators?.includes(id)).map((id) => {
      const values = periods?.[id];
      const length = values && values.length > 0 ? values[0] : undefined;
      if (length) {
        return { id: STUDY_MAP[id], inputs: { length } };
      }
      return { id: STUDY_MAP[id] };
    });
  }, [indicators, periods]);

  const configString = useMemo(() => {
    const widgetInterval = interval && interval.trim() ? interval : 'D';
    const config = {
      allow_symbol_change: true,
      calendar: false,
      details: false,
      hide_side_toolbar: true,
      hide_top_toolbar: false,
      hide_legend: false,
      hide_volume: false,
      hotlist: false,
      interval: widgetInterval,
      locale: 'en',
      save_image: true,
      style: '1',
      symbol,
      theme: 'light',
      timezone: 'Asia/Colombo',
      backgroundColor: '#ffffff',
      gridColor: 'rgba(46, 46, 46, 0.06)',
      watchlist: [] as string[],
      withdateranges: false,
      compareSymbols: [] as string[],
      studies: studyConfigs,
      autosize: true,
      height: containerHeight,
    };
    return JSON.stringify(config);
  }, [symbol, studyConfigs, containerHeight, interval]);

  useEffect(() => {
    if (!container.current) return;

    container.current.innerHTML = '';

    const script = document.createElement('script');
    script.src = 'https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js';
    script.type = 'text/javascript';
    script.async = true;
    script.innerHTML = configString;

    container.current.appendChild(script);
  }, [configString]);

  return (
    <div
      className="tradingview-widget-container overflow-hidden rounded-xl border border-border/60 bg-card"
      ref={container}
      style={{ height: `${containerHeight}px`, minHeight: `${containerHeight}px`, width: '100%' }}
    >
      <div
        className="tradingview-widget-container__widget"
        style={{ height: `${Math.max(containerHeight - 24, 280)}px`, width: '100%' }}
      />
      <div className="tradingview-widget-copyright">
        <a
          href={ticker ? `https://www.tradingview.com/symbols/${encodeURIComponent(ticker)}/` : 'https://www.tradingview.com/'}
          rel="noopener nofollow"
          target="_blank"
        >
          <span className="blue-text">{ticker || 'Symbol'} stock chart</span>
        </a>
        <span className="trademark"> by TradingView</span>
      </div>
    </div>
  );
}

export default memo(TradingViewWidget);
