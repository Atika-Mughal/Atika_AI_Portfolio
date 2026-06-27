-- Smart City Transport Management System
-- Run this once in MySQL Workbench (or `mysql -u root -p < schema.sql`)

CREATE DATABASE IF NOT EXISTS smart_city_transport
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_city_transport;

-- Users (admin / driver / passenger)
CREATE TABLE IF NOT EXISTS users (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  username   VARCHAR(50)  NOT NULL UNIQUE,
  password   VARCHAR(100) NOT NULL,
  user_type  ENUM('Admin','Driver','Passenger') NOT NULL,
  full_name  VARCHAR(100),
  email      VARCHAR(100),
  phone      VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Vehicles / buses
CREATE TABLE IF NOT EXISTS vehicles (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  vehicle_no    VARCHAR(20) NOT NULL UNIQUE,
  vehicle_type  VARCHAR(30),
  capacity      INT,
  driver_name   VARCHAR(100),
  status        ENUM('Active','Maintenance','Idle') DEFAULT 'Active'
);

-- Routes
CREATE TABLE IF NOT EXISTS routes (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  route_name   VARCHAR(80) NOT NULL,
  start_point  VARCHAR(80),
  end_point    VARCHAR(80),
  distance_km  DECIMAL(6,2),
  stops        INT
);

-- Bus schedule
CREATE TABLE IF NOT EXISTS schedules (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  bus_no      VARCHAR(20),
  route_name  VARCHAR(80),
  departure   TIME,
  arrival     TIME,
  status      VARCHAR(20) DEFAULT 'On Time'
);

-- Bus stops (with lat/lon for the live map)
CREATE TABLE IF NOT EXISTS stops (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  name      VARCHAR(80),
  latitude  DECIMAL(10,6),
  longitude DECIMAL(10,6)
);

-- Traffic lights
CREATE TABLE IF NOT EXISTS traffic_lights (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  intersection  VARCHAR(120),
  state         ENUM('Red','Yellow','Green') DEFAULT 'Red',
  cycle_seconds INT DEFAULT 45
);

-- ========== Demo seed data ==========
INSERT IGNORE INTO users (username, password, user_type, full_name, email) VALUES
 ('admin',     'admin',     'Admin',     'System Admin', 'admin@city.gov'),
 ('driver',    'driver',    'Driver',    'Ahmed Khan',   'driver@city.gov'),
 ('passenger', 'passenger', 'Passenger', 'Sara Ali',     'sara@example.com');

INSERT IGNORE INTO vehicles (vehicle_no, vehicle_type, capacity, driver_name, status) VALUES
 ('K-12','Bus',50,'Ahmed Khan','Active'),
 ('K-22','Bus',45,'Sara Ali','Active'),
 ('K-08','Bus',50,'Bilal Raza','Idle'),
 ('K-44','Mini Bus',30,'Usman Tariq','Active');

INSERT IGNORE INTO routes (route_name, start_point, end_point, distance_km, stops) VALUES
 ('Green Line','Saddar','Surjani',26.0,22),
 ('Orange Line','Orangi','Tower',19.0,18),
 ('Red Line','Malir','Numaish',28.0,25),
 ('Blue Line','Korangi','Clifton',14.0,16);

INSERT IGNORE INTO schedules (bus_no, route_name, departure, arrival, status) VALUES
 ('K-12','Green Line','08:00:00','09:15:00','On Time'),
 ('K-22','Orange Line','08:10:00','09:05:00','Delayed'),
 ('K-08','Red Line','08:30:00','09:50:00','On Time'),
 ('K-44','Blue Line','09:00:00','09:45:00','On Time');

INSERT IGNORE INTO stops (name, latitude, longitude) VALUES
 ('Saddar',   24.8520, 67.0010),
 ('Tower',    24.8460, 67.0000),
 ('Numaish',  24.8710, 67.0300),
 ('Clifton',  24.8250, 67.0100),
 ('Gulshan',  24.9160, 67.0900),
 ('Orangi',   24.9000, 66.9800);

INSERT IGNORE INTO traffic_lights (intersection, state, cycle_seconds) VALUES
 ('Shahrah-e-Faisal & Tipu Sultan','Green',45),
 ('II Chundrigar & MA Jinnah','Red',50),
 ('Stadium Road & University','Yellow',30),
 ('Korangi Crossing','Green',40);
