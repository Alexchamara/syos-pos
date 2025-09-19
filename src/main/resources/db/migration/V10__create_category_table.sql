-- Create category table
CREATE TABLE IF NOT EXISTS category (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    prefix VARCHAR(10) NOT NULL UNIQUE,
    next_sequence INT NOT NULL DEFAULT 1,
    display_order INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add category_code column to product table
ALTER TABLE product
ADD COLUMN category_code VARCHAR(20),
ADD CONSTRAINT fk_product_category
    FOREIGN KEY (category_code) REFERENCES category(code);

-- Seed initial categories with meaningful prefixes
INSERT IGNORE INTO category (code, name, description, prefix, display_order) VALUES
('CLEANING', 'Cleaning Products', 'Household cleaning and sanitizing products', 'CLN', 1),
('KITCHEN', 'Kitchen Items', 'Kitchen accessories and disposable items', 'KTC', 2),
('PERSONAL_CARE', 'Personal Care', 'Health and hygiene products', 'PRC', 3),
('HOME_ESSENTIALS', 'Home Essentials', 'Basic household utilities and electronics', 'HME', 4),
('BEVERAGES', 'Beverages', 'Drinks and liquid refreshments', 'BEV', 5),
('SNACKS', 'Snacks & Confectionery', 'Snacks, candies and treats', 'SNK', 6);

-- Update existing products to have categories based on their current codes
UPDATE product SET category_code = 'CLEANING' WHERE code LIKE 'CLN%';
UPDATE product SET category_code = 'KITCHEN' WHERE code LIKE 'KTC%';
UPDATE product SET category_code = 'PERSONAL_CARE' WHERE code LIKE 'PRC%';
UPDATE product SET category_code = 'HOME_ESSENTIALS' WHERE code LIKE 'HME%';

-- Update next_sequence for categories based on existing products
UPDATE category c SET next_sequence = (
    SELECT COALESCE(MAX(CAST(SUBSTRING(p.code, LENGTH(c.prefix) + 1) AS UNSIGNED)), 0) + 1
    FROM product p
    WHERE p.code LIKE CONCAT(c.prefix, '%')
    AND p.code REGEXP CONCAT('^', c.prefix, '[0-9]+$')
);
