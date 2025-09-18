-- If you used ENUM for location before, ALTER it to include MAIN.
-- If your 'location' is VARCHAR (most likely), nothing to change for type.
-- We'll just normalize and add helpful indexes.

-- normalize all existing to uppercase
# UPDATE batch SET location = UPPER(location);

-- helpful composite index
CREATE INDEX IF NOT EXISTS ix_batch_code_loc_exp ON batch(product_code, location, expiry, received_at);

-- OPTIONAL: a simple threshold table you can use later per location
CREATE TABLE IF NOT EXISTS stock_threshold (
    product_code VARCHAR(64) NOT NULL,
    location VARCHAR(16) NOT NULL,
    threshold INT NOT NULL,
    PRIMARY KEY (product_code, location)
);

-- default threshold for shelf only (50)
INSERT IGNORE INTO stock_threshold(product_code, location, threshold)
SELECT DISTINCT b.product_code, 'SHELF', 50 FROM batch b;