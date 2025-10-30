-- Companies table
CREATE TABLE IF NOT EXISTS companies (
    ticker      VARCHAR(32) PRIMARY KEY,
    name        VARCHAR(255),
    sector      VARCHAR(128),
    country     VARCHAR(64),
    market_cap  BIGINT,
    description TEXT
);
CREATE INDEX idx_name ON companies(name);
CREATE INDEX idx_sector ON companies(sector);
CREATE INDEX idx_country ON companies(country);
