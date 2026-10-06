package com.hospital.model;

/**
 * Bed class representing a hospital bed with ward information.
 * Used by BedAllocator for automated bed allocation.
 */
public class Bed {

    private int bedId;
    private String ward;
    private String bedNumber;
    private boolean isOccupied;
    private Integer patientId;
    private String assignedDate;

    // Ward constants for bed allocation
    public static final String WARD_GENERAL   = "General";
    public static final String WARD_ICU       = "ICU";
    public static final String WARD_EMERGENCY = "Emergency";
    public static final String WARD_MATERNITY = "Maternity";
    public static final String WARD_PEDIATRIC = "Pediatric";

    public Bed() {
        this.isOccupied = false;
    }

    public Bed(int bedId, String ward, String bedNumber, boolean isOccupied,
               Integer patientId, String assignedDate) {
        this.bedId = bedId;
        this.ward = ward;
        this.bedNumber = bedNumber;
        this.isOccupied = isOccupied;
        this.patientId = patientId;
        this.assignedDate = assignedDate;
    }

    // ---- Getters and Setters ----

    public int getBedId() { return bedId; }
    public void setBedId(int bedId) { this.bedId = bedId; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getBedNumber() { return bedNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }

    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }

    public Integer getPatientId() { return patientId; }
    public void setPatientId(Integer patientId) { this.patientId = patientId; }

    public String getAssignedDate() { return assignedDate; }
    public void setAssignedDate(String assignedDate) { this.assignedDate = assignedDate; }

    @Override
    public String toString() {
        return "Bed #" + bedNumber +
               " | Ward: " + ward +
               " | Status: " + (isOccupied ? "Occupied" : "Available");
    }
}
