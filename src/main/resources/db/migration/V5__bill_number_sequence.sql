CREATE TABLE IF NOT EXISTS bill_number (
    scope     VARCHAR(32) PRIMARY KEY,
    next_val  BIGINT NOT NULL
    );

INSERT IGNORE INTO bill_number(scope, next_val) VALUES ('COUNTER', 1), ('WEB', 1);

-- Make bill.serial unique (if not already)
ALTER TABLE bill
DROP INDEX IF EXISTS uq_bill_serial,
ADD UNIQUE KEY uq_bill_serial (serial);