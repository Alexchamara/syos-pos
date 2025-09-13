CREATE TABLE IF NOT EXISTS bill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    serial VARCHAR(64) NOT NULL,
    date_time DATETIME NOT NULL,
    subtotal_cents BIGINT NOT NULL,
    discount_cents BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,
    cash_cents BIGINT NOT NULL,
    change_cents BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS bill_line (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bill_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    qty INT NOT NULL,
    unit_price_cents BIGINT NOT NULL,
    line_total_cents BIGINT NOT NULL,
    CONSTRAINT fk_bill_line_bill FOREIGN KEY (bill_id) REFERENCES bill(id)
);