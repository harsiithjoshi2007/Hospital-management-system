package com.hospital.thread;

import com.hospital.database.DatabaseConnection;
import com.hospital.model.Medicine;
import com.hospital.service.InventoryManager;
import com.hospital.service.PatientService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Pharmacy Thread - handles medicine dispensing and inventory management concurrently.
 * Demonstrates Multithreading by running independently from other hospital modules.
 */
public class PharmacyThread implements Runnable {

    private final InventoryManager inventoryManager;
    private final PatientService patientService;
    private final Scanner scanner;
    private volatile boolean running;

    public PharmacyThread(InventoryManager inventoryManager, PatientService patientService) {
        this.inventoryManager = inventoryManager;
        this.patientService = patientService;
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    @Override
    public void run() {
        System.out.println("[THREAD] Pharmacy Module started (Thread: " +
                           Thread.currentThread().getName() + ")");

        while (running) {
            try {
                displayPharmacyMenu();
                int choice = readIntInput();

                switch (choice) {
                    case 1 -> dispenseMedicine();
                    case 2 -> restockMedicine();
                    case 3 -> addNewMedicine();
                    case 4 -> viewInventory();
                    case 5 -> viewLowStockAlerts();
                    case 6 -> searchMedicine();
                    case 7 -> {
                        running = false;
                        System.out.println("[THREAD] Pharmacy Module stopping...");
                    }
                    default -> System.out.println("[ERROR] Invalid option. Please try again.");
                }

                if (running) {
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                }

            } catch (Exception e) {
                System.err.println("[PHARMACY THREAD ERROR] " + e.getMessage());
            }
        }

        System.out.println("[THREAD] Pharmacy Module stopped.");
    }

    /**
     * Displays the pharmacy module menu.
     */
    private void displayPharmacyMenu() {
        System.out.println("\n========================================");
        System.out.println("   PHARMACY MODULE (Thread)");
        System.out.println("========================================");
        System.out.println("  1. Dispense Medicine");
        System.out.println("  2. Restock Medicine");
        System.out.println("  3. Add New Medicine");
        System.out.println("  4. View Full Inventory");
        System.out.println("  5. View Low Stock Alerts");
        System.out.println("  6. Search Medicine");
        System.out.println("  7. Exit Pharmacy Module");
        System.out.println("========================================");
        System.out.print("  Enter choice: ");
    }

    /**
     * Dispenses medicine to a patient.
     */
    private void dispenseMedicine() {
        System.out.println("\n--- Dispense Medicine ---");

        // Verify patient exists
        System.out.print("  Patient ID: ");
        int patientId = readIntInput();

        var patient = patientService.getPatientById(patientId);
        if (patient == null) {
            System.out.println("[ERROR] Patient not found.");
            return;
        }

        System.out.println("  Patient: " + patient.getName());

        // Show available medicines
        inventoryManager.displayInventory();

        System.out.print("  Medicine ID: ");
        int medicineId = readIntInput();

        Medicine med = inventoryManager.getMedicine(medicineId);
        if (med == null) {
            System.out.println("[ERROR] Medicine not found.");
            return;
        }

        System.out.println("  Selected: " + med.getName() + " (Available: " + med.getStockQuantity() + ")");
        System.out.print("  Quantity to dispense: ");
        int quantity = readIntInput();

        if (quantity <= 0) {
            System.out.println("[ERROR] Invalid quantity.");
            return;
        }

        if (inventoryManager.dispenseMedicine(medicineId, quantity)) {
            System.out.println("\n  [SUCCESS] Dispensed " + quantity + "x " + med.getName() +
                               " to patient " + patient.getName());

            // Create prescription record in database
            createPrescription(patientId, medicineId, quantity);
        }
    }

    /**
     * Creates a prescription record in the database.
     * Prompts for doctor selection to avoid hardcoded values.
     */
    private void createPrescription(int patientId, int medicineId, int quantity) {
        System.out.print("  Doctor ID (enter 0 if unknown): ");
        int doctorId = readIntInput();
        if (doctorId <= 0) {
            doctorId = 1; // Default to first doctor
            System.out.println("  Using default doctor ID: " + doctorId);
        }

        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            Connection conn = db.getConnection();
            if (conn == null) return;

            String query = "INSERT INTO prescriptions (patient_id, doctor_id, medicine_id, quantity, dosage, status) " +
                          "VALUES (?, ?, ?, ?, 'As prescribed', 'Dispensed')";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, patientId);
                pstmt.setInt(2, doctorId);
                pstmt.setInt(3, medicineId);
                pstmt.setInt(4, quantity);
                pstmt.executeUpdate();
                System.out.println("[PHARMACY] Prescription recorded.");
            }
        } catch (SQLException e) {
            System.err.println("[PHARMACY] Could not create prescription record: " + e.getMessage());
        }
    }

    /**
     * Restocks an existing medicine.
     */
    private void restockMedicine() {
        System.out.println("\n--- Restock Medicine ---");
        System.out.print("  Medicine ID: ");
        int medicineId = readIntInput();

        Medicine med = inventoryManager.getMedicine(medicineId);
        if (med == null) {
            System.out.println("[ERROR] Medicine not found.");
            return;
        }

        System.out.println("  Current stock of " + med.getName() + ": " + med.getStockQuantity());
        System.out.print("  Quantity to add: ");
        int quantity = readIntInput();

        if (quantity <= 0) {
            System.out.println("[ERROR] Invalid quantity.");
            return;
        }

        if (inventoryManager.restockMedicine(medicineId, quantity)) {
            System.out.println("\n  [SUCCESS] Restocked " + quantity + " units of " + med.getName());
        }
    }

    /**
     * Adds a new medicine to inventory.
     */
    private void addNewMedicine() {
        System.out.println("\n--- Add New Medicine ---");
        System.out.print("  Medicine Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("[ERROR] Name cannot be empty.");
            return;
        }

        System.out.print("  Category (Analgesic/Antibiotic/Antacid/etc): ");
        String category = scanner.nextLine().trim();

        System.out.print("  Manufacturer: ");
        String manufacturer = scanner.nextLine().trim();

        System.out.print("  Unit Price (Rs.): ");
        double price;
        try {
            price = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid price.");
            return;
        }

        System.out.print("  Initial Stock Quantity: ");
        int stock = readIntInput();

        System.out.print("  Minimum Stock Level: ");
        int minStock = readIntInput();

        System.out.print("  Expiry Date (YYYY-MM-DD): ");
        String expiry = scanner.nextLine().trim();

        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setCategory(category);
        medicine.setManufacturer(manufacturer);
        medicine.setUnitPrice(price);
        medicine.setStockQuantity(stock);
        medicine.setMinStockLevel(minStock > 0 ? minStock : 10);
        medicine.setExpiryDate(expiry);

        if (inventoryManager.addMedicine(medicine)) {
            System.out.println("\n  [SUCCESS] Medicine added to inventory.");
        }
    }

    /**
     * Views the full inventory.
     */
    private void viewInventory() {
        inventoryManager.displayInventory();
    }

    /**
     * Views low stock alerts.
     */
    private void viewLowStockAlerts() {
        inventoryManager.displayLowStockAlerts();
    }

    /**
     * Searches for a medicine by name.
     */
    private void searchMedicine() {
        System.out.print("\nEnter medicine name to search: ");
        String searchTerm = scanner.nextLine().trim().toLowerCase();

        List<Medicine> medicines = inventoryManager.getAllMedicines();
        boolean found = false;

        for (Medicine med : medicines) {
            if (med.getName().toLowerCase().contains(searchTerm)) {
                System.out.println("  " + med);
                found = true;
            }
        }

        if (!found) {
            System.out.println("[INFO] No medicines found matching: " + searchTerm);
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
}
