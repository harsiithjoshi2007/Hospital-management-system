package com.hospital.service;

import com.hospital.model.Patient;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages patient triage using a Priority Queue.
 * Patients are prioritized based on severity level (highest first).
 * For patients with same severity, earlier arrival is served first.
 * This ensures emergency cases are handled before minor cases.
 *
 * Demonstrates: Java Collections Framework (PriorityQueue), Comparator, ReentrantLock
 */
public class PriorityQueueManager {

    private final PriorityQueue<Patient> patientQueue;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Comparator that orders patients by:
     * 1. Severity level (descending - highest priority first)
     * 2. Arrival timestamp (ascending - earlier arrival first)
     */
    private static final Comparator<Patient> SEVERITY_COMPARATOR = (p1, p2) -> {
        // Higher severity = higher priority (descending order)
        int severityCompare = Integer.compare(p2.getSeverityLevel(), p1.getSeverityLevel());
        if (severityCompare != 0) {
            return severityCompare;
        }
        // Same severity: earlier arrival first
        return Long.compare(p1.getArrivalTimestamp(), p2.getArrivalTimestamp());
    };

    public PriorityQueueManager() {
        this.patientQueue = new PriorityQueue<>(SEVERITY_COMPARATOR);
    }

    /**
     * Adds a patient to the priority queue.
     * Thread-safe using ReentrantLock.
     *
     * @param patient Patient to enqueue
     */
    public void enqueuePatient(Patient patient) {
        lock.lock();
        try {
            patientQueue.offer(patient);
            System.out.println("[PRIORITY QUEUE] Patient '" + patient.getName() +
                               "' added with severity: " + patient.getSeverityDisplay());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes and returns the highest priority patient.
     * Thread-safe using ReentrantLock.
     *
     * @return Highest priority patient, or null if queue is empty
     */
    public Patient dequeuePatient() {
        lock.lock();
        try {
            Patient patient = patientQueue.poll();
            if (patient != null) {
                System.out.println("[PRIORITY QUEUE] Dequeued patient: " + patient.getName() +
                                   " (Severity: " + patient.getSeverityDisplay() + ")");
            }
            return patient;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Peeks at the highest priority patient without removing.
     *
     * @return Highest priority patient, or null if queue is empty
     */
    public Patient peekNextPatient() {
        lock.lock();
        try {
            return patientQueue.peek();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if the queue is empty.
     *
     * @return true if queue is empty
     */
    public boolean isEmpty() {
        lock.lock();
        try {
            return patientQueue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current size of the queue.
     *
     * @return Number of patients in queue
     */
    public int size() {
        lock.lock();
        try {
            return patientQueue.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Displays all patients currently in the priority queue.
     */
    public void displayQueue() {
        lock.lock();
        try {
            if (patientQueue.isEmpty()) {
                System.out.println("[PRIORITY QUEUE] No patients waiting.");
                return;
            }

            System.out.println("\n========== PATIENT TRIAGE QUEUE ==========");
            System.out.println("Patients ordered by priority (Critical first):");
            System.out.println("------------------------------------------------");
            
            // Create a copy to display without modifying original
            PriorityQueue<Patient> tempQueue = new PriorityQueue<>(patientQueue);
            int position = 1;
            
            while (!tempQueue.isEmpty()) {
                Patient p = tempQueue.poll();
                System.out.println(position + ". " + p.getName() +
                                   " [Severity: " + p.getSeverityDisplay() + "]");
                position++;
            }
            
            System.out.println("------------------------------------------------");
            System.out.println("Total waiting: " + patientQueue.size() + " patients");
            System.out.println("================================================\n");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a specific patient from the queue (e.g., when discharged).
     * Note: PriorityQueue doesn't support direct removal by object efficiently.
     * This rebuilds the queue without the specified patient.
     *
     * @param patientId ID of patient to remove
     * @return true if patient was found and removed
     */
    public boolean removePatient(int patientId) {
        lock.lock();
        try {
            // PriorityQueue doesn't have a direct remove by ID
            // We rebuild the queue excluding the patient
            PriorityQueue<Patient> newQueue = new PriorityQueue<>(SEVERITY_COMPARATOR);
            boolean found = false;
            
            while (!patientQueue.isEmpty()) {
                Patient p = patientQueue.poll();
                if (p.getId() == patientId) {
                    found = true;
                    System.out.println("[PRIORITY QUEUE] Removed patient ID " + patientId);
                } else {
                    newQueue.offer(p);
                }
            }
            
            patientQueue.addAll(newQueue);
            return found;
        } finally {
            lock.unlock();
        }
    }
}
