package com.hospital.service;

import com.hospital.database.DatabaseConnection;
import com.hospital.model.Patient;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all CRUD (Create, Read, Update, Delete) operations for patients.
 * Works in conjunction with HashMapManager for caching and PriorityQueueManager for triage.
 */
public class PatientService {

    private final DatabaseConnection dbConnection;
    private final HashMapManager hashMapManager;
    private final PriorityQueueManager priorityQueueManager;
    private final BedAllocator bedAllocator;

    public PatientService(HashMapManager hashMapManager,
                          PriorityQueueManager priorityQueueManager,
                          BedAllocator bedAllocator) {
        this.dbConnection = DatabaseConnection.getInstance();
        this.hashMapManager = hashMapManager;
        this.priorityQueueManager = priorityQueueManager;
        this.bedAllocator = bedAllocator;
    }

    /**
     * Registers a new patient in the system with triage priority.
     * Adds to database, HashMap cache, and Priority Queue.
     *
     * @param name          Patient name
     * @param age           Age
     * @param gender        Gender
     * @param contact       Contact number
     * @param address       Address
     * @param severityLevel Severity level (1-5)
     * @return Generated patient ID, or -1 on failure
     */
    public int registerPatient(String name, int age, String gender, String contact,
                               String address, int severityLevel) {
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return -1;

            String query = "INSERT INTO patients (name, age, gender, contact, address, severity_level, severity_desc) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, name);
                pstmt.setInt(2, age);
                pstmt.setString(3, gender);
                pstmt.setString(4, contact);
                pstmt.setString(5, address);
                pstmt.setInt(6, severityLevel);
                pstmt.setString(7, getSeverityDesc(severityLevel));

                int inserted = pstmt.executeUpdate();
                if (inserted > 0) {
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        int patientId = rs.getInt(1);
                        Patient patient = new Patient(patientId, name, age, gender, contact,
                                                      address, severityLevel);

                        // Add to HashMap for O(1) lookup
                        hashMapManager.addPatient(patient);

                        // Add to Priority Queue for triage
                        priorityQueueManager.enqueuePatient(patient);

                        System.out.println("\n[REGISTRATION] Patient registered successfully!");
                        System.out.println("  ID: " + patientId);
                        System.out.println("  Name: " + name);
                        System.out.println("  Severity: " + patient.getSeverityDisplay());

                        // Auto-allocate bed for critical/serious patients
                        if (severityLevel >= Patient.SEVERITY_SERIOUS) {
                            System.out.println("[REGISTRATION] Auto-triggering bed allocation for high-severity patient...");
                            bedAllocator.allocateBed(patient);
                        }

                        return patientId;
                    }
                }
            }
            return -1;

        } catch (SQLException e) {
            System.err.println("[PATIENT SERVICE ERROR] " + e.getMessage());
            return -1;
        }
    }

    /**
     * Retrieves a patient by ID - uses HashMap for fast cache lookup first.
     *
     * @param patientId Patient ID
     * @return Patient object or null
     */
    public Patient getPatientById(int patientId) {
        // First check HashMap cache (O(1))
        Patient cached = hashMapManager.getPatient(patientId);
        if (cached != null) {
            return cached;
        }

        // Fall back to database
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return null;

            String query = "SELECT * FROM patients WHERE patient_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, patientId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    Patient patient = extractPatient(rs);
                    hashMapManager.addPatient(patient); // Cache it
                    return patient;
                }
            }
        } catch (SQLException e) {
            System.err.println("[PATIENT SERVICE ERROR] " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all patients from database.
     *
     * @return List of all patients
     */
    public List<Patient> getAllPatients() {
        List<Patient> patients = new ArrayList<>();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return patients;

            String query = "SELECT * FROM patients ORDER BY registration_time DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    patients.add(extractPatient(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[PATIENT SERVICE ERROR] " + e.getMessage());
        }
        return patients;
    }

    /**
     * Updates patient information.
     *
     * @param patient Updated patient object
     * @return true if updated successfully
     */
    public boolean updatePatient(Patient patient) {
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return false;

            String query = "UPDATE patients SET name = ?, age = ?, gender = ?, contact = ?, " +
                          "address = ?, severity_level = ?, severity_desc = ?, status = ? WHERE patient_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, patient.getName());
                pstmt.setInt(2, patient.getAge());
                pstmt.setString(3, patient.getGender());
                pstmt.setString(4, patient.getContact());
                pstmt.setString(5, patient.getAddress());
                pstmt.setInt(6, patient.getSeverityLevel());
                pstmt.setString(7, patient.getSeverityDesc());
                pstmt.setString(8, patient.getStatus());
                pstmt.setInt(9, patient.getId());

                int updated = pstmt.executeUpdate();
                if (updated > 0) {
                    hashMapManager.updatePatient(patient);
                    System.out.println("[PATIENT SERVICE] Patient " + patient.getId() + " updated.");
                    return true;
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[PATIENT SERVICE ERROR] " + e.getMessage());
            return false;
        }
    }

    /**
     * Discharges a patient - releases bed and updates status.
     *
     * @param patientId Patient ID to discharge
     * @return true if discharged successfully
     */
    public boolean dischargePatient(int patientId) {
        try {
            Patient patient = getPatientById(patientId);
            if (patient == null) {
                System.out.println("[PATIENT SERVICE] Patient ID " + patientId + " not found.");
                return false;
            }

            Connection conn = dbConnection.getConnection();
            if (conn == null) return false;

            // Release bed if assigned
            if (patient.getAssignedBedId() != null) {
                bedAllocator.releaseBed(patient.getAssignedBedId());
            }

            // Update patient status
            String query = "UPDATE patients SET status = 'Discharged', assigned_bed_id = NULL WHERE patient_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, patientId);
                pstmt.executeUpdate();
            }

            // Remove from queue
            priorityQueueManager.removePatient(patientId);
            hashMapManager.removePatient(patientId);

            System.out.println("[PATIENT SERVICE] Patient " + patient.getName() + " discharged successfully.");
            return true;

        } catch (SQLException e) {
            System.err.println("[PATIENT SERVICE ERROR] " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a patient record.
     *
     * @param patientId Patient ID to delete
     * @return true if deleted
     */
    public boolean deletePatient(int patientId) {
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return false;

            String query = "DELETE FROM patients WHERE patient_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, patientId);
                int deleted = pstmt.executeUpdate();
                if (deleted > 0) {
                    hashMapManager.removePatient(patientId);
                    priorityQueueManager.removePatient(patientId);
                    System.out.println("[PATIENT SERVICE] Patient ID " + patientId + " deleted.");
                    return true;
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[PATIENT SERVICE ERROR] " + e.getMessage());
            return false;
        }
    }

    /**
     * Searches patients by name.
     *
     * @param name Name or partial name
     * @return List of matching patients
     */
    public List<Patient> searchPatients(String name) {
        List<Patient> patients = new ArrayList<>();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return patients;

            String query = "SELECT * FROM patients WHERE name LIKE ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, "%" + name + "%");
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    patients.add(extractPatient(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[PATIENT SERVICE ERROR] " + e.getMessage());
        }
        return patients;
    }

    /**
     * Displays the triage queue (patients sorted by priority).
     */
    public void displayTriageQueue() {
        priorityQueueManager.displayQueue();
    }

    /**
     * Displays all patient records.
     */
    public void displayAllPatients() {
        List<Patient> patients = getAllPatients();
        if (patients.isEmpty()) {
            System.out.println("\n[PATIENT SERVICE] No patients registered yet.");
            return;
        }

        System.out.println("\n========== ALL PATIENTS ==========");
        System.out.println(String.format("%-5s %-20s %-4s %-8s %-15s %-10s %-12s %-10s",
                          "ID", "Name", "Age", "Gender", "Contact", "Severity", "Bed", "Status"));
        System.out.println("--------------------------------------------------------------");

        for (Patient p : patients) {
            System.out.println(String.format("%-5d %-20s %-4d %-8s %-15s %-10s %-12s %-10s",
                              p.getId(),
                              truncate(p.getName(), 18),
                              p.getAge(),
                              p.getGender(),
                              p.getContact(),
                              p.getSeverityDesc(),
                              p.getAssignedBedId() != null ? "Bed " + p.getAssignedBedId() : "Not Assigned",
                              p.getStatus()));
        }
        System.out.println("==============================================================\n");
    }

    /**
     * Extracts Patient object from database ResultSet.
     */
    private Patient extractPatient(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getInt("patient_id"));
        patient.setName(rs.getString("name"));
        patient.setAge(rs.getInt("age"));
        patient.setGender(rs.getString("gender"));
        patient.setContact(rs.getString("contact"));
        patient.setAddress(rs.getString("address"));
        patient.setSeverityLevel(rs.getInt("severity_level"));
        patient.setSeverityDesc(rs.getString("severity_desc"));

        int bedId = rs.getInt("assigned_bed_id");
        patient.setAssignedBedId(rs.wasNull() ? null : bedId);

        int docId = rs.getInt("doctor_id");
        patient.setDoctorId(rs.wasNull() ? null : docId);

        patient.setRegistrationTime(rs.getString("registration_time"));
        patient.setStatus(rs.getString("status"));

        return patient;
    }

    /**
     * Returns severity description for a level.
     */
    private String getSeverityDesc(int level) {
        return switch (level) {
            case 5 -> "Critical";
            case 4 -> "Serious";
            case 3 -> "Urgent";
            case 2 -> "Moderate";
            default -> "Minor";
        };
    }

    /**
     * Truncates string to max length.
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen - 2) + ".." : str;
    }
}
