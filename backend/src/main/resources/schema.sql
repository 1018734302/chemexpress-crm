CREATE TABLE IF NOT EXISTS app_meta (
    meta_key VARCHAR(64) PRIMARY KEY,
    meta_value VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(40) NOT NULL,
    password VARCHAR(120) NOT NULL,
    name VARCHAR(40) NOT NULL,
    role VARCHAR(20) NOT NULL,
    region VARCHAR(20),
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(80) NOT NULL,
    type VARCHAR(20) NOT NULL,
    level VARCHAR(2) NOT NULL,
    region VARCHAR(20) NOT NULL,
    owner_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    address VARCHAR(160) NOT NULL,
    annual_potential DECIMAL(14,2),
    tags VARCHAR(200),
    source VARCHAR(20),
    remark VARCHAR(500),
    last_visit_date DATE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_customers_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS contacts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    name VARCHAR(40) NOT NULL,
    title VARCHAR(40),
    role VARCHAR(20) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(80),
    primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(300),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    trade_name VARCHAR(60) NOT NULL,
    generic_name VARCHAR(80) NOT NULL,
    spec VARCHAR(40) NOT NULL,
    dosage_form VARCHAR(20) NOT NULL,
    insurance_type VARCHAR(20) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    indication VARCHAR(500),
    CONSTRAINT uk_products_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS visits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    customer_id BIGINT NOT NULL,
    contact_id BIGINT,
    rep_id BIGINT NOT NULL,
    region VARCHAR(20) NOT NULL,
    visit_date DATE NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    checkin_time TIMESTAMP,
    checkin_distance INT,
    result VARCHAR(20),
    feedback VARCHAR(1000),
    next_step VARCHAR(500),
    reject_reason VARCHAR(300),
    approver_id BIGINT,
    approved_at TIMESTAMP,
    cancel_reason VARCHAR(300),
    remark VARCHAR(300),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_visits_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS visit_products (
    visit_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (visit_id, product_id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    customer_id BIGINT NOT NULL,
    rep_id BIGINT NOT NULL,
    region VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(14,2) NOT NULL,
    receiver_address VARCHAR(160),
    expected_date DATE,
    remark VARCHAR(500),
    reject_reason VARCHAR(300),
    approver_id BIGINT,
    approved_at TIMESTAMP,
    logistics_company VARCHAR(60),
    tracking_no VARCHAR(40),
    shipped_at TIMESTAMP,
    cancel_reason VARCHAR(300),
    completed_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(80) NOT NULL,
    product_spec VARCHAR(60) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    discount DECIMAL(5,2) NOT NULL DEFAULT 0,
    amount DECIMAL(14,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    pay_date DATE NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    reference_no VARCHAR(60),
    remark VARCHAR(300),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS business_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_type VARCHAR(20) NOT NULL,
    biz_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    detail VARCHAR(500),
    operator_id BIGINT,
    operator_name VARCHAR(40),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customers_owner ON customers(owner_id);
CREATE INDEX IF NOT EXISTS idx_customers_region ON customers(region);
CREATE INDEX IF NOT EXISTS idx_contacts_customer ON contacts(customer_id);
CREATE INDEX IF NOT EXISTS idx_visits_customer ON visits(customer_id);
CREATE INDEX IF NOT EXISTS idx_visits_rep ON visits(rep_id);
CREATE INDEX IF NOT EXISTS idx_visits_date ON visits(visit_date);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_rep ON orders(rep_id);
CREATE INDEX IF NOT EXISTS idx_orders_shipped ON orders(shipped_at);
CREATE INDEX IF NOT EXISTS idx_payments_order ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_events_biz ON business_events(biz_type, biz_id);
