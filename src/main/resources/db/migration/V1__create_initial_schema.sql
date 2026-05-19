-- Enable PostGIS spatial extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Stores (tenants)
CREATE TABLE stores (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255)  NOT NULL,
    email       VARCHAR(255)  NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,
    address_text VARCHAR(500) NOT NULL,
    location    GEOMETRY(Point, 4326),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Routes (optimized delivery sequences)
CREATE TABLE routes (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id               UUID         NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    total_distance_meters  INTEGER,
    total_time_seconds     INTEGER,
    status                 VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Deliveries (packages)
CREATE TABLE deliveries (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id       UUID         NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    route_id       UUID         REFERENCES routes(id) ON DELETE SET NULL,
    customer_name  VARCHAR(255) NOT NULL,
    address_text   VARCHAR(500) NOT NULL,
    location       GEOMETRY(Point, 4326),
    delivery_order INTEGER,
    status         VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_deliveries_store_id ON deliveries(store_id);
CREATE INDEX idx_deliveries_status   ON deliveries(status);
CREATE INDEX idx_deliveries_route_id ON deliveries(route_id);
CREATE INDEX idx_routes_store_id     ON routes(store_id);

-- Spatial indexes for geometry lookups
CREATE INDEX idx_stores_location     ON stores USING GIST(location);
CREATE INDEX idx_deliveries_location ON deliveries USING GIST(location);
