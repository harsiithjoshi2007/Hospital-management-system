package com.hospital.service;

import com.hospital.database.DatabaseConnection;
import com.hospital.model.Bed;
import com.hospital.model.Patient;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Automated bed allocation system.
 * Allocates beds based on patient severity:
 * - CRITICAL: ICU ward
 * - SERIOUS: ICU or Emergency ward
 * - URGENT: Emergency or General ward
 * - MODERATE: General ward
 * - MINOR: General ward
 *
 * Demonstrates: Resource allocation algorithm, thread-safety
 */
public class BedAllocator {

    private final DatabaseConnection dbConnection;
    private final ReentrantLock lock = new ReentrantLock();

    public BedAllocator() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Allocates a bed to a patient based on their severity level.
     * Critical patients get ICU priority, then Emergency, etc.
     *
     * @param patient Patient who needs a bed
     * @return true if bed was allocated successfully
     */
    public boolean allocateBed(Patient patient) {
        lock.lock();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) {
                System.err.println("[BED ALLOCATOR] No database connection.");
                return false;
            }

            // Determine preferred wards based on severity
            String[] preferredWards = getPreferredWards(patient.getSeverityLevel());

            // Find available bed
            Bed availableBed = findAvailableBed(conn, preferredWards);
            if (availableBed == null) {
                System.out.println("[BED ALLOCATOR] No beds available for patient: " + patient.getName());
                return false;
            }

            // Assign bed to patient
            String assignQuery = "UPDATE beds SET is_occupied = TRUE, patient_id = ?, assigned_date = NOW() WHERE bed_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(assignQuery)) {
                pstmt.setInt(1, patient.getId());
                pstmt.setInt(2, availableBed.getBedId());
                int updated = pstmt.executeUpdate();

                if (updated > 0) {
                    // Update patient record with bed assignment
                    String updatePatient = "UPDATE patients SET assigned_bed_id = ?, status = 'Admitted' WHERE patient_id = ?";
                    try (PreparedStatement pstmt2 = conn.prepareStatement(updatePatient)) {
                        pstmt2.setInt(1, availableBed.getBedId());
                        pstmt2.setInt(2, patient.getId());
                        pstmt2.executeUpdate();
                    }

                    patient.setAssignedBedId(availableBed.getBedId());
                    patient.setStatus("Admitted");

                    System.out.println("[BED ALLOCATOR] Bed " + availableBed.getBedNumber() +
                                       " (" + availableBed.getWard() + ") allocated to " +
                                       patient.getName() + " (Severity: " + patient.getSeverityDisplay() + ")");
                    return true;
                }
            }

            return false;

        } catch (SQLException e) {
            System.err.println("[BED ALLOCATOR ERROR] " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases a bed when patient is discharged.
     *
     * @param bedId Bed ID to release
     * @return true if released successfully
     */
    public boolean releaseBed(int bedId) {
        lock.lock();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return false;

            String query = "UPDATE beds SET is_occupied = FALSE, patient_id = NULL, assigned_date = NULL WHERE bed_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, bedId);
                int updated = pstmt.executeUpdate();
                
                if (updated > 0) {
                    System.out.println("[BED ALLOCATOR] Bed ID " + bedId + " released.");
                    return true;
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[BED ALLOCATOR ERROR] " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns wards based on patient severity level.
     *
     * @param severityLevel Patient severity (1-5)
     * @return Array of ward names in preference order
     */
    private String[] getPreferredWards(int severityLevel) {
        return switch (severityLevel) {
            case Patient.SEVERITY_CRITICAL -> new String[]{
                Bed.WARD_ICU, Bed.WARD_EMERGENCY, Bed.WARD_GENERAL
            };
            case Patient.SEVERITY_SERIOUS -> new String[]{
                Bed.WARD_ICU, Bed.WARD_EMERGENCY, Bed.WARD_GENERAL
            };
            case Patient.SEVERITY_URGENT -> new String[]{
                Bed.WARD_EMERGENCY, Bed.WARD_ICU, Bed.WARD_GENERAL
            };
            case Patient.SEVERITY_MODERATE -> new String[]{
                Bed.WARD_GENERAL, Bed.WARD_EMERGENCY
            };
            default -> new String[]{Bed.WARD_GENERAL};
        };
    }

    /**
     * Finds an available bed from preferred wards.
     *
     * @param conn           Database connection
     * @param preferredWards Wards to search in order
     * @return Available Bed object, or null if none available
     */
    private Bed findAvailableBed(Connection conn, String[] preferredWards) throws SQLException {
        for (String ward : preferredWards) {
            String query = "SELECT * FROM beds WHERE ward = ? AND is_occupied = FALSE LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, ward);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return new Bed(
                        rs.getInt("bed_id"),
                        rs.getString("ward"),
                        rs.getString("bed_number"),
                        false, null, null
                    );
                }
            }
        }
        return null;
    }

    /**
     * Displays all beds with their current status.
     */
    public void displayBedStatus() {
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return;

            String query = "SELECT b.*, p.name as patient_name FROM beds b " +
                          "LEFT JOIN patients p ON b.patient_id = p.patient_id " +
                          "ORDER BY b.ward, b.bed_number";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                System.out.println("\n========== BED AVAILABILITY ==========");
                
                String currentWard = "";
                int totalBeds = 0;
                int occupiedBeds = 0;

                while (rs.next()) {
                    String ward = rs.getString("ward");
                    if (!ward.equals(currentWard)) {
                        currentWard = ward;
                        System.out.println("\n--- " + ward + " Ward ---");
                    }

                    String bedNum = rs.getString("bed_number");
                    boolean occupied = rs.getBoolean("is_occupied");
                    String patientName = rs.getString("patient_name");

                    System.out.println("  " + bedNum + " | " +
                                       (occupied ? "[OCCUPIED] " + (patientName != null ? patientName : "") : "[AVAILABLE]"));
                    totalBeds++;
                    if (occupied) occupiedBeds++;
                }

                System.out.println("\nSummary: " + occupiedBeds + "/" + totalBeds + " beds occupied");
                System.out.println("======================================\n");
            }

        } catch (SQLException e) {
            System.err.println("[BED ALLOCATOR ERROR] " + e.getMessage());
        }
    }

    /**
     * Gets all available beds.
     *
     * @return List of available beds
     */
    public List<Bed> getAvailableBeds() {
        List<Bed> beds = new ArrayList<>();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return beds;

            String query = "SELECT * FROM beds WHERE is_occupied = FALSE ORDER BY ward, bed_number";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    beds.add(new Bed(
                        rs.getInt("bed_id"),
                        rs.getString("ward"),
                        rs.getString("bed_number"),
                        false, null, null
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BED ALLOCATOR ERROR] " + e.getMessage());
        }
        return beds;
    }

    /**
     * Counts available beds.
     *
     * @return Number of available beds
     */
    public int getAvailableBedCount() {
        int count = 0;
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return 0;

            String query = "SELECT COUNT(*) FROM beds WHERE is_occupied = FALSE";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[BED ALLOCATOR ERROR] " + e.getMessage());
        }
        return count;
    }
}
