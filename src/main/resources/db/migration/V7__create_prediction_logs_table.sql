CREATE TABLE prediction_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    predicted_price DOUBLE NOT NULL,
    actual_price DOUBLE
);
