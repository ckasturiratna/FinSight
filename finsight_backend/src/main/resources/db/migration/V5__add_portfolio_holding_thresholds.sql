-- MySQL compatibility: avoid IF NOT EXISTS for columns
ALTER TABLE portfolio_holdings
    ADD COLUMN min_threshold DOUBLE NULL;

ALTER TABLE portfolio_holdings
    ADD COLUMN max_threshold DOUBLE NULL;
