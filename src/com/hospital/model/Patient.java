package com.hospital.model;

/**
 * Patient class extending Person.
 * Demonstrates Inheritance - inherits from Person and adds specific attributes.
 * Patients are prioritized using severity levels for triage.
 */
public class Patient extends Person {

    // Severity level constants
    public static final int SEVERITY_MINOR    = 1;
    public static final int SEVERITY_MODERATE = 2;
    public static final int SEVERITY_URGENT   = 3;
    public static final int SEVERITY_SERIOUS  = 4;
    public static final int SEVERITY_CRITICAL = 5;

    private String address;
    private int severityLevel;
    private String severityDesc;
    private Integer assignedBedId;
    private Integer doctorId;
    private String registrationTime;
    private String status;

    /** Timestamp used by PriorityQueue for ordering within same severity */
    private long arrivalTimestamp;

    /**
     * Default constructor.
     */
    public Patient() {
        super();
        this.severityLevel = SEVERITY_MINOR;
        this.severityDesc = "Minor";
        this.status = "Registered";
        this.arrivalTimestamp = System.currentTimeMillis();
    }

    /**
     * Parameterized constructor for creating a new patient.
     *
     * @param id            Patient ID
     * @param name          Patient name
     * @param age           Patient age
     * @param gender        Patient gender
     * @param contact       Contact number
     * @param address       Residential address
     * @param severityLevel Severity level (1-5)
     */
    public Patient(int id, String name, int age, String gender, String contact,
                   String address, int severityLevel) {
        super(id, name, age, gender, contact);
        this.address = address;
        setSeverityLevel(severityLevel);
        this.status = "Registered";
        this.arrivalTimestamp = System.currentTimeMillis();
    }

    /**
     * Full constructor including all fields.
     */
    public Patient(int id, String name, int age, String gender, String contact,
                   String address, int severityLevel, String severityDesc,
                   Integer assignedBedId, Integer doctorId, String registrationTime, String status) {
        super(id, name, age, gender, contact);
        this.address = address;
        this.severityLevel = severityLevel;
        this.severityDesc = severityDesc;
        this.assignedBedId = assignedBedId;
        this.doctorId = doctorId;
        this.registrationTime = registrationTime;
        this.status = status;
        this.arrivalTimestamp = System.currentTimeMillis();
    }

    /**
     * Sets severity level and automatically updates the description.
     *
     * @param level Severity level (1-5)
     */
    public final void setSeverityLevel(int level) {
        this.severityLevel = level;
        switch (level) {
            case SEVERITY_CRITICAL -> this.severityDesc = "Critical";
            case SEVERITY_SERIOUS  -> this.severityDesc = "Serious";
            case SEVERITY_URGENT   -> this.severityDesc = "Urgent";
            case SEVERITY_MODERATE -> this.severityDesc = "Moderate";
            default                -> { this.severityLevel = SEVERITY_MINOR; this.severityDesc = "Minor"; }
        }
    }

    // ---- Getters and Setters ----

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getSeverityLevel() { return severityLevel; }

    public String getSeverityDesc() { return severityDesc; }
    public void setSeverityDesc(String severityDesc) { this.severityDesc = severityDesc; }

    public Integer getAssignedBedId() { return assignedBedId; }
    public void setAssignedBedId(Integer assignedBedId) { this.assignedBedId = assignedBedId; }

    public Integer getDoctorId() { return doctorId; }
    public void setDoctorId(Integer doctorId) { this.doctorId = doctorId; }

    public String getRegistrationTime() { return registrationTime; }
    public void setRegistrationTime(String registrationTime) { this.registrationTime = registrationTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getArrivalTimestamp() { return arrivalTimestamp; }
    public void setArrivalTimestamp(long arrivalTimestamp) { this.arrivalTimestamp = arrivalTimestamp; }

    /**
     * Implementation of abstract method from Person class.
     *
     * @return Role description
     */
    @Override
    public String getRole() {
        return "Patient";
    }

    /**
     * Returns severity level as a color-coded string (for console display).
     *
     * @return Severity label with indicator
     */
    public String getSeverityDisplay() {
        return switch (severityLevel) {
            case SEVERITY_CRITICAL -> "[CRITICAL] " + severityDesc;
            case SEVERITY_SERIOUS  -> "[SERIOUS]  " + severityDesc;
            case SEVERITY_URGENT   -> "[URGENT]   " + severityDesc;
            case SEVERITY_MODERATE -> "[MODERATE] " + severityDesc;
            default                -> "[MINOR]    " + severityDesc;
        };
    }

    /**
     * Overridden toString method.
     * Polymorphism - provides patient-specific representation.
     *
     * @return Formatted patient details
     */
    @Override
    public String toString() {
        return super.toString() +
               " | Severity: " + getSeverityDisplay() +
               " | Status: " + status +
               " | Bed: " + (assignedBedId != null ? assignedBedId : "Not assigned");
    }
}
