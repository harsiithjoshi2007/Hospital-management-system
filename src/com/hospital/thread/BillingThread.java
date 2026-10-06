package com.hospital.thread;

import com.hospital.model.Bill;
import com.hospital.service.BillingService;
import com.hospital.service.PatientService;
import java.util.Scanner;

/**
 * Billing Thread - handles billing operations concurrently.
 * Demonstrates Multithreading by running independently from other hospital modules.
 */
public class BillingThread implements Runnable {

    private final BillingService billingService;
    private final PatientService patientService;
    private final Scanner scanner;
    private volatile boolean running;

    public BillingThread(BillingService billingService, PatientService patientService) {
        this.billingService = billingService;
        this.patientService = patientService;
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    @Override
    public void run() {
        System.out.println("[THREAD] Billing Module started (Thread: " +
                           Thread.currentThread().getName() + ")");

        while (running) {
            try {
                displayBillingMenu();
                int choice = readIntInput();

                switch (choice) {
                    case 1 -> generateNewBill();
                    case 2 -> processPayment();
                    case 3 -> viewBill();
                    case 4 -> viewUnpaidBills();
                    case 5 -> viewPatientBills();
                    case 6 -> {
                        running = false;
                        System.out.println("[THREAD] Billing Module stopping...");
                    }
                    default -> System.out.println("[ERROR] Invalid option. Please try again.");
                }

                if (running) {
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                }

            } catch (Exception e) {
                System.err.println("[BILLING THREAD ERROR] " + e.getMessage());
            }
        }

        System.out.println("[THREAD] Billing Module stopped.");
    }

    /**
     * Displays the billing module menu.
     */
    private void displayBillingMenu() {
        System.out.println("\n========================================");
        System.out.println("   BILLING MODULE (Thread)");
        System.out.println("========================================");
        System.out.println("  1. Generate New Bill");
        System.out.println("  2. Process Payment");
        System.out.println("  3. View Bill Details");
        System.out.println("  4. View Unpaid Bills");
        System.out.println("  5. View Patient's Bills");
        System.out.println("  6. Exit Billing Module");
        System.out.println("========================================");
        System.out.print("  Enter choice: ");
    }

    /**
     * Generates a new bill for a patient.
     */
    private void generateNewBill() {
        System.out.println("\n--- Generate Bill ---");

        System.out.print("  Patient ID: ");
        int patientId = readIntInput();

        var patient = patientService.getPatientById(patientId);
        if (patient == null) {
            System.out.println("[ERROR] Patient not found.");
            return;
        }

        System.out.println("  Patient: " + patient.getName());

        System.out.print("  Consultation Fee (Rs.): ");
        double consultationFee = readDoubleInput();

        System.out.print("  Medicine Charges (Rs.): ");
        double medicineCharges = readDoubleInput();

        System.out.print("  Bed Charges (Rs.): ");
        double bedCharges = readDoubleInput();

        System.out.print("  Other Charges (Rs.): ");
        double otherCharges = readDoubleInput();

        int billId = billingService.generateBill(patientId, consultationFee, medicineCharges,
                                                  bedCharges, otherCharges);
        if (billId > 0) {
            billingService.displayBill(billId);
        }
    }

    /**
     * Processes payment for a bill.
     */
    private void processPayment() {
        System.out.println("\n--- Process Payment ---");

        // Show unpaid bills first
        billingService.displayUnpaidBills();

        System.out.print("  Bill ID: ");
        int billId = readIntInput();

        Bill bill = billingService.getBillById(billId);
        if (bill == null) {
            System.out.println("[ERROR] Bill not found.");
            return;
        }

        System.out.println("  Total Amount: Rs." + String.format("%.2f", bill.getTotalAmount()));
        System.out.println("  Current Status: " + bill.getPaymentStatus());
        System.out.print("  Amount to Pay (Rs.): ");
        double amount = readDoubleInput();

        if (billingService.processPayment(billId, amount)) {
            System.out.println("\n  [SUCCESS] Payment processed.");
            billingService.displayBill(billId);
        }
    }

    /**
     * Views a specific bill.
     */
    private void viewBill() {
        System.out.print("\n  Bill ID: ");
        int billId = readIntInput();
        billingService.displayBill(billId);
    }

    /**
     * Views all unpaid bills.
     */
    private void viewUnpaidBills() {
        billingService.displayUnpaidBills();
    }

    /**
     * Views all bills for a specific patient.
     */
    private void viewPatientBills() {
        System.out.print("\n  Patient ID: ");
        int patientId = readIntInput();

        var bills = billingService.getBillsByPatient(patientId);
        if (bills.isEmpty()) {
            System.out.println("[INFO] No bills found for patient ID " + patientId);
            return;
        }

        System.out.println("\n--- Bills for Patient ID " + patientId + " ---");
        for (Bill bill : bills) {
            System.out.println("  " + bill);
        }
    }

    /**
     * Reads integer input safely.
     */
    private int readIntInput() {
        try {
            String input = scanner.nextLine().trim();
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Reads double input safely.
     */
    private double readDoubleInput() {
        try {
            String input = scanner.nextLine().trim();
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
