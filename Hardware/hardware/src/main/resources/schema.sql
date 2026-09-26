-- Database schema for XAMPP MySQL: hardware_db (W J Digital Hardware System)
CREATE DATABASE IF NOT EXISTS hardware_db;
USE hardware_db;

SET FOREIGN_KEY_CHECKS = 0;

-- Drop legacy table structures if present to force fresh schema recreation
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS drivers;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS fleet_vehicles;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS staff_users;
DROP TABLE IF EXISTS customers;

-- --------------------------------------------------------
-- Table: staff_users (Admin, Inventory Officers, Fleet Officers)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS staff_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    role VARCHAR(50) NOT NULL, -- 'ROLE_ADMIN', 'ROLE_INVENTORY_OFFICER', 'ROLE_FLEET_OFFICER'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: customers (Store Customers)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: categories
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    icon_class VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: suppliers
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(150) NOT NULL UNIQUE,
    contact_person VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: products
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(12, 2) NOT NULL, -- Selling Price in LKR
    discount_percent DECIMAL(5, 2) DEFAULT 0.00,
    stock_quantity DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    unit VARCHAR(20) NOT NULL, -- 'pcs', 'kg', 'kubs', 'm', 'l'
    unit_weight_kg DECIMAL(10, 2) NOT NULL DEFAULT 1.00, -- Unit weight in kg
    unit_volume_kubs DECIMAL(10, 4) NOT NULL DEFAULT 0.0100, -- Unit volume in kubs/m3
    low_stock_threshold DECIMAL(10, 2) NOT NULL DEFAULT 10.00, -- Custom safety alert threshold
    supplier_id BIGINT,
    image_url VARCHAR(500),
    category_id BIGINT NOT NULL,
    is_featured BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: fleet_vehicles
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS fleet_vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_type VARCHAR(50) NOT NULL, -- 'BIKE', 'TUKTUK', 'LIGHT_TRUCK', 'TIPPER'
    model_name VARCHAR(100) NOT NULL,
    license_plate VARCHAR(30) NOT NULL UNIQUE,
    max_weight_kg DECIMAL(10, 2) NOT NULL,
    max_volume_kubs DECIMAL(10, 2) NOT NULL,
    base_fee DECIMAL(10, 2) NOT NULL, -- Fixed base fee in LKR
    per_km_rate DECIMAL(10, 2) NOT NULL, -- Distance rate per KM in LKR
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE', -- 'AVAILABLE', 'DELIVERING'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: drivers
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS drivers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    assigned_vehicle_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_drivers_vehicle FOREIGN KEY (assigned_vehicle_id) REFERENCES fleet_vehicles (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: orders
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    fulfillment_type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY', -- 'PICKUP' or 'DELIVERY'
    hardware_subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    delivery_charge DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL, -- Hardware + Delivery
    total_weight_kg DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_volume_kubs DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
    distance_km DECIMAL(8, 2) DEFAULT 0.00,
    latitude DECIMAL(10, 7) DEFAULT 6.9271000,
    longitude DECIMAL(10, 7) DEFAULT 79.8612000,
    assigned_vehicle_id BIGINT,
    assigned_driver_id BIGINT,
    assigned_driver_name VARCHAR(100),
    pickup_pin VARCHAR(10),
    shipping_address TEXT NOT NULL,
    phone VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50) NOT NULL, -- 'COD' or 'CARD'
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PAID'
    order_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'DELIVERING', 'DELIVERED', 'CANCELLED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_vehicle FOREIGN KEY (assigned_vehicle_id) REFERENCES fleet_vehicles (id) ON DELETE SET NULL,
    CONSTRAINT fk_orders_driver FOREIGN KEY (assigned_driver_id) REFERENCES drivers (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: order_items
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    unit_weight_kg DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    unit_volume_kubs DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
    quantity DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table: cart_items
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Seed Data: Categories
-- --------------------------------------------------------
INSERT IGNORE INTO categories (id, name, description, icon_class) VALUES
(1, 'Hand Tools', 'Hammers, screwdrivers, wrenches, pliers & manual equipment', 'bi bi-tools'),
(2, 'Power Tools', 'Drills, saws, grinders & heavy-duty electric tools', 'bi bi-lightning-charge-fill'),
(3, 'Electronics', 'Digital measuring tools, testing gear & smart hardware', 'bi bi-cpu-fill'),
(4, 'Plumbing', 'PVC pipes, fittings, valves & water tank components', 'bi bi-droplet-fill'),
(5, 'Construction', 'River sand, aggregate gravel, cement & brick materials', 'bi bi-building-fill'),
(6, 'Electrical', 'Wires, switches, circuit breakers & conduit pipes', 'bi bi-plug-fill'),
(7, 'Safety Gear', 'Helmets, safety goggles, gloves & protective boots', 'bi bi-shield-lock-fill'),
(8, 'Paints', 'Wall emulsions, anti-rust primers & paint brushes', 'bi bi-paint-bucket');

-- --------------------------------------------------------
-- Seed Data: Default Suppliers
-- --------------------------------------------------------
INSERT IGNORE INTO suppliers (id, company_name, contact_person, email, phone, address) VALUES
(1, 'Lanka Cement Industries Ltd', 'Sunil Perera', 'sales@lankacement.lk', '+94 11 234 5678', 'Industrial Zone, Puttalam'),
(2, 'Mahaweli Aggregate & Sand Suppliers', 'Kanthi Silva', 'orders@mahawelisand.lk', '+94 81 445 6789', 'Yard 4, River Basin, Kandy'),
(3, 'Bosch Lanka Power Hardware', 'Rohan Fernando', 'supply@bosch.lk', '+94 11 555 9900', 'Tech Park, Colombo 03'),
(4, 'Lanka PVC & Plumbing Supplies', 'Nimal Jayasinghe', 'orders@lankapvc.lk', '+94 33 221 4455', 'Ekala Industrial Estate, Ja-Ela');

-- --------------------------------------------------------
-- Seed Data: Staff Users (staff_users table)
-- --------------------------------------------------------
INSERT IGNORE INTO staff_users (id, username, email, password, full_name, phone, address, role) VALUES
(1, 'admin', 'admin@hardware.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Super Admin', '+94 77 123 4567', 'Main Hardware Hub, Colombo, Sri Lanka', 'ROLE_ADMIN'),
(2, 'inventory', 'inventory@hardware.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Inventory Officer', '+94 77 888 1122', 'Stock Yard, Colombo', 'ROLE_INVENTORY_OFFICER'),
(3, 'fleet', 'fleet@hardware.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Fleet Officer', '+94 77 999 3344', 'Dispatch Office, Colombo', 'ROLE_FLEET_OFFICER');

-- --------------------------------------------------------
-- Seed Data: Customer Users (customers table)
-- --------------------------------------------------------
INSERT IGNORE INTO customers (id, username, email, password, full_name, phone, address, role) VALUES
(1, 'customer', 'customer@hardware.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'John Silva', '+94 71 987 6543', '123 Temple Road, Nugegoda', 'ROLE_CUSTOMER'),
(2, 'kamal', 'kamal@hardware.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Kamal Perera', '+94 76 555 1234', '45 Main Street, Galle', 'ROLE_CUSTOMER');

-- --------------------------------------------------------
-- Seed Data: Fleet Vehicle Rate & Capability Threshold Matrix
-- --------------------------------------------------------
INSERT IGNORE INTO fleet_vehicles (id, vehicle_type, model_name, license_plate, max_weight_kg, max_volume_kubs, base_fee, per_km_rate, status) VALUES
(1, 'BIKE', 'Honda Cargo 125 Express', 'WP BZ-1020', 15.00, 0.05, 250.00, 50.00, 'AVAILABLE'),
(2, 'TUKTUK', 'Bajaj Maxima Cargo Three-Wheeler', 'WP AB-5040', 150.00, 0.20, 500.00, 80.00, 'AVAILABLE'),
(3, 'LIGHT_TRUCK', 'Dimo Batta 207 DI Light Commercial', 'WP CAT-8821', 1200.00, 1.50, 1500.00, 150.00, 'AVAILABLE'),
(4, 'TIPPER', 'Tata 1618 Heavy Dump Tipper Truck', 'WP EAD-9005', 8000.00, 5.00, 4000.00, 300.00, 'AVAILABLE');

-- --------------------------------------------------------
-- Seed Data: Default Drivers Roster
-- --------------------------------------------------------
INSERT IGNORE INTO drivers (id, full_name, phone, license_number, status) VALUES
(1, 'Sunil Perera', '0771234567', 'B1029384', 'AVAILABLE'),
(2, 'Bandula Jayasinghe', '0719876543', 'B5839201', 'AVAILABLE'),
(3, 'Saman Silva', '0765554321', 'B9483726', 'AVAILABLE');

-- --------------------------------------------------------
-- Seed Data: Products (Weights in KG & Volumes in Kubs)
-- --------------------------------------------------------
INSERT IGNORE INTO products (id, name, description, price, discount_percent, stock_quantity, unit, unit_weight_kg, unit_volume_kubs, low_stock_threshold, supplier_id, image_url, category_id, is_featured) VALUES
(1, 'Portland Cement 50kg Bag', 'Premium grade structural hydraulic cement bag for reinforced concrete work.', 2300.00, 0.00, 150.00, 'pcs', 50.00, 0.0300, 50.00, 1, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ4EGi7G1s3kROAgV0eBEB5ySpX1IBj7ff9gJPdL4qPtYFF2nTUlHiVGYU&s=10', 5, TRUE),
(2, 'Screened River Sand', 'Fine washed river sand ideal for plastering and concrete bricklaying.', 18000.00, 0.00, 25.50, 'kubs', 1600.00, 1.0000, 10.00, 2, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQieIPby7i0ROn67HV-K-XOybXY3DR3YIXQOwgL8K7bfAtuO82MLhDHBDy1&s=10', 5, TRUE),
(3, 'Aggregate Metal Chips (3/4")', 'Crushed granite aggregate stone suitable for foundation slabs and beams.', 16200.00, 8.00, 18.00, 'kubs', 1550.00, 1.0000, 8.00, 2, 'https://4.imimg.com/data4/VK/YS/IMOB-41589443/22829601_698732786987244_9108158046707123231_o.jpg', 5, FALSE),
(4, 'Heavy Duty Claw Hammer 16oz', 'High-carbon steel forged claw hammer with ergonomic non-slip rubber grip.', 2450.00, 10.00, 45.00, 'pcs', 0.85, 0.0020, 10.00, 1, 'https://images.unsplash.com/photo-1586864387967-d02ef85d93e8?w=500', 1, TRUE),
(5, 'Bosch Professional Impact Drill 650W', 'Variable speed reversible impact driver drill with auxiliary handle and depth gauge.', 18500.00, 5.00, 15.00, 'pcs', 2.30, 0.0150, 5.00, 3, 'https://images.unsplash.com/photo-1504148455328-c376907d081c?w=500', 2, TRUE),
(6, 'DeWalt Angle Grinder 850W 4.5"', 'Compact high performance disc grinder for metal cutting and masonry polishing.', 14200.00, 12.00, 8.00, 'pcs', 1.90, 0.0100, 5.00, 3, 'https://images.unsplash.com/photo-1572981779307-38b8cabb2407?w=500', 2, FALSE),
(7, 'Digital Multimeter & Circuit Tester', 'Auto-ranging digital voltage, resistance, and continuity multimeter with backlit LCD.', 4850.00, 5.00, 22.00, 'pcs', 0.45, 0.0015, 8.00, 3, 'https://img.drz.lazcdn.com/static/bd/p/c5a4025c059c780c76d9dba4795156bd.png_960x960q80.png_.webp', 3, TRUE),
(8, 'PVC Water Pipe (1 inch, 4m)', 'Durable unplasticized PVC pressure pipe for clean water distribution networks.', 1150.00, 0.00, 85.00, 'm', 0.40, 0.0050, 20.00, 4, 'https://tiimg.tistatic.com/fp/1/008/434/round-shape-seamless-1-mm-thickness-4-inch-length-pvc-water-pipe-055.jpg', 4, FALSE),
(9, 'Industrial Copper Wire Roll 1.5mm (100m)', 'High conductivity flame-retardant insulated pure copper cable roll for domestic and industrial wiring.', 9200.00, 0.00, 35.00, 'pcs', 2.80, 0.0040, 10.00, 4, 'https://images.unsplash.com/photo-1544724569-5f546fd6f2b5?w=500', 6, TRUE),
(10, 'Industrial Safety Helmet & Visor', 'ANSI-certified high-density polyethylene construction hard hat with adjustable ratchet suspension.', 2150.00, 0.00, 60.00, 'pcs', 0.60, 0.0035, 15.00, 1, 'https://images.unsplash.com/photo-1578873375969-d7285a864761?w=500', 7, TRUE),
(11, 'Dulux WeatherShield Exterior Paint (White 10L)', 'All-weather UV protection acrylic exterior paint with anti-fungal formula.', 12800.00, 15.00, 30.00, 'l', 14.00, 0.0100, 10.00, 1, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRul6x2f10obel1vzsx-yWn0ywhIIwDo79qsGQ7qPcQ9g&s=10', 8, TRUE),
(12, 'S-Lon PVC Elbow Connector (1 inch)', 'Premium 90-degree PVC elbow pressure fitting for residential water distribution.', 180.00, 0.00, 200.00, 'pcs', 0.10, 0.0005, 30.00, 4, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR9Bi5gRRtVoU0GB8l465ZstvSG_j0Wjm2xgbl_xHIuaA&s=10', 4, TRUE),
(13, 'Torpedo Magnetic Spirit Level 12-Inch', 'Heavy-duty magnetic torpedo level with 3 high-visibility bubble vials.', 1650.00, 5.00, 40.00, 'pcs', 0.35, 0.0010, 8.00, 1, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTM4BdyBuzlCAyZi2ShyDhGLy1FBzwRLyWuJDNBJtlRKw&s=10', 1, TRUE),
(14, 'Steel Toe Safety Boots (Size 42)', 'Oil-resistant anti-slip leather safety footwear with reinforced steel toe cap.', 5400.00, 0.00, 25.00, 'pcs', 1.60, 0.0080, 5.00, 1, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSdQ9eUanKwLUxanWu_H3sx1oJC76I3REEwIm-VMSnR7w&s=10', 7, TRUE),
(15, 'Single Pole MCB Circuit Breaker 16A', 'Din-rail single pole miniature circuit breaker for electrical distribution panels.', 950.00, 0.00, 60.00, 'pcs', 0.15, 0.0008, 15.00, 4, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTjdeDSrdqGrrkdLihNXdiaVYtP_69K13nRzUeCdR7kwg&s=10', 6, FALSE),
(16, 'Nippon Red Oxide Metal Primer (4L)', 'Anti-corrosive protective metal primer paint for structural ironwork.', 4200.00, 10.00, 35.00, 'l', 5.20, 0.0050, 10.00, 1, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT-6FImVx8K-ujBrsbCUdH5evqvvxog7MT7LLhV0f1w1w&s=10', 8, TRUE),
(17, 'Heavy Duty Construction Wheelbarrow 90L', 'Seamless steel tray wheelbarrow with pneumatic tire for site material haulage.', 14500.00, 0.00, 12.00, 'pcs', 14.50, 0.1500, 3.00, 2, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTW2cZop6vKnIs4W1Ibr7ohLsnuN_gaWCJ0rjv5F5OgGg&s=10', 5, TRUE),
(18, 'Makita Circular Saw 1800W 7-1/4"', 'High power electric timber circular cutting saw with dust extraction port.', 26800.00, 8.00, 10.00, 'pcs', 4.80, 0.0250, 3.00, 3, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRC_7og4f0NAatQ2eVZBlNYfPVsnXrN6V6eYABeHxRIrg&s=10', 2, TRUE),
(19, 'Adjustable Plumbing Pipe Wrench 14-Inch', 'Drop-forged steel pipe wrench with self-cleaning threads and replaceable jaws.', 3250.00, 0.00, 30.00, 'pcs', 1.40, 0.0030, 5.00, 4, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ37FQBrGmC_Mh7GKIeVD81_Gw04UlEuvJixV2wdzrU9w&s', 1, FALSE),
(20, 'High-Yield Deformed Steel Rebar 12mm (6m)', 'Structural grade ribbed steel reinforcement bar for concrete columns and beams.', 2850.00, 0.00, 100.00, 'pcs', 5.33, 0.0040, 20.00, 1, 'https://www.bmsteel.co.uk/images/products/standard/318_16052.jpg', 5, TRUE),
(21, 'Outdoor Waterproof LED Flood Light 100W', 'High luminosity IP66 waterproof aluminum alloy exterior security flood light.', 4950.00, 5.00, 40.00, 'pcs', 1.20, 0.0040, 10.00, 3, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSZvFMAsKCziuCxEv1Z1iGQozRlcIOvvDC9rSeYYRBYwg&s=10', 6, TRUE);

SET FOREIGN_KEY_CHECKS = 1;
