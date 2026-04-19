CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_label VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    room_name VARCHAR(255) NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    capacity INTEGER,
    price_per_night NUMERIC(14,2) NOT NULL,
    price_per_hour NUMERIC(14,2),
    status VARCHAR(50) NOT NULL,
    cover_image_url TEXT NOT NULL,
    gallery_urls JSONB NOT NULL DEFAULT '[]'::jsonb,
    amenities JSONB NOT NULL DEFAULT '[]'::jsonb,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    cccd VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    booking_count INTEGER NOT NULL DEFAULT 0,
    joined_label VARCHAR(30),
    last_visit_at TIMESTAMPTZ,
    color_tag VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_customers_phone UNIQUE (phone),
    CONSTRAINT uq_customers_cccd UNIQUE (cccd)
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    customer_id UUID,
    created_by_account_id UUID,
    user_identifier VARCHAR(255) NOT NULL,
    room_name_snapshot VARCHAR(255) NOT NULL,
    room_image_snapshot TEXT,
    customer_name_snapshot VARCHAR(255),
    customer_phone VARCHAR(20),
    customer_id_number VARCHAR(20),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    check_in_time TIME,
    check_out_time TIME,
    booking_type VARCHAR(50) NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50),
    payment_percent INTEGER,
    payment_amount NUMERIC(14,2),
    pay_amount_vnd NUMERIC(14,2),
    transfer_content VARCHAR(255),
    payment_expires_at TIMESTAMPTZ,
    cancel_reason TEXT,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_bookings_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_bookings_created_by FOREIGN KEY (created_by_account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_bookings_room_id ON bookings (room_id);
CREATE INDEX idx_bookings_customer_id ON bookings (customer_id);
CREATE INDEX idx_bookings_created_by_account_id ON bookings (created_by_account_id);
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_bookings_check_in_date ON bookings (check_in_date);
CREATE INDEX idx_bookings_deleted_at ON bookings (deleted_at);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    account_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_refresh_tokens_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_refresh_tokens_account_id ON refresh_tokens (account_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);

CREATE TABLE cleaner_tasks (
    id BIGSERIAL PRIMARY KEY,
    room_id UUID NOT NULL,
    cleaner_account_id UUID,
    state VARCHAR(50) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_cleaner_tasks_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_cleaner_tasks_account FOREIGN KEY (cleaner_account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_cleaner_tasks_room_id ON cleaner_tasks (room_id);
CREATE INDEX idx_cleaner_tasks_cleaner_account_id ON cleaner_tasks (cleaner_account_id);
CREATE INDEX idx_cleaner_tasks_deleted_at ON cleaner_tasks (deleted_at);

CREATE TABLE cleaner_issues (
    id BIGSERIAL PRIMARY KEY,
    cleaner_task_id BIGINT,
    room_id UUID NOT NULL,
    cleaner_account_id UUID,
    reason VARCHAR(255) NOT NULL,
    note TEXT,
    status VARCHAR(50) NOT NULL,
    reported_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_cleaner_issues_task FOREIGN KEY (cleaner_task_id) REFERENCES cleaner_tasks (id),
    CONSTRAINT fk_cleaner_issues_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_cleaner_issues_account FOREIGN KEY (cleaner_account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_cleaner_issues_task_id ON cleaner_issues (cleaner_task_id);
CREATE INDEX idx_cleaner_issues_room_id ON cleaner_issues (room_id);
CREATE INDEX idx_cleaner_issues_cleaner_account_id ON cleaner_issues (cleaner_account_id);
CREATE INDEX idx_cleaner_issues_deleted_at ON cleaner_issues (deleted_at);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_account_id UUID,
    actor_role VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    old_data JSONB,
    new_data JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_audit_logs_account FOREIGN KEY (actor_account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_audit_logs_actor_account_id ON audit_logs (actor_account_id);
CREATE INDEX idx_audit_logs_entity_name ON audit_logs (entity_name);
CREATE INDEX idx_audit_logs_entity_id ON audit_logs (entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
