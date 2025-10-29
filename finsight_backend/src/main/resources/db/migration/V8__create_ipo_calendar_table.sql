-- Create IPO Calendar table
CREATE TABLE ipo_calendar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    exchange VARCHAR(100),
    number_of_shares BIGINT,
    price VARCHAR(50),
    status VARCHAR(50),
    total_shares_value DECIMAL(20,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_symbol (symbol),
    INDEX idx_date (date),
    INDEX idx_status (status)
);
