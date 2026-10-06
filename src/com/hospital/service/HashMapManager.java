package com.hospital.service;

import com.hospital.model.Patient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides O(1) patient record lookups using HashMap.
 * Maps Patient ID -> Patient object for instant access.
 * Significantly faster than scanning database tables repeatedly.
 *
 * Demonstrates: Java Collections Framework (HashMap), O(1) lookup performance
 */
public class HashMapManager {

    private final HashMap<Integer, Patient> patientMap;
    private final ReentrantLock lock = new ReentrantLock();

    public HashMapManager() {
        this.patientMap = new HashMap<>();
    }

    /**
     * Adds a patient to the HashMap cache.
     * Thread-safe using ReentrantLock.
     *
     * @param patient Patient to add
     */
    public void addPatient(Patient patient) {
        lock.lock();
        try {
            patientMap.put(patient.getId(), patient);
            System.out.println("[HASHMAP] Patient cached: " + patient.getName() + " (ID: " + patient.getId() + ")");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves a patient by ID in O(1) time.
     *
     * @param patientId Patient ID to look up
     * @return Patient object, or null if not found
     */
    public Patient getPatient(int patientId) {
        lock.lock();
        try {
            Patient patient = patientMap.get(patientId);
            if (patient != null) {
                System.out.println("[HASHMAP] Patient found: " + patient.getName() + " (ID: " + patientId + ")");
            } else {
                System.out.println("[HASHMAP] Patient ID " + patientId + " not found in cache");
            }
            return patient;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates an existing patient record in the cache.
     *
     * @param patient Updated patient object
     */
    public void updatePatient(Patient patient) {
        lock.lock();
        try {
            if (patientMap.containsKey(patient.getId())) {
                patientMap.put(patient.getId(), patient);
                System.out.println("[HASHMAP] Patient updated: " + patient.getName() + " (ID: " + patient.getId() + ")");
            } else {
                System.out.println("[HASHMAP] Patient ID " + patient.getId() + " not found for update");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a patient from the cache.
     *
     * @param patientId Patient ID to remove
     * @return true if patient was found and removed
     */
    public boolean removePatient(int patientId) {
        lock.lock();
        try {
            Patient removed = patientMap.remove(patientId);
            if (removed != null) {
                System.out.println("[HASHMAP] Patient removed from cache: ID " + patientId);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if a patient exists in the cache.
     *
     * @param patientId Patient ID to check
     * @return true if patient exists in HashMap
     */
    public boolean containsPatient(int patientId) {
        lock.lock();
        try {
            return patientMap.containsKey(patientId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the total number of patients in cache.
     *
     * @return Cache size
     */
    public int size() {
        lock.lock();
        try {
            return patientMap.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Displays all patients currently in the HashMap cache.
     */
    public void displayAllPatients() {
        lock.lock();
        try {
            if (patientMap.isEmpty()) {
                System.out.println("[HASHMAP] No patients in cache.");
                return;
            }

            System.out.println("\n========== PATIENT DIRECTORY (HASHMAP) ==========");
            System.out.println("Total patients cached: " + patientMap.size());
            System.out.println("----------------------------------------------------");
            
            // Sort by ID for display
            patientMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Patient p = entry.getValue();
                    System.out.println("ID: " + p.getId() +
                                       " | " + p.getName() +
                                       " | Age: " + p.getAge() +
                                       " | Severity: " + p.getSeverityDisplay() +
                                       " | Status: " + p.getStatus());
                });
            
            System.out.println("====================================================\n");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Searches patients by name (case-insensitive).
     *
     * @param name Name or partial name to search
     */
    public void searchByName(String name) {
        lock.lock();
        try {
            boolean found = false;
            System.out.println("\n========== SEARCH RESULTS ==========");
            
            for (Patient p : patientMap.values()) {
                if (p.getName().toLowerCase().contains(name.toLowerCase())) {
                    System.out.println(p);
                    found = true;
                }
            }
            
            if (!found) {
                System.out.println("No patients found with name containing: " + name);
            }
            System.out.println("=====================================\n");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears all patients from the cache.
     */
    public void clearCache() {
        lock.lock();
        try {
            patientMap.clear();
            System.out.println("[HASHMAP] Patient cache cleared.");
        } finally {
            lock.unlock();
        }
    }
}
