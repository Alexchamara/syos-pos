CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    price_cents BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(64) NOT NULL,
    location ENUM('SHELF','WEB') NOT NULL,
    received_at DATETIME NOT NULL,
    expiry DATE NULL,
    quantity INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_batch_product FOREIGN KEY (product_code) REFERENCES product(code)
);

CREATE INDEX idx_batch_lookup
    ON batch (product_code, location, expiry, received_at);

-- Ensure each batch is unique per product, location, received_at, and expiry to allow idempotent seeding
CREATE UNIQUE INDEX uniq_batch_identity
    ON batch (product_code, location, received_at, expiry);