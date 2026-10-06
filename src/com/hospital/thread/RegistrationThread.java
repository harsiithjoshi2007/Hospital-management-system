package com.hospital.thread;

import com.hospital.model.Patient;
import com.hospital.service.PatientService;
import java.util.Scanner;

/**
 * Registration Thread - handles new patient registration concurrently.
 * Demonstrates Multithreading by running independently from other hospital modules.
 */
public class RegistrationThread implements Runnable {

    private final PatientService patientService;
    private final Scanner scanner;
    private volatile boolean running;

    public RegistrationThread(PatientService patientService) {
        this.patientService = patientService;
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    @Override
    public void run() {
        System.out.println("[THREAD] Registration Module started (Thread: " +
                           Thread.currentThread().getName() + ")");

        while (running) {
            try {
                displayRegistrationMenu();
                int choice = readIntInput();

                switch (choice) {
                    case 1 -> registerNewPatient();
                    case 2 -> viewPatient();
                    case 3 -> searchPatients();
                    case 4 -> viewAllPatients();
                    case 5 -> viewTriageQueue();
                    case 6 -> dischargePatient();
                    case 7 -> {
                        running = false;
                        System.out.println("[THREAD] Registration Module stopping...");
                    }
                    default -> System.out.println("[ERROR] Invalid option. Please try again.");
                }

                if (running) {
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                }

            } catch (Exception e) {
                System.err.println("[REGISTRATION THREAD ERROR] " + e.getMessage());
            }
        }

        System.out.println("[THREAD] Registration Module stopped.");
    }

    /**
     * Displays the registration module menu.
     */
    private void displayRegistrationMenu() {
        System.out.println("\n========================================");
        System.out.println("   REGISTRATION MODULE (Thread)");
        System.out.println("========================================");
        System.out.println("  1. Register New Patient");
        System.out.println("  2. View Patient Details");
        System.out.println("  3. Search Patients by Name");
        System.out.println("  4. View All Patients");
        System.out.println("  5. View Triage Queue");
        System.out.println("  6. Discharge Patient");
        System.out.println("  7. Exit Registration Module");
        System.out.println("========================================");
        System.out.print("  Enter choice: ");
    }

    /**
     * Registers a new patient with severity-based triage.
     */
    private void registerNewPatient() {
        System.out.println("\n--- New Patient Registration ---");
        System.out.print("  Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("[ERROR] Name cannot be empty.");
            return;
        }

        System.out.print("  Age: ");
        int age = readIntInput();

        System.out.print("  Gender (Male/Female/Other): ");
        String gender = scanner.nextLine().trim();

        System.out.print("  Contact: ");
        String contact = scanner.nextLine().trim();

        System.out.print("  Address: ");
        String address = scanner.nextLine().trim();

        System.out.println("\n  Severity Level (Triage Priority):");
        System.out.println("    1 - Minor (e.g., routine checkup)");
        System.out.println("    2 - Moderate (e.g., mild fever)");
        System.out.println("    3 - Urgent (e.g., fracture)");
        System.out.println("    4 - Serious (e.g., heart condition)");
        System.out.println("    5 - Critical (e.g., emergency, life-threatening)");
        System.out.print("  Enter severity (1-5): ");
        int severity = readIntInput();
        if (severity < 1 || severity > 5) {
            System.out.println("[ERROR] Invalid severity. Setting to Minor (1).");
            severity = 1;
        }

        int patientId = patientService.registerPatient(name, age, gender, contact, address, severity);
        if (patientId > 0) {
            System.out.println("\n  [SUCCESS] Patient registered with ID: " + patientId);
            if (severity >= Patient.SEVERITY_SERIOUS) {
                System.out.println("  [INFO] High-severity patient - priority queue & bed allocation activated.");
            }
        } else {
            System.out.println("\n  [FAILED] Registration failed. Check database connection.");
        }
    }

    /**
     * Views a specific patient's details by ID.
     */
    private void viewPatient() {
        System.out.print("\nEnter Patient ID: ");
        int id = readIntInput();

        Patient patient = patientService.getPatientById(id);
        if (patient != null) {
            System.out.println("\n--- Patient Details ---");
            System.out.println("  ID:           " + patient.getId());
            System.out.println("  Name:         " + patient.getName());
            System.out.println("  Age:          " + patient.getAge());
            System.out.println("  Gender:       " + patient.getGender());
            System.out.println("  Contact:      " + patient.getContact());
            System.out.println("  Address:      " + patient.getAddress());
            System.out.println("  Severity:     " + patient.getSeverityDisplay());
            System.out.println("  Status:       " + patient.getStatus());
            System.out.println("  Bed:          " + (patient.getAssignedBedId() != null ?
                                                     "Bed #" + patient.getAssignedBedId() : "Not assigned"));
            System.out.println("  Registered:   " + patient.getRegistrationTime());
        } else {
            System.out.println("[ERROR] Patient not found.");
        }
    }

    /**
     * Searches patients by name (partial match).
     */
    private void searchPatients() {
        System.out.print("\nEnter name to search: ");
        String name = scanner.nextLine().trim();

        var patients = patientService.searchPatients(name);
        if (patients.isEmpty()) {
            System.out.println("[INFO] No patients found matching: " + name);
        } else {
            System.out.println("\n--- Search Results ---");
            for (Patient p : patients) {
                System.out.println("  " + p);
            }
        }
    }

    /**
     * Views all registered patients.
     */
    private void viewAllPatients() {
        patientService.displayAllPatients();
    }

    /**
     * Displays the triage queue (patients sorted by priority).
     */
    private void viewTriageQueue() {
        System.out.println("\n--- Current Triage Queue (Priority Order) ---");
        patientService.displayTriageQueue();
    }

    /**
     * Discharges a patient.
     */
    private void dischargePatient() {
        System.out.print("\nEnter Patient ID to discharge: ");
        int id = readIntInput();

        if (patientService.dischargePatient(id)) {
            System.out.println("[SUCCESS] Patient discharged.");
        } else {
            System.out.println("[ERROR] Could not discharge patient.");
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
