package com.hospital.model;

/**
 * Bill class representing patient billing information.
 * Tracks consultation, medicine, bed, and other charges.
 */
public class Bill {

    private int billId;
    private int patientId;
    private double consultationFee;
    private double medicineCharges;
    private double bedCharges;
    private double otherCharges;
    private double totalAmount;
    private String paymentStatus;
    private String billDate;

    public static final String STATUS_UNPAID = "Unpaid";
    public static final String STATUS_PAID   = "Paid";
    public static final String STATUS_PARTIAL = "Partial";

    public Bill() {
        this.paymentStatus = STATUS_UNPAID;
    }

    public Bill(int billId, int patientId, double consultationFee, double medicineCharges,
                double bedCharges, double otherCharges, double totalAmount,
                String paymentStatus, String billDate) {
        this.billId = billId;
        this.patientId = patientId;
        this.consultationFee = consultationFee;
        this.medicineCharges = medicineCharges;
        this.bedCharges = bedCharges;
        this.otherCharges = otherCharges;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
        this.billDate = billDate;
    }

    // ---- Getters and Setters ----

    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }

    public double getMedicineCharges() { return medicineCharges; }
    public void setMedicineCharges(double medicineCharges) { this.medicineCharges = medicineCharges; }

    public double getBedCharges() { return bedCharges; }
    public void setBedCharges(double bedCharges) { this.bedCharges = bedCharges; }

    public double getOtherCharges() { return otherCharges; }
    public void setOtherCharges(double otherCharges) { this.otherCharges = otherCharges; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getBillDate() { return billDate; }
    public void setBillDate(String billDate) { this.billDate = billDate; }

    /**
     * Calculates total amount from individual charges.
     */
    public double calculateTotal() {
        this.totalAmount = consultationFee + medicineCharges + bedCharges + otherCharges;
        return totalAmount;
    }

    @Override
    public String toString() {
        return "Bill #" + billId +
               " | Patient ID: " + patientId +
               " | Total: Rs." + String.format("%.2f", totalAmount) +
               " | Status: " + paymentStatus +
               " | Date: " + billDate;
    }
}
