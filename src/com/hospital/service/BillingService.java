package com.hospital.service;

import com.hospital.database.DatabaseConnection;
import com.hospital.model.Bill;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles billing operations including bill generation, payment processing,
 * and bill history tracking.
 */
public class BillingService {

    private final DatabaseConnection dbConnection;

    // Rate constants
    public static final double CONSULTATION_FEE_GENERAL = 300.00;
    public static final double CONSULTATION_FEE_SPECIALIST = 800.00;
    public static final double BED_CHARGE_GENERAL_PER_DAY = 1000.00;
    public static final double BED_CHARGE_ICU_PER_DAY = 5000.00;
    public static final double BED_CHARGE_EMERGENCY_PER_DAY = 3000.00;

    public BillingService() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Generates a bill for a patient.
     *
     * @param patientId     Patient ID
     * @param consultationFee Consultation fee
     * @param medicineCharges Medicine charges
     * @param bedCharges     Bed charges
     * @param otherCharges   Other charges
     * @return Bill ID, or -1 on failure
     */
    public int generateBill(int patientId, double consultationFee, double medicineCharges,
                            double bedCharges, double otherCharges) {
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return -1;

            double total = consultationFee + medicineCharges + bedCharges + otherCharges;

            String query = "INSERT INTO bills (patient_id, consultation_fee, medicine_charges, " +
                          "bed_charges, other_charges, total_amount) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, patientId);
                pstmt.setDouble(2, consultationFee);
                pstmt.setDouble(3, medicineCharges);
                pstmt.setDouble(4, bedCharges);
                pstmt.setDouble(5, otherCharges);
                pstmt.setDouble(6, total);

                int inserted = pstmt.executeUpdate();
                if (inserted > 0) {
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        int billId = rs.getInt(1);
                        System.out.println("\n[BILLING] Bill generated successfully!");
                        System.out.println("  Bill ID: " + billId);
                        System.out.println("  Patient ID: " + patientId);
                        System.out.println("  Total Amount: Rs." + String.format("%.2f", total));
                        return billId;
                    }
                }
            }
            return -1;

        } catch (SQLException e) {
            System.err.println("[BILLING ERROR] " + e.getMessage());
            return -1;
        }
    }

    /**
     * Processes payment for a bill.
     *
     * @param billId  Bill ID
     * @param amount  Amount paid
     * @return true if payment processed successfully
     */
    public boolean processPayment(int billId, double amount) {
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return false;

            // Get current bill
            String getQuery = "SELECT * FROM bills WHERE bill_id = ?";
            try (PreparedStatement getStmt = conn.prepareStatement(getQuery)) {
                getStmt.setInt(1, billId);
                ResultSet rs = getStmt.executeQuery();

                if (!rs.next()) {
                    System.out.println("[BILLING] Bill ID " + billId + " not found.");
                    return false;
                }

                double total = rs.getDouble("total_amount");
                String currentStatus = rs.getString("payment_status");

                String newStatus;
                if (amount >= total) {
                    newStatus = Bill.STATUS_PAID;
                } else if (amount > 0) {
                    newStatus = Bill.STATUS_PARTIAL;
                } else {
                    System.out.println("[BILLING] Invalid payment amount.");
                    return false;
                }

                // Update payment status
                String updateQuery = "UPDATE bills SET payment_status = ? WHERE bill_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                    updateStmt.setString(1, newStatus);
                    updateStmt.setInt(2, billId);
                    updateStmt.executeUpdate();
                }

                System.out.println("[BILLING] Payment of Rs." + String.format("%.2f", amount) +
                                   " received for Bill #" + billId +
                                   " | Status: " + newStatus);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[BILLING ERROR] " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets bill by ID.
     *
     * @param billId Bill ID
     * @return Bill object or null
     */
    public Bill getBillById(int billId) {
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return null;

            String query = "SELECT * FROM bills WHERE bill_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, billId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return extractBill(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[BILLING ERROR] " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets all bills for a patient.
     *
     * @param patientId Patient ID
     * @return List of bills
     */
    public List<Bill> getBillsByPatient(int patientId) {
        List<Bill> bills = new ArrayList<>();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return bills;

            String query = "SELECT * FROM bills WHERE patient_id = ? ORDER BY bill_date DESC";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, patientId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    bills.add(extractBill(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BILLING ERROR] " + e.getMessage());
        }
        return bills;
    }

    /**
     * Gets all unpaid bills.
     *
     * @return List of unpaid bills
     */
    public List<Bill> getUnpaidBills() {
        List<Bill> bills = new ArrayList<>();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return bills;

            String query = "SELECT * FROM bills WHERE payment_status IN ('Unpaid', 'Partial') ORDER BY bill_date";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    bills.add(extractBill(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BILLING ERROR] " + e.getMessage());
        }
        return bills;
    }

    /**
     * Displays a bill for printing.
     *
     * @param billId Bill ID to display
     */
    public void displayBill(int billId) {
        Bill bill = getBillById(billId);
        if (bill == null) {
            System.out.println("[BILLING] Bill not found.");
            return;
        }

        System.out.println("\n========== HOSPITAL BILL ==========");
        System.out.println("  Bill ID:      " + bill.getBillId());
        System.out.println("  Patient ID:   " + bill.getPatientId());
        System.out.println("  Date:         " + bill.getBillDate());
        System.out.println("  ---------------------------------");
        System.out.println("  Consultation: Rs." + String.format("%.2f", bill.getConsultationFee()));
        System.out.println("  Medicines:    Rs." + String.format("%.2f", bill.getMedicineCharges()));
        System.out.println("  Bed Charges:  Rs." + String.format("%.2f", bill.getBedCharges()));
        System.out.println("  Other:        Rs." + String.format("%.2f", bill.getOtherCharges()));
        System.out.println("  ---------------------------------");
        System.out.println("  TOTAL:        Rs." + String.format("%.2f", bill.getTotalAmount()));
        System.out.println("  Status:       " + bill.getPaymentStatus());
        System.out.println("====================================\n");
    }

    /**
     * Displays all unpaid bills.
     */
    public void displayUnpaidBills() {
        List<Bill> unpaidBills = getUnpaidBills();
        if (unpaidBills.isEmpty()) {
            System.out.println("[BILLING] No unpaid bills.");
            return;
        }

        System.out.println("\n========== UNPAID BILLS ==========");
        System.out.println(String.format("%-8s %-12s %-12s %-12s %-10s",
                          "Bill ID", "Patient ID", "Total", "Paid", "Status"));
        System.out.println("------------------------------------------");

        for (Bill bill : unpaidBills) {
            System.out.println(String.format("%-8d %-12d Rs.%-8.2f %-12s %-10s",
                              bill.getBillId(),
                              bill.getPatientId(),
                              bill.getTotalAmount(),
                              "-",
                              bill.getPaymentStatus()));
        }
        System.out.println("==========================================\n");
    }

    /**
     * Calculates estimated bed charges based on ward and days.
     *
     * @param ward Ward type
     * @param days Number of days
     * @return Estimated bed charges
     */
    public double calculateBedCharges(String ward, int days) {
        return switch (ward) {
            case "ICU" -> BED_CHARGE_ICU_PER_DAY * days;
            case "Emergency" -> BED_CHARGE_EMERGENCY_PER_DAY * days;
            default -> BED_CHARGE_GENERAL_PER_DAY * days;
        };
    }

    /**
     * Extracts Bill from ResultSet.
     */
    private Bill extractBill(ResultSet rs) throws SQLException {
        return new Bill(
            rs.getInt("bill_id"),
            rs.getInt("patient_id"),
            rs.getDouble("consultation_fee"),
            rs.getDouble("medicine_charges"),
            rs.getDouble("bed_charges"),
            rs.getDouble("other_charges"),
            rs.getDouble("total_amount"),
            rs.getString("payment_status"),
            rs.getString("bill_date")
        );
    }
}
