CREATE TABLE IF NOT EXISTS inventory_movement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    happened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_code VARCHAR(64) NOT NULL,
    from_location VARCHAR(16) NULL,
    to_location   VARCHAR(16) NULL,
    quantity INT NOT NULL,
    note VARCHAR(255) NULL
);
CREATE INDEX IF NOT EXISTS ix_mov_product_time ON inventory_movement(product_code, happened_at);