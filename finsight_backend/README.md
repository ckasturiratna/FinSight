# FinSight Backend Docs

## Indicators API

The indicators service exposes a candle-powered endpoint that calculates simple moving average (SMA), exponential moving average (EMA), and relative strength index (RSI) overlays from Finnhub data.

### Endpoint

```
GET /api/indicators/{ticker}
```

Ticker must correspond to a company stored in the FinSight catalogue (same validation as the price endpoints).

### Query Parameters

| Name       | Type    | Default | Notes |
|------------|---------|---------|-------|
| `resolution` | string | `D` | One of `1`, `5`, `15`, `30`, `60`, `D`, `W`, `M`. Values mirror Finnhub candle resolution. |
| `count`      | number | `180` | Max number of candles to request. The service auto-expands to include enough lookback for the largest requested period. |
| `sma`        | string | `20`  | Comma separated periods, e.g. `sma=20,50`. Empty or invalid entries are ignored. |
| `ema`        | string | `20`  | Works the same as `sma`. |
| `rsi`        | string | `14`  | Comma separated RSI periods. |

Periods are validated so only values between 2 and 365 are accepted. If no valid values remain, the default period is used.

### Response Shape

```json
{
  "ticker": "AAPL",
  "resolution": "D",
  "overlays": [
    { "key": "sma-20", "label": "SMA (20)", "type": "SMA", "period": 20 },
    { "key": "ema-20", "label": "EMA (20)", "type": "EMA", "period": 20 },
    { "key": "rsi-14", "label": "RSI (14)", "type": "RSI", "period": 14 }
  ],
  "points": [
    {
      "timestamp": 1717046400000,
      "close": 189.98,
      "overlays": {
        "sma-20": 187.34,
        "ema-20": 188.11,
        "rsi-14": 61.22
      }
    }
  ]
}
```

- `overlays` lists the active indicator definitions the service calculated.
- `points` is aligned with the candle timestamps (milliseconds). Each point contains the close price and whichever indicator values exist for that index.

### Errors

- `400 Bad Request` – unknown ticker or Finnhub returned an empty response for the requested symbol.
- `502 Bad Gateway` – Finnhub request failed or produced malformed data (logged for troubleshooting).

The service caches candle responses for each `(ticker, resolution, count)` triple for 60 seconds (`indicatorCandles` cache) to reduce API usage.

### Frontend Usage Guide

The Companies page includes a compact toggle bar labelled **Overlays**. Selecting a company and then enabling SMA, EMA, or RSI pushes the corresponding studies into the TradingView advanced chart:

1. Navigate to **Companies** and pick a ticker (this unlocks the overlay toggles).
2. Use the pill toggles to activate SMA, EMA, and/or RSI. These correspond to the same periods requested server side (defaults are SMA 20, EMA 20, RSI 14).
3. The toggles persist until changed; deactivating them removes the study from the chart without reloading the page.

### Implementation Notes

- SMA/EMA/RSI calculations live in `IndicatorCalculator` and are unit-tested.
- A Resilience4j retry configuration backs Finnhub requests while a Caffeine cache avoids repeated candle downloads.
- The service gracefully handles missing volumes or partial candle data, only emitting overlay values once enough history is available.

## Portfolio Snapshotting

Nightly snapshots capture the aggregated portfolio valuation so that historical trends stay available.

- A scheduled job in `PortfolioService` runs at 02:00 UTC each day (`app.portfolio.snapshot-cron` overrides the default) and iterates over every portfolio.
- For each holding the job multiplies quantity by the latest Finnhub price, falling back to zero when quotes are unavailable and counting those gaps in the `stale_count` column.
- The totals (invested, market value, absolute and percentage PnL) plus the capture timestamp are stored in the new `portfolio_history` table and deduplicated per `(portfolio, snapshot_date)`.
- Snapshots reuse the existing `PriceService` so quote caching and retry behaviour remain consistent with live valuation requests.

### Manual Runs & Verification

- Trigger immediately by autowiring `PortfolioService` in a temporary command-line runner (or debugger) and calling `snapshotPortfolios()`. Remove the runner afterwards to avoid duplicate scheduling.
- Unit coverage lives in `PortfolioServiceSnapshotTest`, which stubs quote responses and asserts the persisted totals. Run `./mvnw -q -Dtest=PortfolioServiceSnapshotTest test` to exercise the workflow.
- Retrieve history through `GET /api/portfolios/{id}/history`. The endpoint returns the ordered list of `PortfolioHistoryPointDto` objects the frontend consumes for its performance chart.

### Backfilled History (no nightly job required)

- When no stored snapshots exist for a portfolio, the history endpoint now auto-computes a backfilled series from Finnhub daily candles using the portfolio’s current positions.
- You can also force backfill (or choose the window) with an optional query param:
  - `GET /api/portfolios/{id}/history?backfillDays=90` – returns ~90 trading days computed from historical closes.
- Backfill logic:
  - For each holding, fetch daily close prices and aggregate market value by date: `sum(quantity × close)`.
  - Invested capital is treated as constant: `sum(quantity × averagePrice)`.
  - P/L is derived per day; `staleCount` increments for holdings missing a close on that date.
- Notes/limits:
  - Backfill reflects current quantities and averages; it does not reconstruct past transactions.
  - Data source remains Finnhub (already used elsewhere in the app). No TradingView data is queried server-side.

## Holding Thresholds

- `POST /api/portfolios/{portfolioId}/holdings` and `PUT /api/portfolios/{portfolioId}/holdings/{holdingId}` now accept optional `minThreshold` and `maxThreshold` numbers. Both values must be non-negative and, when supplied together, the minimum cannot exceed the maximum (`400 Bad Request` otherwise).
- The `portfolio_holdings` table gains two nullable columns through Flyway migration `V5__add_portfolio_holding_thresholds.sql`. Existing rows are backfilled with `NULL` so no manual data patching is required.
- `GET /api/portfolios/{portfolioId}/holdings` exposes the saved thresholds via `PortfolioHoldingDto`, enabling the UI to edit or clear the range.
- `GET /api/portfolios/{portfolioId}/value` surfaces the thresholds alongside each `HoldingValuationDto`. The frontend uses this metadata to flag positions that have drifted outside their configured band without duplicating range logic client-side.
- Unit coverage lives in `PortfolioServiceThresholdTest` (request validation/persistence) and `PortfolioValuationServiceTest` (valuation payload), keeping the happy-path flow and error handling locked down.

## Database & Migrations

This project uses MySQL and Flyway for schema migrations. The app connects using the properties in `src/main/resources/application.properties`.

### Important notes (Flyway)
- Hibernate DDL is set to `validate` so that Flyway is the single source of schema changes in non-test runs.
- If you previously ran the app and edited migration SQL files afterwards, Flyway will detect checksum mismatches and fail fast. This is by design.
- Typical recovery options:
  - Fresh dev database: run a clean and migrate.
  - Preserve data: manually fix partial objects, then `repair` to update checksums after restoring scripts or accepting new ones.

#### Clean slate (development only)
1. Ensure `spring.flyway.clean-disabled=false` (already set).
2. Drop all objects: either use your DB client or run `flyway clean` (CLI) against the same JDBC URL.
3. Start the app; Flyway will run `V0.1` … `V5` in order.

#### Keep data (repair path)
1. Migration order and content are now:
   - `V0.1` users table
   - `V0.2` companies table (+ indexes)
   - `V1` portfolios + portfolio_holdings (no threshold columns)
   - `V2` alerts + notifications tables
   - `V3` email_verifications table
   - `V4` portfolio_history table
   - `V5` add min/max thresholds to portfolio_holdings
2. Remove any half-created objects from failed migrations (e.g., partially created `portfolio_history`).
3. Run `flyway repair` to fix the failed row(s) and update checksums to match the current scripts.
4. Start the app; Flyway should validate and migrate forward.

Test runs use H2 with `create-drop` and Flyway disabled (`src/test/resources/application.properties`).
