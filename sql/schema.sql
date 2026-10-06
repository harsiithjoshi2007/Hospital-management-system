-- =============================================================
-- Hospital Management System - Database Schema
-- =============================================================

CREATE DATABASE IF NOT EXISTS hospital_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE hospital_db;

-- =============================================================
-- PATIENTS TABLE
-- Stores all patient information with severity triage level
-- =============================================================
CREATE TABLE IF NOT EXISTS patients (
    patient_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(10) NOT NULL,
    contact VARCHAR(15) NOT NULL,
    address TEXT,
    severity_level INT NOT NULL DEFAULT 1 COMMENT '1=Minor, 2=Moderate, 3=Urgent, 4=Serious, 5=Critical',
    severity_desc VARCHAR(50) NOT NULL DEFAULT 'Minor',
    assigned_bed_id INT DEFAULT NULL,
    doctor_id INT DEFAULT NULL,
    registration_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'Registered' COMMENT 'Registered, Admitted, Discharged, Transferred'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- DOCTORS TABLE
-- =============================================================
CREATE TABLE IF NOT EXISTS doctors (
    doctor_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    contact VARCHAR(15) NOT NULL,
    email VARCHAR(100),
    available BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- MEDICINES TABLE (Centralized Inventory)
-- =============================================================
CREATE TABLE IF NOT EXISTS medicines (
    medicine_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100),
    unit_price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    min_stock_level INT DEFAULT 10,
    expiry_date DATE,
    last_restocked TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- BEDS TABLE
-- =============================================================
CREATE TABLE IF NOT EXISTS beds (
    bed_id INT AUTO_INCREMENT PRIMARY KEY,
    ward VARCHAR(50) NOT NULL COMMENT 'General, ICU, Emergency, Maternity, Pediatric',
    bed_number VARCHAR(20) NOT NULL UNIQUE,
    is_occupied BOOLEAN DEFAULT FALSE,
    patient_id INT DEFAULT NULL,
    assigned_date TIMESTAMP NULL,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- PRESCRIPTIONS TABLE
-- =============================================================
CREATE TABLE IF NOT EXISTS prescriptions (
    prescription_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    medicine_id INT NOT NULL,
    quantity INT NOT NULL,
    dosage VARCHAR(100),
    prescribed_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'Pending' COMMENT 'Pending, Dispensed, Cancelled',
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- BILLS TABLE
-- =============================================================
CREATE TABLE IF NOT EXISTS bills (
    bill_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    consultation_fee DECIMAL(10,2) DEFAULT 0.00,
    medicine_charges DECIMAL(10,2) DEFAULT 0.00,
    bed_charges DECIMAL(10,2) DEFAULT 0.00,
    other_charges DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(20) DEFAULT 'Unpaid' COMMENT 'Unpaid, Paid, Partial',
    bill_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- AUDIT LOG TABLE (for tracking operations)
-- =============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    performed_by VARCHAR(50) DEFAULT 'System',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- INDEXES for performance
-- =============================================================
CREATE INDEX idx_patient_status ON patients(status);
CREATE INDEX idx_patient_severity ON patients(severity_level);
CREATE INDEX idx_bed_ward ON beds(ward);
CREATE INDEX idx_medicine_stock ON medicines(stock_quantity);
CREATE INDEX idx_bill_status ON bills(payment_status);

-- =============================================================
-- SAMPLE DATA
-- =============================================================

-- Insert sample doctors
INSERT INTO doctors (name, specialization, contact, email) VALUES
('Dr. Sharma', 'Cardiology', '9876543210', 'sharma@hospital.com'),
('Dr. Patel', 'Neurology', '9876543211', 'patel@hospital.com'),
('Dr. Gupta', 'Orthopedics', '9876543212', 'gupta@hospital.com'),
('Dr. Singh', 'Pediatrics', '9876543213', 'singh@hospital.com'),
('Dr. Verma', 'General Medicine', '9876543214', 'verma@hospital.com');

-- Insert sample beds
INSERT INTO beds (ward, bed_number) VALUES
('General', 'G-101'), ('General', 'G-102'), ('General', 'G-103'), ('General', 'G-104'),
('ICU', 'ICU-01'), ('ICU', 'ICU-02'), ('ICU', 'ICU-03'),
('Emergency', 'ER-01'), ('Emergency', 'ER-02'), ('Emergency', 'ER-03'),
('Maternity', 'M-01'), ('Maternity', 'M-02'),
('Pediatric', 'P-01'), ('Pediatric', 'P-02');

-- Insert sample medicines
INSERT INTO medicines (name, category, manufacturer, unit_price, stock_quantity, min_stock_level, expiry_date) VALUES
('Paracetamol 500mg', 'Analgesic', 'Cipla', 2.50, 500, 50, '2027-12-31'),
('Amoxicillin 250mg', 'Antibiotic', 'Sun Pharma', 5.00, 300, 30, '2027-06-30'),
('Omeprazole 20mg', 'Antacid', 'Dr.Reddy', 4.00, 200, 20, '2027-09-30'),
('Aspirin 75mg', 'Blood Thinner', 'Bayer', 3.50, 400, 40, '2027-08-31'),
('Cetirizine 10mg', 'Antihistamine', 'GSK', 1.50, 600, 50, '2027-11-30'),
('Metformin 500mg', 'Diabetes', 'Novartis', 6.00, 250, 25, '2027-10-31'),
('Amlodipine 5mg', 'BP Medication', 'Pfizer', 8.00, 180, 20, '2027-07-31'),
('Ibuprofen 400mg', 'Anti-inflammatory', 'Zydus', 3.00, 350, 35, '2027-05-31');
