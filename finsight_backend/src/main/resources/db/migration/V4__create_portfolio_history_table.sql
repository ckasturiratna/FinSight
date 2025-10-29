CREATE TABLE IF NOT EXISTS portfolio_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    portfolio_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invested DOUBLE NOT NULL DEFAULT 0,
    market_value DOUBLE NOT NULL DEFAULT 0,
    pnl_abs DOUBLE NOT NULL DEFAULT 0,
    pnl_pct DOUBLE NOT NULL DEFAULT 0,
    stale_count INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_portfolio_history_portfolio FOREIGN KEY (portfolio_id)
        REFERENCES portfolios(id) ON DELETE CASCADE,
    CONSTRAINT uq_portfolio_history_day UNIQUE (portfolio_id, snapshot_date)
);

CREATE INDEX idx_history_portfolio ON portfolio_history (portfolio_id);
CREATE INDEX idx_history_date ON portfolio_history (snapshot_date);
