-- =============================================================
-- Hotel Room Reservation System - database schema
-- SE1020 OOP group project (Y1.S2.WD.IT.01.02)
-- Run this whole file once in MySQL Workbench or phpMyAdmin.
-- Running it again resets all tables (existing data is deleted).
-- =============================================================

CREATE DATABASE IF NOT EXISTS hotel_db;
USE hotel_db;

-- Drop in reverse order of foreign keys so the script can be re-run
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS invoice;
DROP TABLE IF EXISTS checkin_checkout;
DROP TABLE IF EXISTS booking;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS guest;
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS staff;

-- Admin Management (Paramie)
CREATE TABLE staff (
    staff_id    INT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('ADMIN', 'RECEPTIONIST') NOT NULL,
    contact_no  VARCHAR(20)
);

-- Room Management (De Silva)
CREATE TABLE room (
    room_id   INT AUTO_INCREMENT PRIMARY KEY,
    room_no   VARCHAR(10)   NOT NULL UNIQUE,
    type      ENUM('SINGLE', 'DOUBLE', 'SUITE') NOT NULL,
    rate      DECIMAL(10, 2) NOT NULL,
    capacity  INT            NOT NULL,
    status    ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE'
);

-- Guest Management (Dissanayake)
CREATE TABLE guest (
    guest_id      INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    nic_passport  VARCHAR(20)  NOT NULL UNIQUE,
    contact       VARCHAR(20)  NOT NULL,
    email         VARCHAR(100)
);

-- Reservation Management (Dayarathne)
CREATE TABLE reservation (
    reservation_id  INT AUTO_INCREMENT PRIMARY KEY,
    guest_id        INT            NOT NULL,
    room_id         INT            NOT NULL,
    date_from       DATE           NOT NULL,
    date_to         DATE           NOT NULL,
    advance_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    source          ENUM('PHONE', 'EMAIL') NOT NULL,
    status          ENUM('PENDING', 'CONVERTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_guest FOREIGN KEY (guest_id) REFERENCES guest (guest_id),
    CONSTRAINT fk_reservation_room  FOREIGN KEY (room_id)  REFERENCES room (room_id),
    CONSTRAINT chk_reservation_dates CHECK (date_to > date_from)
);

-- Booking Management (Prabath)
CREATE TABLE booking (
    booking_id      INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id  INT NULL,
    guest_id        INT  NOT NULL,
    room_id         INT  NOT NULL,
    date_from       DATE NOT NULL,
    date_to         DATE NOT NULL,
    booking_type    ENUM('RESERVATION', 'WALK_IN') NOT NULL,
    status          ENUM('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id),
    CONSTRAINT fk_booking_guest       FOREIGN KEY (guest_id)       REFERENCES guest (guest_id),
    CONSTRAINT fk_booking_room        FOREIGN KEY (room_id)        REFERENCES room (room_id),
    CONSTRAINT chk_booking_dates CHECK (date_to > date_from)
);

-- Check-in / Check-out Management (Dissanayake)
CREATE TABLE checkin_checkout (
    record_id      INT AUTO_INCREMENT PRIMARY KEY,
    booking_id     INT      NOT NULL UNIQUE,
    checkin_time   DATETIME NOT NULL,
    checkout_time  DATETIME NULL,
    checkout_type  ENUM('STANDARD', 'LATE', 'EARLY') NULL,
    extra_charge   DECIMAL(10, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_checkin_booking FOREIGN KEY (booking_id) REFERENCES booking (booking_id)
);

-- Finance Management (Wickramarathna)
CREATE TABLE invoice (
    invoice_id        INT AUTO_INCREMENT PRIMARY KEY,
    booking_id        INT            NOT NULL UNIQUE,
    room_total        DECIMAL(10, 2) NOT NULL,
    extra_charges     DECIMAL(10, 2) NOT NULL DEFAULT 0,
    advance_deducted  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    balance_due       DECIMAL(10, 2) NOT NULL,
    status            ENUM('UNPAID', 'PAID', 'VOID') NOT NULL DEFAULT 'UNPAID',
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_booking FOREIGN KEY (booking_id) REFERENCES booking (booking_id)
);

CREATE TABLE payment (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    invoice_id      INT NULL,
    reservation_id  INT NULL,
    amount          DECIMAL(10, 2) NOT NULL,
    method          ENUM('CARD', 'CASH') NOT NULL,
    paid_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_invoice     FOREIGN KEY (invoice_id)     REFERENCES invoice (invoice_id),
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id),
    CONSTRAINT chk_payment_target CHECK (invoice_id IS NOT NULL OR reservation_id IS NOT NULL)
);

-- First admin account so someone can log in (change the password after the first login)
INSERT INTO staff (full_name, username, password, role, contact_no)
VALUES ('System Admin', 'admin', 'admin123', 'ADMIN', '0110000000');

-- Each member adds 5+ sample rows for their own tables in docs/sample-data/<module>-data.sql
