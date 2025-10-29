-- Alerts table
CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ticker VARCHAR(32) NOT NULL,
    condition_type VARCHAR(10) NOT NULL,
    threshold DOUBLE NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    cooldown_expires_at DATETIME NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_alert_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_company FOREIGN KEY (ticker) REFERENCES companies(ticker) ON DELETE RESTRICT
);
CREATE INDEX idx_alert_user ON alerts(user_id);
CREATE INDEX idx_alert_ticker ON alerts(ticker);

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_notification_user ON notifications(user_id);
