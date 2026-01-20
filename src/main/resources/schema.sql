-- ============================================
-- 1. PRODUCTS & CATALOG
-- ============================================

-- Bảng sản phẩm chính (Product Master)
CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          slug VARCHAR(255) UNIQUE NOT NULL,
                          description TEXT,
                          base_price DECIMAL(10,2) NOT NULL, -- Giá gốc (có thể variants override)
                          category VARCHAR(100), -- 'ao-thun', 'hoodie', 'quan'
                          is_active BOOLEAN DEFAULT true,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category) WHERE is_active = true;
CREATE INDEX idx_products_slug ON products(slug);

-- Bảng variants (SKU: Size + Màu)
CREATE TABLE product_variants (
                                  id BIGSERIAL PRIMARY KEY,
                                  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                  sku VARCHAR(100) UNIQUE NOT NULL, -- 'AO-RONG-M-DEN'
                                  size VARCHAR(20), -- 'M', 'L', 'XL'
                                  color VARCHAR(50), -- 'Đen', 'Trắng'
                                  price_adjustment DECIMAL(10,2) DEFAULT 0, -- Điều chỉnh giá so với base_price
                                  stock_quantity INT NOT NULL DEFAULT 0, -- Tồn kho thực tế
                                  reserved_quantity INT NOT NULL DEFAULT 0, -- Đang giữ cho checkout
                                  is_active BOOLEAN DEFAULT true,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT chk_stock_non_negative CHECK (stock_quantity >= 0),
                                  CONSTRAINT chk_reserved_non_negative CHECK (reserved_quantity >= 0),
                                  CONSTRAINT chk_reserved_lte_stock CHECK (reserved_quantity <= stock_quantity)
);

CREATE INDEX idx_variants_product ON product_variants(product_id);
CREATE INDEX idx_variants_sku ON product_variants(sku);
CREATE INDEX idx_variants_stock ON product_variants(stock_quantity, reserved_quantity)
    WHERE is_active = true;

-- ============================================
-- 2. SHOPPING CART
-- ============================================

CREATE TABLE carts (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       session_id VARCHAR(255) UNIQUE NOT NULL, -- Cookie/Session ID từ frontend
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_carts_session ON carts(session_id);

CREATE TABLE cart_items (
                            id BIGSERIAL PRIMARY KEY,
                            cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
                            variant_id BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
                            quantity INT NOT NULL DEFAULT 1,
                            added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT chk_quantity_positive CHECK (quantity > 0),
                            UNIQUE(cart_id, variant_id) -- Mỗi variant chỉ xuất hiện 1 lần trong giỏ
);

CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);
CREATE INDEX idx_cart_items_variant ON cart_items(variant_id);

-- ============================================
-- 3. INVENTORY RESERVATION (Giữ hàng khi checkout)
-- ============================================

CREATE TABLE inventory_reservations (
                                        id BIGSERIAL PRIMARY KEY,
                                        variant_id BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
                                        quantity INT NOT NULL,
                                        reserved_for_order_id BIGINT, -- NULL khi đang giữ, có giá trị khi đã tạo đơn
                                        session_id VARCHAR(255) NOT NULL, -- Để biết reservation của ai
                                        reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        expires_at TIMESTAMP NOT NULL, -- Hết hạn sau 15 phút
                                        status VARCHAR(20) DEFAULT 'active', -- 'active', 'completed', 'expired', 'cancelled'

                                        CONSTRAINT chk_reservation_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_reservations_variant ON inventory_reservations(variant_id);
CREATE INDEX idx_reservations_session ON inventory_reservations(session_id);
CREATE INDEX idx_reservations_expires ON inventory_reservations(expires_at, status);
CREATE INDEX idx_reservations_order ON inventory_reservations(reserved_for_order_id);

-- ============================================
-- 4. ORDERS & CHECKOUT
-- ============================================

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        order_number VARCHAR(50) UNIQUE NOT NULL, -- 'HH-20250108-00001'
                        tracking_token UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(), -- Để tracking public

    -- Thông tin khách hàng
                        customer_name VARCHAR(255) NOT NULL,
                        customer_email VARCHAR(255) NOT NULL,
                        customer_phone VARCHAR(20) NOT NULL,

    -- Địa chỉ giao hàng
                        shipping_address TEXT NOT NULL,
                        shipping_city VARCHAR(100),
                        shipping_district VARCHAR(100),

    -- Thanh toán
                        payment_method VARCHAR(20) NOT NULL, -- 'COD' (Phase 1), other methods in Phase 2
                        payment_status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'paid', 'failed'

    -- Giá trị đơn hàng
                        subtotal DECIMAL(10,2) NOT NULL, -- Tổng giá sản phẩm
                        shipping_fee DECIMAL(10,2) DEFAULT 0,
                        total_amount DECIMAL(10,2) NOT NULL,

    -- Trạng thái đơn hàng
                        status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'confirmed', 'processing', 'shipping', 'completed', 'cancelled'

    -- Notes
                        customer_note TEXT,
                        admin_note TEXT,

    -- Timestamps
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        confirmed_at TIMESTAMP,
                        paid_at TIMESTAMP,
                        shipped_at TIMESTAMP,
                        completed_at TIMESTAMP,
                        cancelled_at TIMESTAMP,

                        CONSTRAINT chk_amounts_non_negative CHECK (
                            subtotal >= 0 AND shipping_fee >= 0 AND total_amount >= 0
                            )
);

CREATE INDEX idx_orders_number ON orders(order_number);
CREATE INDEX idx_orders_tracking ON orders(tracking_token);
CREATE INDEX idx_orders_email ON orders(customer_email);
CREATE INDEX idx_orders_status ON orders(status, created_at DESC);
CREATE INDEX idx_orders_created ON orders(created_at DESC);

-- Chi tiết đơn hàng
CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             variant_id BIGINT NOT NULL REFERENCES product_variants(id),

    -- Snapshot thông tin tại thời điểm đặt hàng (để tránh ảnh hưởng nếu product bị sửa/xóa)
                             product_name VARCHAR(255) NOT NULL,
                             variant_sku VARCHAR(100) NOT NULL,
                             variant_size VARCHAR(20),
                             variant_color VARCHAR(50),

                             unit_price DECIMAL(10,2) NOT NULL, -- Giá tại thời điểm mua
                             quantity INT NOT NULL,
                             subtotal DECIMAL(10,2) NOT NULL, -- unit_price * quantity

                             CONSTRAINT chk_order_item_quantity_positive CHECK (quantity > 0),
                             CONSTRAINT chk_order_item_amounts CHECK (subtotal = unit_price * quantity)
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_variant ON order_items(variant_id);

-- ============================================
-- 5. PAYMENT TRACKING (Reserved for Phase 2)
-- ============================================

CREATE TABLE payment_transactions (
                                      id BIGSERIAL PRIMARY KEY,
                                      order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                                      transaction_id VARCHAR(255), -- External payment gateway transaction ID
                                      amount DECIMAL(10,2) NOT NULL,
                                      payment_method VARCHAR(20) NOT NULL, -- Payment method identifier
                                      status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'success', 'failed'
                                      gateway_response TEXT, -- JSON response from payment gateway
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_order ON payment_transactions(order_id);
CREATE INDEX idx_payment_transaction ON payment_transactions(transaction_id);

-- ============================================
-- 6. ADMIN USERS (Simple)
-- ============================================

CREATE TABLE admin_users (
                             id BIGSERIAL PRIMARY KEY,
                             username VARCHAR(100) UNIQUE NOT NULL,
                             password_hash VARCHAR(255) NOT NULL, -- Bcrypt hash
                             full_name VARCHAR(255),
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             last_login_at TIMESTAMP
);

CREATE INDEX idx_admin_username ON admin_users(username);

-- ============================================
-- TRIGGERS & FUNCTIONS
-- ============================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_variants_updated_at BEFORE UPDATE ON product_variants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_carts_updated_at BEFORE UPDATE ON carts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();