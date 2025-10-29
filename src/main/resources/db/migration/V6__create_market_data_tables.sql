-- Historical market candles per day
CREATE TABLE IF NOT EXISTS historical_stock_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticker VARCHAR(32) NOT NULL,
    date DATE,
    `open` DOUBLE,
    `close` DOUBLE,
    high DOUBLE,
    low DOUBLE,
    volume BIGINT,
    CONSTRAINT fk_hist_company FOREIGN KEY (ticker) REFERENCES companies(ticker) ON DELETE CASCADE
);
CREATE INDEX idx_hist_ticker_date ON historical_stock_data (ticker, date);

-- Intraday/streamed quotes snapshot
CREATE TABLE IF NOT EXISTS stock_quotes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticker VARCHAR(32) NOT NULL,
    current_price DOUBLE,
    volume BIGINT,
    percent_change DOUBLE,
    high DOUBLE,
    low DOUBLE,
    `timestamp` DATETIME,
    CONSTRAINT fk_quote_company FOREIGN KEY (ticker) REFERENCES companies(ticker) ON DELETE CASCADE
);
CREATE INDEX idx_ticker_timestamp ON stock_quotes (ticker, `timestamp`);

