-- Seed data for household products (idempotent)
INSERT IGNORE INTO product (code, name, price_cents) VALUES
-- Cleaning Products
('CLN001', 'All-Purpose Cleaner 500ml', 299),
('CLN002', 'Dish Soap 750ml', 199);
# ('CLN003', 'Laundry Detergent 1L', 599),
# ('CLN004', 'Toilet Paper 12-pack', 899),
# ('CLN005', 'Paper Towels 6-pack', 699),
# ('CLN006', 'Glass Cleaner 500ml', 249),
# ('CLN007', 'Floor Cleaner 1L', 399),
#
# -- Kitchen Items
# ('KTC001', 'Aluminum Foil 25ft', 349),
# ('KTC002', 'Plastic Wrap 100ft', 279),
# ('KTC003', 'Trash Bags 30-count', 799),
# ('KTC004', 'Food Storage Containers Set', 1299),
# ('KTC005', 'Paper Plates 50-pack', 599),
# ('KTC006', 'Disposable Cups 100-pack', 449),
#
# -- Personal Care
# ('PRC001', 'Shampoo 400ml', 699),
# ('PRC002', 'Body Soap Bar 3-pack', 399),
# ('PRC003', 'Toothpaste 100ml', 299),
# ('PRC004', 'Deodorant Stick', 499),
# ('PRC005', 'Hand Sanitizer 250ml', 199),
#
# -- Home Essentials
# ('HME001', 'Light Bulbs LED 4-pack', 1199),
# ('HME002', 'Batteries AA 8-pack', 899),
# ('HME003', 'Batteries AAA 8-pack', 899),
# ('HME004', 'Extension Cord 6ft', 1599),
# ('HME005', 'Air Freshener Spray', 349);

-- Seed data for product batches (idempotent with unique index on batch identity)
INSERT IGNORE INTO batch (product_code, location, received_at, expiry, quantity, version) VALUES
-- MAIN_STORE Batches (Initial supplier deliveries)
('CLN001', 'MAIN_STORE', '2024-07-28 08:00:00', '2026-08-01', 200, 0),
('CLN002', 'MAIN_STORE', '2024-08-01 09:00:00', '2025-12-31', 150, 0),
# ('CLN003', 'MAIN_STORE', '2024-07-15 07:30:00', '2025-07-20', 180, 0),
# ('CLN004', 'MAIN_STORE', '2024-08-05 10:00:00', NULL, 300, 0),
# ('CLN005', 'MAIN_STORE', '2024-08-03 11:00:00', NULL, 250, 0),
# ('CLN006', 'MAIN_STORE', '2024-07-30 08:30:00', '2026-02-01', 120, 0),
# ('CLN007', 'MAIN_STORE', '2024-08-02 09:15:00', '2025-11-30', 160, 0),
#
# ('KTC001', 'MAIN_STORE', '2024-08-08 08:45:00', NULL, 220, 0),
# ('KTC002', 'MAIN_STORE', '2024-08-06 10:20:00', NULL, 200, 0),
# ('KTC003', 'MAIN_STORE', '2024-08-04 14:30:00', NULL, 280, 0),
# ('KTC004', 'MAIN_STORE', '2024-07-20 13:00:00', NULL, 100, 0),
# ('KTC005', 'MAIN_STORE', '2024-08-07 09:00:00', NULL, 350, 0),
# ('KTC006', 'MAIN_STORE', '2024-08-02 11:30:00', NULL, 500, 0),
#
# ('PRC001', 'MAIN_STORE', '2024-07-25 08:30:00', '2025-08-02', 180, 0),
# ('PRC002', 'MAIN_STORE', '2024-07-28 09:15:00', '2026-01-15', 240, 0),
# ('PRC003', 'MAIN_STORE', '2024-07-22 11:45:00', '2025-07-28', 300, 0),
# ('PRC004', 'MAIN_STORE', '2024-07-26 14:20:00', '2025-06-01', 150, 0),
# ('PRC005', 'MAIN_STORE', '2024-08-10 10:10:00', '2025-03-18', 200, 0),
#
# ('HME001', 'MAIN_STORE', '2024-08-05 09:00:00', NULL, 120, 0),
# ('HME002', 'MAIN_STORE', '2024-07-30 13:15:00', '2027-08-05', 180, 0),
# ('HME003', 'MAIN_STORE', '2024-07-30 13:20:00', '2027-08-05', 180, 0),
# ('HME004', 'MAIN_STORE', '2024-07-25 16:30:00', NULL, 80, 0),
# ('HME005', 'MAIN_STORE', '2024-08-12 11:30:00', '2025-02-17', 160, 0),

-- Cleaning Products Batches (Distributed from main store)
('CLN001', 'SHELF', '2024-08-01 09:00:00', '2026-08-01', 50, 0),
('CLN001', 'WEB', '2024-08-15 10:30:00', '2026-08-01', 25, 0)
# ('CLN002', 'SHELF', '2024-08-05 11:00:00', '2025-12-31', 75, 0),
# ('CLN003', 'SHELF', '2024-07-20 08:00:00', '2025-07-20', 40, 0),
# ('CLN003', 'WEB', '2024-08-10 14:00:00', '2025-07-20', 30, 0),
# ('CLN004', 'SHELF', '2024-08-12 16:00:00', NULL, 100, 0),
# ('CLN005', 'SHELF', '2024-08-08 12:00:00', NULL, 60, 0),
# ('CLN006', 'SHELF', '2024-08-03 13:30:00', '2026-02-01', 35, 0),
# ('CLN007', 'SHELF', '2024-08-06 10:15:00', '2025-11-30', 45, 0),
#
# -- Kitchen Items Batches (Distributed from main store)
# ('KTC001', 'SHELF', '2024-08-14 09:45:00', NULL, 80, 0),
# ('KTC002', 'SHELF', '2024-08-11 11:20:00', NULL, 70, 0),
# ('KTC003', 'SHELF', '2024-08-09 15:30:00', NULL, 90, 0),
# ('KTC003', 'WEB', '2024-08-20 16:45:00', NULL, 40, 0),
# ('KTC004', 'SHELF', '2024-07-25 14:00:00', NULL, 25, 0),
# ('KTC005', 'SHELF', '2024-08-13 10:00:00', NULL, 120, 0),
# ('KTC006', 'SHELF', '2024-08-07 12:30:00', NULL, 200, 0),
#
# -- Personal Care Batches (Distributed from main store)
# ('PRC001', 'SHELF', '2024-08-02 08:30:00', '2025-08-02', 60, 0),
# ('PRC001', 'WEB', '2024-08-16 13:00:00', '2025-08-02', 35, 0),
# ('PRC002', 'SHELF', '2024-08-04 09:15:00', '2026-01-15', 80, 0),
# ('PRC003', 'SHELF', '2024-07-28 11:45:00', '2025-07-28', 100, 0),
# ('PRC004', 'SHELF', '2024-08-01 14:20:00', '2025-06-01', 45, 0),
# ('PRC005', 'SHELF', '2024-08-18 10:10:00', '2025-03-18', 70, 0),
# ('PRC005', 'WEB', '2024-08-22 15:00:00', '2025-03-18', 30, 0),
#
# -- Home Essentials Batches (Distributed from main store)
# ('HME001', 'SHELF', '2024-08-10 09:00:00', NULL, 40, 0),
# ('HME002', 'SHELF', '2024-08-05 13:15:00', '2027-08-05', 60, 0),
# ('HME003', 'SHELF', '2024-08-05 13:20:00', '2027-08-05', 60, 0),
# ('HME004', 'SHELF', '2024-07-30 16:30:00', NULL, 20, 0),
# ('HME005', 'SHELF', '2024-08-17 11:30:00', '2025-02-17', 55, 0),
# ('HME005', 'WEB', '2024-08-19 14:45:00', '2025-02-17', 25, 0);
