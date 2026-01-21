-- ============================================
-- SEED DATA FOR HUNG HYPEBEAST E-COMMERCE
-- ============================================

-- Clear existing data (in reverse order of dependencies)
TRUNCATE TABLE order_items, orders, payment_transactions, 
             inventory_reservations, cart_items, carts, product_variants, products, admin_users 
RESTART IDENTITY CASCADE;

-- ============================================
-- 1. PRODUCTS
-- ============================================

INSERT INTO products (name, slug, description, base_price, category, is_active, created_at, updated_at) VALUES
-- Áo thun (T-Shirts)
('Áo Thun Rồng Đen', 'ao-thun-rong-den', 'Áo thun cotton 100% in hình rồng phong cách streetwear', 350000.00, 'ao-thun', true, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days'),
('Áo Thun Hypebeast Logo', 'ao-thun-hypebeast-logo', 'Áo thun oversized với logo Hypebeast độc quyền', 299000.00, 'ao-thun', true, CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '25 days'),
('Áo Thun Vintage Wave', 'ao-thun-vintage-wave', 'Thiết kế sóng biển retro, chất liệu cotton cao cấp', 320000.00, 'ao-thun', true, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days'),
('Áo Thun Minimalist White', 'ao-thun-minimalist-white', 'Áo thun trắng basic, form rộng thoải mái', 250000.00, 'ao-thun', true, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),
('Áo Thun Street Art', 'ao-thun-street-art', 'In hình nghệ thuật đường phố độc đáo', 380000.00, 'ao-thun', true, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),

-- Hoodie
('Hoodie Black Dragon', 'hoodie-black-dragon', 'Hoodie cao cấp với họa tiết rồng thêu nổi', 650000.00, 'hoodie', true, CURRENT_TIMESTAMP - INTERVAL '28 days', CURRENT_TIMESTAMP - INTERVAL '28 days'),
('Hoodie Oversized Gray', 'hoodie-oversized-gray', 'Hoodie form rộng phong cách Hàn Quốc', 550000.00, 'hoodie', true, CURRENT_TIMESTAMP - INTERVAL '22 days', CURRENT_TIMESTAMP - INTERVAL '22 days'),
('Hoodie Zip-Up Navy', 'hoodie-zip-up-navy', 'Hoodie có khóa kéo, tiện lợi và năng động', 590000.00, 'hoodie', true, CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '18 days'),
('Hoodie Limited Edition', 'hoodie-limited-edition', 'Phiên bản giới hạn với chữ ký designer', 850000.00, 'hoodie', true, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Quần (Pants)
('Quần Jogger Black', 'quan-jogger-black', 'Quần jogger thun co giãn, thoải mái vận động', 450000.00, 'quan', true, CURRENT_TIMESTAMP - INTERVAL '27 days', CURRENT_TIMESTAMP - INTERVAL '27 days'),
('Quần Jean Rách Xanh', 'quan-jean-rach-xanh', 'Quần jean wash rách phong cách vintage', 520000.00, 'quan', true, CURRENT_TIMESTAMP - INTERVAL '23 days', CURRENT_TIMESTAMP - INTERVAL '23 days'),
('Quần Cargo Túi Hộp', 'quan-cargo-tui-hop', 'Quần cargo với nhiều túi tiện dụng', 480000.00, 'quan', true, CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '12 days'),
('Quần Short Kaki', 'quan-short-kaki', 'Quần short kaki cho mùa hè', 280000.00, 'quan', true, CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days');

-- ============================================
-- 2. PRODUCT VARIANTS (SKU với Size + Color)
-- ============================================

-- Áo Thun Rồng Đen (Product ID 1)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(1, 'ATRD-M-DEN', 'M', 'Đen', 0.00, 15, 0, true),
(1, 'ATRD-L-DEN', 'L', 'Đen', 0.00, 20, 0, true),
(1, 'ATRD-XL-DEN', 'XL', 'Đen', 20000.00, 12, 0, true),
(1, 'ATRD-M-TRANG', 'M', 'Trắng', 0.00, 10, 0, true),
(1, 'ATRD-L-TRANG', 'L', 'Trắng', 0.00, 8, 0, true),
(1, 'ATRD-XL-TRANG', 'XL', 'Trắng', 20000.00, 5, 0, true);

-- Áo Thun Hypebeast Logo (Product ID 2)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(2, 'ATHB-M-DEN', 'M', 'Đen', 0.00, 25, 0, true),
(2, 'ATHB-L-DEN', 'L', 'Đen', 0.00, 30, 0, true),
(2, 'ATHB-XL-DEN', 'XL', 'Đen', 0.00, 18, 0, true),
(2, 'ATHB-M-DO', 'M', 'Đỏ', 10000.00, 12, 0, true),
(2, 'ATHB-L-DO', 'L', 'Đỏ', 10000.00, 15, 0, true);

-- Áo Thun Vintage Wave (Product ID 3)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(3, 'ATVW-M-XANHDUONG', 'M', 'Xanh Dương', 0.00, 20, 0, true),
(3, 'ATVW-L-XANHDUONG', 'L', 'Xanh Dương', 0.00, 22, 0, true),
(3, 'ATVW-XL-XANHDUONG', 'XL', 'Xanh Dương', 15000.00, 10, 0, true),
(3, 'ATVW-M-CAM', 'M', 'Cam', 10000.00, 8, 0, true),
(3, 'ATVW-L-CAM', 'L', 'Cam', 10000.00, 6, 0, true);

-- Áo Thun Minimalist White (Product ID 4)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(4, 'ATMW-M-TRANG', 'M', 'Trắng', 0.00, 50, 0, true),
(4, 'ATMW-L-TRANG', 'L', 'Trắng', 0.00, 45, 0, true),
(4, 'ATMW-XL-TRANG', 'XL', 'Trắng', 0.00, 35, 0, true),
(4, 'ATMW-M-BE', 'M', 'Be', 0.00, 30, 0, true),
(4, 'ATMW-L-BE', 'L', 'Be', 0.00, 25, 0, true);

-- Áo Thun Street Art (Product ID 5)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(5, 'ATSA-M-DEN', 'M', 'Đen', 0.00, 8, 0, true),
(5, 'ATSA-L-DEN', 'L', 'Đen', 0.00, 12, 0, true),
(5, 'ATSA-XL-DEN', 'XL', 'Đen', 20000.00, 5, 0, true),
(5, 'ATSA-M-TRANG', 'M', 'Trắng', 0.00, 10, 0, true);

-- Hoodie Black Dragon (Product ID 6)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(6, 'HBD-M-DEN', 'M', 'Đen', 0.00, 10, 0, true),
(6, 'HBD-L-DEN', 'L', 'Đen', 0.00, 15, 0, true),
(6, 'HBD-XL-DEN', 'XL', 'Đen', 50000.00, 8, 0, true),
(6, 'HBD-M-XAMNHAT', 'M', 'Xám Nhạt', 30000.00, 6, 0, true),
(6, 'HBD-L-XAMNHAT', 'L', 'Xám Nhạt', 30000.00, 4, 0, true);

-- Hoodie Oversized Gray (Product ID 7)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(7, 'HOG-M-XAM', 'M', 'Xám', 0.00, 18, 0, true),
(7, 'HOG-L-XAM', 'L', 'Xám', 0.00, 20, 0, true),
(7, 'HOG-XL-XAM', 'XL', 'Xám', 30000.00, 12, 0, true),
(7, 'HOG-M-DEN', 'M', 'Đen', 0.00, 15, 0, true),
(7, 'HOG-L-DEN', 'L', 'Đen', 0.00, 10, 0, true);

-- Hoodie Zip-Up Navy (Product ID 8)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(8, 'HZN-M-XANHDAM', 'M', 'Xanh Đậm', 0.00, 12, 0, true),
(8, 'HZN-L-XANHDAM', 'L', 'Xanh Đậm', 0.00, 14, 0, true),
(8, 'HZN-XL-XANHDAM', 'XL', 'Xanh Đậm', 40000.00, 8, 0, true),
(8, 'HZN-M-DEN', 'M', 'Đen', 0.00, 10, 0, true);

-- Hoodie Limited Edition (Product ID 9) - Low stock for testing
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(9, 'HLE-M-DEN', 'M', 'Đen', 0.00, 3, 0, true),
(9, 'HLE-L-DEN', 'L', 'Đen', 0.00, 2, 0, true),
(9, 'HLE-XL-DEN', 'XL', 'Đen', 50000.00, 1, 0, true);

-- Quần Jogger Black (Product ID 10)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(10, 'QJB-M-DEN', 'M', 'Đen', 0.00, 20, 0, true),
(10, 'QJB-L-DEN', 'L', 'Đen', 0.00, 25, 0, true),
(10, 'QJB-XL-DEN', 'XL', 'Đen', 30000.00, 15, 0, true),
(10, 'QJB-M-XAM', 'M', 'Xám', 0.00, 18, 0, true);

-- Quần Jean Rách Xanh (Product ID 11)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(11, 'QJRX-29-XANH', '29', 'Xanh', 0.00, 12, 0, true),
(11, 'QJRX-30-XANH', '30', 'Xanh', 0.00, 15, 0, true),
(11, 'QJRX-31-XANH', '31', 'Xanh', 0.00, 10, 0, true),
(11, 'QJRX-32-XANH', '32', 'Xanh', 0.00, 8, 0, true);

-- Quần Cargo Túi Hộp (Product ID 12)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(12, 'QCTH-M-REU', 'M', 'Rêu', 0.00, 14, 0, true),
(12, 'QCTH-L-REU', 'L', 'Rêu', 0.00, 16, 0, true),
(12, 'QCTH-XL-REU', 'XL', 'Rêu', 20000.00, 10, 0, true),
(12, 'QCTH-M-DEN', 'M', 'Đen', 0.00, 18, 0, true),
(12, 'QCTH-L-DEN', 'L', 'Đen', 0.00, 12, 0, true);

-- Quần Short Kaki (Product ID 13)
INSERT INTO product_variants (product_id, sku, size, color, price_adjustment, stock_quantity, reserved_quantity, is_active) VALUES
(13, 'QSK-M-KAKI', 'M', 'Kaki', 0.00, 30, 0, true),
(13, 'QSK-L-KAKI', 'L', 'Kaki', 0.00, 35, 0, true),
(13, 'QSK-XL-KAKI', 'XL', 'Kaki', 0.00, 20, 0, true),
(13, 'QSK-M-DEN', 'M', 'Đen', 0.00, 25, 0, true);

-- ============================================
-- 3. ADMIN USERS
-- ============================================

-- Password: admin123 (BCrypt hash)
INSERT INTO admin_users (username, password_hash, full_name, created_at) VALUES
('admin', '$2a$12$mqSCydGOyidg8UuwOJL6ROZjsAfluEilRV8Di./ykpfLxqG91xq7m', 'Anh Hùng - Admin', CURRENT_TIMESTAMP),
('warehouse', '$2a$12$mqSCydGOyidg8UuwOJL6ROZjsAfluEilRV8Di./ykpfLxqG91xq7m', 'Nhân Viên Kho', CURRENT_TIMESTAMP);

-- ============================================
-- 4. TEST ORDERS FOR SEPAY WEBHOOK
-- ============================================

INSERT INTO orders (
    order_number, 
    tracking_token,
    customer_name, 
    customer_email, 
    customer_phone,
    shipping_address, 
    shipping_city, 
    shipping_district,
    payment_method, 
    payment_status,
    subtotal, 
    shipping_fee, 
    total_amount,
    status,
    customer_note,
    created_at,
    updated_at
) VALUES
(
    'ORD20260121001',
    gen_random_uuid(),
    'Nguyễn Văn Test',
    'test@example.com',
    '0912345678',
    '123 Đường Test, Phường 1',
    'TP Hồ Chí Minh',
    'Quận 1',
    'SEPAY',
    'pending',
    700000.00,
    30000.00,
    730000.00,
    'pending',
    'Giao giờ hành chính',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'ORD20260121002',
    gen_random_uuid(),
    'Trần Thị Demo',
    'demo@example.com',
    '0987654321',
    '456 Đường Demo, Phường 5',
    'Hà Nội',
    'Quận Ba Đình',
    'SEPAY',
    'pending',
    650000.00,
    30000.00,
    680000.00,
    'pending',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO order_items (
    order_id, 
    variant_id, 
    product_name, 
    variant_sku, 
    variant_size, 
    variant_color,
    quantity, 
    unit_price, 
    subtotal
) VALUES
(
    1, 
    1, 
    'Áo Thun Rồng Đen', 
    'ATRD-M-DEN', 
    'M', 
    'Đen',
    2, 
    350000.00, 
    700000.00
),
(
    2, 
    26, 
    'Hoodie Black Dragon', 
    'HBD-M-DEN', 
    'M', 
    'Đen',
    1, 
    650000.00, 
    650000.00
);

UPDATE product_variants SET reserved_quantity = 2 WHERE id = 1;
UPDATE product_variants SET reserved_quantity = 1 WHERE id = 26;

-- ============================================
-- SUMMARY
-- ============================================
-- Products: 13 products across 3 categories
-- Variants: 70+ SKUs with various sizes and colors
-- Stock levels: Mix of high, medium, and low stock for testing
-- Admin users: 2 users for testing (password: admin123)
-- Test orders: 2 orders with SEPAY payment method (pending status)
