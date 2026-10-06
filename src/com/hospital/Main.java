package com.hospital;

import com.hospital.database.DatabaseConnection;
import com.hospital.database.DatabaseSetup;
import com.hospital.model.*;
import com.hospital.service.*;
import com.hospital.thread.*;
import java.sql.*;
import java.util.List;
import java.util.Scanner;

/**
 * Intelligent Hospital Resource Management System
 * ==============================================
 *
 * A purely console-based application developed using Java and MySQL.
 * Key Features:
 * - Priority Queue for patient triage (critical patients first)
 * - HashMap for O(1) patient record lookup
 * - Multithreading for concurrent Registration, Pharmacy, and Billing modules
 * - Automated bed allocation based on severity
 * - Centralized medicine inventory management
 * - Built with OOP principles (Encapsulation, Inheritance, Polymorphism, Abstraction)
 *
 * @version 1.0
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    // Core services
    private static DatabaseConnection dbConnection;
    private static DatabaseSetup dbSetup;
    private static PriorityQueueManager priorityQueueManager;
    private static HashMapManager hashMapManager;
    private static BedAllocator bedAllocator;
    private static InventoryManager inventoryManager;
    private static PatientService patientService;
    private static BillingService billingService;

    public static void main(String[] args) {
        printBanner();
        initializeSystem();

        boolean exit = false;
        while (!exit) {
            try {
                displayMainMenu();
                int choice = readIntInput();

                switch (choice) {
                    case 1 -> launchRegistrationModule();
                    case 2 -> launchPharmacyModule();
                    case 3 -> launchBillingModule();
                    case 4 -> launchConcurrentDemo();
                    case 5 -> launchBedManagement();
                    case 6 -> launchInventoryManagement();
                    case 7 -> launchDoctorManagement();
                    case 8 -> displaySystemStatus();
                    case 9 -> showAbout();
                    case 10 -> {
                        exit = true;
                        shutdownSystem();
                    }
                    default -> System.out.println("[ERROR] Invalid option. Please enter 1-10.");
                }

                if (!exit && choice >= 1 && choice <= 9) {
                    System.out.println("\nPress Enter to return to Main Menu...");
                    scanner.nextLine();
                }

            } catch (Exception e) {
                System.err.println("[SYSTEM ERROR] " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Initializes the system - database connection, table creation, and service setup.
     */
    private static void initializeSystem() {
        System.out.println("\nInitializing system...");

        // Initialize database
        dbConnection = DatabaseConnection.getInstance();
        if (dbConnection.getConnection() == null) {
            System.err.println("[FATAL] Cannot connect to database. Please ensure MySQL is running.");
            System.err.println("[FATAL] Check connection settings in DatabaseConnection.java");
            System.exit(1);
        }

        // Create tables and seed data
        dbSetup = new DatabaseSetup();
        dbSetup.createTables();
        dbSetup.seedInitialData();

        // Initialize core services
        priorityQueueManager = new PriorityQueueManager();
        hashMapManager = new HashMapManager();
        bedAllocator = new BedAllocator();
        inventoryManager = new InventoryManager();
        patientService = new PatientService(hashMapManager, priorityQueueManager, bedAllocator);
        billingService = new BillingService();

        // Load existing patients into HashMap for fast lookup
        loadPatientsIntoCache();

        System.out.println("\n[SUCCESS] System initialized successfully!");
        System.out.println("[INFO] Ready to serve.\n");
    }

    /**
     * Loads existing patients from database into HashMap cache.
     */
    private static void loadPatientsIntoCache() {
        try {
            var patients = patientService.getAllPatients();
            for (var patient : patients) {
                hashMapManager.addPatient(patient);
            }
            System.out.println("[CACHE] Loaded " + patients.size() + " existing patient records.");
        } catch (Exception e) {
            System.out.println("[CACHE] No existing patients to load.");
        }
    }

    // ============================================================
    // BANNER & MENU
    // ============================================================

    private static void printBanner() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════╗");
        System.out.println("  ║        INTELLIGENT HOSPITAL RESOURCE            ║");
        System.out.println("  ║         MANAGEMENT SYSTEM v1.0                  ║");
        System.out.println("  ╠══════════════════════════════════════════════════╣");
        System.out.println("  ║  Java + MySQL | Priority Queue | HashMap       ║");
        System.out.println("  ║  Multithreading | Automated Bed Allocation     ║");
        System.out.println("  ╚══════════════════════════════════════════════════╝");
    }

    private static void displayMainMenu() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════╗");
        System.out.println("  ║            MAIN MENU                ║");
        System.out.println("  ╠══════════════════════════════════════╣");
        System.out.println("  ║  MODULES (run in their own threads):║");
        System.out.println("  ║  1. Registration Module             ║");
        System.out.println("  ║  2. Pharmacy Module                 ║");
        System.out.println("  ║  3. Billing Module                  ║");
        System.out.println("  ║                                     ║");
        System.out.println("  ║  DEMONSTRATION:                     ║");
        System.out.println("  ║  4. ★ Concurrent Processing Demo ★ ║");
        System.out.println("  ║                                     ║");
        System.out.println("  ║  MANAGEMENT:                        ║");
        System.out.println("  ║  5. Bed Management                  ║");
        System.out.println("  ║  6. Inventory Management            ║");
        System.out.println("  ║  7. Doctor Management               ║");
        System.out.println("  ║                                     ║");
        System.out.println("  ║  SYSTEM:                            ║");
        System.out.println("  ║  8. System Status                   ║");
        System.out.println("  ║  9. About                           ║");
        System.out.println("  ║  10. Exit                           ║");
        System.out.println("  ╚══════════════════════════════════════╝");
        System.out.print("  Enter your choice: ");
    }

    // ============================================================
    // MODULE LAUNCHERS
    // Each module runs in its own thread to demonstrate multithreading.
    // The thread.join() call ensures the module completes before
    // returning to the main menu (necessary for console input).
    // For true parallel execution, see the Concurrent Demo (option 4).
    // ============================================================

    private static void launchRegistrationModule() {
        System.out.println("\n--- Launching Registration Module in new thread ---");
        Thread thread = new Thread(new RegistrationThread(patientService), "Registration-Thread");
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.err.println("[MAIN] Registration thread interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    private static void launchPharmacyModule() {
        System.out.println("\n--- Launching Pharmacy Module in new thread ---");
        Thread thread = new Thread(new PharmacyThread(inventoryManager, patientService), "Pharmacy-Thread");
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.err.println("[MAIN] Pharmacy thread interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    private static void launchBillingModule() {
        System.out.println("\n--- Launching Billing Module in new thread ---");
        Thread thread = new Thread(new BillingThread(billingService, patientService), "Billing-Thread");
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.err.println("[MAIN] Billing thread interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    // ============================================================
    // ★ CONCURRENT PROCESSING DEMO ★
    // Demonstrates true multithreading by starting ALL THREE
    // modules simultaneously. They run in parallel, sharing
    // resources via thread-safe locks (ReentrantLock).
    // ============================================================

    private static void launchConcurrentDemo() {
        System.out.println("\n  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║     ★ CONCURRENT PROCESSING DEMO ★        ║");
        System.out.println("  ╠══════════════════════════════════════════════╣");
        System.out.println("  ║  Phase 1: Register patients (sequential)   ║");
        System.out.println("  ║  Phase 2: Pharmacy + Billing + Bed Alloc   ║");
        System.out.println("  ║  run SIMULTANEOUSLY in parallel threads.   ║");
        System.out.println("  ║  Shared resources use ReentrantLock.       ║");
        System.out.println("  ╚══════════════════════════════════════════════╝");

        System.out.print("\n  Press Enter to start demo...");
        scanner.nextLine();

        // ---- Phase 1: Register patients (sequential - needed for valid patient IDs) ----
        System.out.println("\n[DEMO] Phase 1: Registering patients...");
        int pid1 = registerDemoPatient("John Doe", 45, "Male", "Critical", 5, 200);
        int pid2 = registerDemoPatient("Jane Smith", 30, "Female", "Moderate", 2, 100);
        int pid3 = registerDemoPatient("Bob Wilson", 65, "Male", "Urgent", 3, 150);
        System.out.println("[DEMO] Phase 1 complete. Patient IDs: " + pid1 + ", " + pid2 + ", " + pid3);

        // ---- Phase 2: Run Pharmacy, Billing, and Bed Alloc CONCURRENTLY ----
        System.out.println("\n[DEMO] Phase 2: Starting 3 parallel threads simultaneously...\n");

        Thread pharmThread = new Thread(() -> {
            System.out.println("[DEMO-PHARM] '" + Thread.currentThread().getName() + "' started.");
            dispenseDemoMedicine(1, 10);   // Paracetamol
            dispenseDemoMedicine(3, 5);    // Omeprazole
            dispenseDemoMedicine(7, 15);   // Amlodipine
            System.out.println("[DEMO-PHARM] Thread completed.");
        }, "Demo-Pharmacy");

        Thread billThread = new Thread(() -> {
            System.out.println("[DEMO-BILL] '" + Thread.currentThread().getName() + "' started.");
            billDemoPatient(pid1, 300, 25, 0, 50);
            billDemoPatient(pid2, 800, 20, 1000, 200);
            System.out.println("[DEMO-BILL] Thread completed.");
        }, "Demo-Billing");

        Thread bedThread = new Thread(() -> {
            System.out.println("[DEMO-BED] '" + Thread.currentThread().getName() + "' started.");
            var p1 = patientService.getPatientById(pid1);
            if (p1 != null) bedAllocator.allocateBed(p1);
            var p3 = patientService.getPatientById(pid3);
            if (p3 != null) bedAllocator.allocateBed(p3);
            System.out.println("[DEMO-BED] Thread completed.");
        }, "Demo-BedAllocation");

        long startTime = System.currentTimeMillis();

        pharmThread.start();
        billThread.start();
        bedThread.start();

        try {
            pharmThread.join();
            billThread.join();
            bedThread.join();
        } catch (InterruptedException e) {
            System.err.println("[DEMO] Interrupted.");
            Thread.currentThread().interrupt();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("\n[D] All 3 concurrent threads completed in " + elapsed + " ms (parallel execution).");
        System.out.println("[D] Pharmacy, Billing, and Bed Allocation ran SIMULTANEOUSLY!");
        System.out.println("[D] They safely shared resources using ReentrantLock-based locks.");
    }

    /** Helper for demo - registers a patient, returns patient ID. */
    private static int registerDemoPatient(String name, int age, String gender,
                                            String severityStr, int severity, int delayMs) {
        int id = patientService.registerPatient(name, age, gender, "9999999999",
                                                "Demo Address", severity);
        sleepMs(delayMs);
        System.out.println("[DEMO-REG] Registered: " + name + " (ID:" + id +
                           ", Severity: " + severityStr + ")");
        return id;
    }

    /** Helper for demo - dispenses medicine. */
    private static void dispenseDemoMedicine(int medicineId, int qty) {
        boolean success = inventoryManager.dispenseMedicine(medicineId, qty);
        System.out.println("[DEMO-PHARM] Dispensed " + qty + "x Medicine ID:" +
                           medicineId + " -> " + (success ? "OK" : "FAILED"));
        sleepMs(150);
    }

    /** Helper for demo - generates a bill. */
    private static void billDemoPatient(int patientId, double consult, double med,
                                         double bed, double other) {
        int billId = billingService.generateBill(patientId, consult, med, bed, other);
        if (billId > 0) {
            System.out.println("[DEMO-BILL] Bill #" + billId + " for Patient ID:" +
                               patientId + " | Total: Rs." + (consult + med + bed + other));
        }
        sleepMs(200);
    }

    private static void sleepMs(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ============================================================
    // BED MANAGEMENT
    // ============================================================

    private static void launchBedManagement() {
        boolean back = false;
        while (!back) {
            System.out.println("\n  ╔══════════════════════════════════════╗");
            System.out.println("  ║        BED MANAGEMENT              ║");
            System.out.println("  ╠══════════════════════════════════════╣");
            System.out.println("  ║  1. View Bed Availability           ║");
            System.out.println("  ║  2. Manually Allocate Bed           ║");
            System.out.println("  ║  3. Release Bed                     ║");
            System.out.println("  ║  4. Back to Main Menu               ║");
            System.out.println("  ╚══════════════════════════════════════╝");
            System.out.print("  Enter choice: ");

            int choice = readIntInput();
            switch (choice) {
                case 1 -> bedAllocator.displayBedStatus();
                case 2 -> manuallyAllocateBed();
                case 3 -> manuallyReleaseBed();
                case 4 -> back = true;
                default -> System.out.println("[ERROR] Invalid option.");
            }

            if (!back) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    private static void manuallyAllocateBed() {
        System.out.print("\nEnter Patient ID: ");
        int patientId = readIntInput();
        var patient = patientService.getPatientById(patientId);
        if (patient == null) {
            System.out.println("[ERROR] Patient not found.");
            return;
        }
        System.out.println("  Patient: " + patient.getName() +
                           " | Severity: " + patient.getSeverityDisplay());
        if (bedAllocator.allocateBed(patient)) {
            System.out.println("[SUCCESS] Bed allocated to " + patient.getName());
        } else {
            System.out.println("[FAILED] No available beds matching severity.");
        }
    }

    private static void manuallyReleaseBed() {
        System.out.print("\nEnter Bed ID to release: ");
        int bedId = readIntInput();
        if (bedAllocator.releaseBed(bedId)) {
            System.out.println("[SUCCESS] Bed released.");
        } else {
            System.out.println("[FAILED] Could not release bed.");
        }
    }

    // ============================================================
    // INVENTORY MANAGEMENT
    // ============================================================

    private static void launchInventoryManagement() {
        boolean back = false;
        while (!back) {
            System.out.println("\n  ╔══════════════════════════════════════╗");
            System.out.println("  ║      INVENTORY MANAGEMENT          ║");
            System.out.println("  ╠══════════════════════════════════════╣");
            System.out.println("  ║  1. View Full Inventory             ║");
            System.out.println("  ║  2. View Low Stock Alerts           ║");
            System.out.println("  ║  3. Add New Medicine                 ║");
            System.out.println("  ║  4. Restock Medicine                 ║");
            System.out.println("  ║  5. Back to Main Menu                ║");
            System.out.println("  ╚══════════════════════════════════════╝");
            System.out.print("  Enter choice: ");

            int choice = readIntInput();
            switch (choice) {
                case 1 -> inventoryManager.displayInventory();
                case 2 -> inventoryManager.displayLowStockAlerts();
                case 3 -> addMedicinePrompt();
                case 4 -> restockMedicinePrompt();
                case 5 -> back = true;
                default -> System.out.println("[ERROR] Invalid option.");
            }

            if (!back) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    private static void addMedicinePrompt() {
        System.out.println("\n--- Add New Medicine ---");
        System.out.print("  Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("  Category: ");
        String category = scanner.nextLine().trim();
        System.out.print("  Manufacturer: ");
        String mfr = scanner.nextLine().trim();
        System.out.print("  Unit Price: ");
        double price = readDoubleInput();
        System.out.print("  Initial Stock: ");
        int stock = readIntInput();
        System.out.print("  Min Stock Level: ");
        int minStock = readIntInput();
        System.out.print("  Expiry (YYYY-MM-DD): ");
        String expiry = scanner.nextLine().trim();
        var medicine = new Medicine(0, name, category, mfr, price, stock, minStock, expiry, null);
        if (inventoryManager.addMedicine(medicine)) {
            System.out.println("[SUCCESS] Medicine added.");
        }
    }

    private static void restockMedicinePrompt() {
        System.out.print("\n  Medicine ID: ");
        int id = readIntInput();
        System.out.print("  Quantity to add: ");
        int qty = readIntInput();
        if (inventoryManager.restockMedicine(id, qty)) {
            System.out.println("[SUCCESS] Medicine restocked.");
        }
    }

    // ============================================================
    // DOCTOR MANAGEMENT
    // ============================================================

    private static void launchDoctorManagement() {
        boolean back = false;
        while (!back) {
            System.out.println("\n  ╔══════════════════════════════════════╗");
            System.out.println("  ║      DOCTOR MANAGEMENT              ║");
            System.out.println("  ╠══════════════════════════════════════╣");
            System.out.println("  ║  1. View All Doctors                 ║");
            System.out.println("  ║  2. Add New Doctor                   ║");
            System.out.println("  ║  3. Update Doctor                    ║");
            System.out.println("  ║  4. Toggle Availability              ║");
            System.out.println("  ║  5. Back to Main Menu                ║");
            System.out.println("  ╚══════════════════════════════════════╝");
            System.out.print("  Enter choice: ");

            int choice = readIntInput();
            switch (choice) {
                case 1 -> displayAllDoctors();
                case 2 -> addDoctor();
                case 3 -> updateDoctor();
                case 4 -> toggleDoctorAvailability();
                case 5 -> back = true;
                default -> System.out.println("[ERROR] Invalid option.");
            }

            if (!back) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    private static void displayAllDoctors() {
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return;

            String query = "SELECT * FROM doctors ORDER BY doctor_id";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                System.out.println("\n========== DOCTORS ==========");
                System.out.println(String.format("%-5s %-22s %-20s %-15s %-25s %-10s",
                                  "ID", "Name", "Specialization", "Contact", "Email", "Available"));
                System.out.println("--------------------------------------------------------------");

                boolean hasData = false;
                while (rs.next()) {
                    hasData = true;
                    System.out.println(String.format("%-5d %-22s %-20s %-15s %-25s %-10s",
                                      rs.getInt("doctor_id"),
                                      rs.getString("name"),
                                      rs.getString("specialization"),
                                      rs.getString("contact"),
                                      rs.getString("email") != null ? rs.getString("email") : "",
                                      rs.getBoolean("available") ? "Yes" : "No"));
                }
                if (!hasData) {
                    System.out.println("  No doctors found.");
                }
                System.out.println("================================\n");
            }
        } catch (SQLException e) {
            System.err.println("[DOCTOR ERROR] " + e.getMessage());
        }
    }

    private static void addDoctor() {
        System.out.println("\n--- Add New Doctor ---");
        System.out.print("  Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("  Specialization: ");
        String spec = scanner.nextLine().trim();
        System.out.print("  Contact: ");
        String contact = scanner.nextLine().trim();
        System.out.print("  Email: ");
        String email = scanner.nextLine().trim();

        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return;

            String query = "INSERT INTO doctors (name, specialization, contact, email) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, name);
                pstmt.setString(2, spec);
                pstmt.setString(3, contact);
                pstmt.setString(4, email);
                pstmt.executeUpdate();
                System.out.println("[SUCCESS] Doctor " + name + " added.");
            }
        } catch (SQLException e) {
            System.err.println("[DOCTOR ERROR] " + e.getMessage());
        }
    }

    private static void updateDoctor() {
        System.out.print("\nEnter Doctor ID to update: ");
        int id = readIntInput();

        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return;

            // Check if doctor exists
            String check = "SELECT * FROM doctors WHERE doctor_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    System.out.println("[ERROR] Doctor not found.");
                    return;
                }
                System.out.println("  Current: " + rs.getString("name") +
                                   " | " + rs.getString("specialization") +
                                   " | " + rs.getString("contact"));
            }

            System.out.print("  New Name (Enter to keep): ");
            String name = scanner.nextLine().trim();
            System.out.print("  New Specialization (Enter to keep): ");
            String spec = scanner.nextLine().trim();
            System.out.print("  New Contact (Enter to keep): ");
            String contact = scanner.nextLine().trim();
            System.out.print("  New Email (Enter to keep): ");
            String email = scanner.nextLine().trim();

            String query = "UPDATE doctors SET name = COALESCE(NULLIF(?, ''), name), " +
                          "specialization = COALESCE(NULLIF(?, ''), specialization), " +
                          "contact = COALESCE(NULLIF(?, ''), contact), " +
                          "email = COALESCE(NULLIF(?, ''), email) WHERE doctor_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, name);
                pstmt.setString(2, spec);
                pstmt.setString(3, contact);
                pstmt.setString(4, email);
                pstmt.setInt(5, id);
                pstmt.executeUpdate();
                System.out.println("[SUCCESS] Doctor updated.");
            }
        } catch (SQLException e) {
            System.err.println("[DOCTOR ERROR] " + e.getMessage());
        }
    }

    private static void toggleDoctorAvailability() {
        System.out.print("\nEnter Doctor ID: ");
        int id = readIntInput();

        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return;

            // Get current state
            String getQuery = "SELECT name, available FROM doctors WHERE doctor_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(getQuery)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    System.out.println("[ERROR] Doctor not found.");
                    return;
                }
                boolean current = rs.getBoolean("available");
                String name = rs.getString("name");

                String query = "UPDATE doctors SET available = ? WHERE doctor_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setBoolean(1, !current);
                    pstmt.setInt(2, id);
                    pstmt.executeUpdate();
                    System.out.println("[SUCCESS] " + name + " is now " +
                                       (!current ? "Available" : "Unavailable"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DOCTOR ERROR] " + e.getMessage());
        }
    }

    // ============================================================
    // SYSTEM STATUS
    // ============================================================

    private static void displaySystemStatus() {
        var conn = dbConnection.getConnection();
        var patients = patientService.getAllPatients();
        int availableBeds = bedAllocator.getAvailableBedCount();
        var lowStock = inventoryManager.getLowStockMedicines();

        System.out.println("\n  ╔══════════════════════════════════════════╗");
        System.out.println("  ║         SYSTEM STATUS                  ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.println("  ║  Database:        " +
                           (conn != null ? "Connected" : "Disconnected"));
        System.out.println("  ║  Total Patients:  " + patients.size());
        System.out.println("  ║  Available Beds:  " + availableBeds);
        System.out.println("  ║  HashMap Cache:   " + hashMapManager.size() + " patients");
        System.out.println("  ║  Triage Queue:    " + priorityQueueManager.size() + " waiting");
        System.out.println("  ║  Low Stock Items: " + lowStock.size());
        System.out.println("  ║  Threading:       ReentrantLock-based thread safety");
        System.out.println("  ╚══════════════════════════════════════════╝");
    }

    // ============================================================
    // ABOUT
    // ============================================================

    private static void showAbout() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════════════════════╗");
        System.out.println("  ║         INTELLIGENT HOSPITAL RESOURCE MANAGEMENT        ║");
        System.out.println("  ║                    SYSTEM v1.0                          ║");
        System.out.println("  ╠═══════════════════════════════════════════════════════════╣");
        System.out.println("  ║                                                          ║");
        System.out.println("  ║  A console-based Java application with MySQL backend    ║");
        System.out.println("  ║  that intelligently manages hospital resources using:   ║");
        System.out.println("  ║                                                          ║");
        System.out.println("  ║  🔷 Priority Queue - Patient triage by severity        ║");
        System.out.println("  ║  🔷 HashMap - O(1) patient record lookup               ║");
        System.out.println("  ║  🔷 Multithreading - Concurrent hospital modules       ║");
        System.out.println("  ║  🔷 Automated Bed Allocation - Severity-based           ║");
        System.out.println("  ║  🔷 Centralized Inventory - Medicine stock mgmt        ║");
        System.out.println("  ║                                                          ║");
        System.out.println("  ║  OOP Principles Used:                                    ║");
        System.out.println("  ║  • Encapsulation - Private fields with getters/setters  ║");
        System.out.println("  ║  • Inheritance - Patient/Doctor extends Person          ║");
        System.out.println("  ║  • Polymorphism - Overridden toString(), getRole()      ║");
        System.out.println("  ║  • Abstraction - Abstract Person class                  ║");
        System.out.println("  ║                                                          ║");
        System.out.println("  ║  Technologies: Java 17+, MySQL 8+, JDBC,                 ║");
        System.out.println("  ║               Java Collections Framework, Threads       ║");
        System.out.println("  ║                                                          ║");
        System.out.println("  ╚═══════════════════════════════════════════════════════════╝");
    }

    // ============================================================
    // SHUTDOWN
    // ============================================================

    private static void shutdownSystem() {
        System.out.println("\n  Shutting down Hospital Management System...");
        dbConnection.closeConnection();
        System.out.println("  Thank you for using the system. Goodbye!\n");
    }

    // ============================================================
    // INPUT HELPERS
    // ============================================================

    private static int readIntInput() {
        try {
            String input = scanner.nextLine().trim();
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static double readDoubleInput() {
        try {
            String input = scanner.nextLine().trim();
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
