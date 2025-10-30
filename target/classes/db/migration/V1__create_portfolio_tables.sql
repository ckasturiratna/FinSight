-- Portfolios and holdings
CREATE TABLE IF NOT EXISTS portfolios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    user_id BIGINT NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_portfolio_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_portfolio_user ON portfolios(user_id);

CREATE TABLE IF NOT EXISTS portfolio_holdings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    portfolio_id BIGINT NOT NULL,
    ticker VARCHAR(32) NOT NULL,
    quantity DOUBLE NOT NULL,
    avg_price DOUBLE NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_holding_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    CONSTRAINT fk_holding_company FOREIGN KEY (ticker) REFERENCES companies(ticker) ON DELETE RESTRICT
);
CREATE INDEX idx_holding_portfolio ON portfolio_holdings(portfolio_id);
CREATE INDEX idx_holding_ticker ON portfolio_holdings(ticker);
