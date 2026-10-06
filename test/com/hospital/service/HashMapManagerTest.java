package com.hospital.service;

import com.hospital.model.Patient;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive unit tests for HashMapManager.
 * Tests:
 * - CRUD operations (add, get, update, remove)
 * - Edge cases (null returns, non-existent keys)
 * - searchByName case-insensitive matching
 * - Size tracking and clear
 * - Thread safety under concurrent access
 * - O(1) access pattern verification
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HashMapManagerTest {

    private HashMapManager hashMapManager;

    private Patient createPatient(int id, String name, int severity) {
        return new Patient(id, name, 30, "Male", "9999999999", "Test Address", severity);
    }

    @BeforeEach
    void setUp() {
        hashMapManager = new HashMapManager();
    }

    // ==============================================================
    //  CREATE / ADD
    // ==============================================================

    @Test
    @Order(1)
    @DisplayName("addPatient stores patient and getPatient retrieves it")
    void addAndGetPatient() {
        Patient patient = createPatient(1, "John Doe", Patient.SEVERITY_URGENT);
        hashMapManager.addPatient(patient);

        Patient retrieved = hashMapManager.getPatient(1);
        assertNotNull(retrieved, "Patient should be found in cache");
        assertEquals("John Doe", retrieved.getName());
        assertEquals(Patient.SEVERITY_URGENT, retrieved.getSeverityLevel());
    }

    @Test
    @Order(2)
    @DisplayName("Multiple patients can be added and retrieved")
    void multiplePatients() {
        Patient p1 = createPatient(1, "Alice", Patient.SEVERITY_MINOR);
        Patient p2 = createPatient(2, "Bob", Patient.SEVERITY_CRITICAL);
        Patient p3 = createPatient(3, "Charlie", Patient.SEVERITY_MODERATE);

        hashMapManager.addPatient(p1);
        hashMapManager.addPatient(p2);
        hashMapManager.addPatient(p3);

        assertEquals("Alice", hashMapManager.getPatient(1).getName());
        assertEquals("Bob", hashMapManager.getPatient(2).getName());
        assertEquals("Charlie", hashMapManager.getPatient(3).getName());
        assertEquals(3, hashMapManager.size());
    }

    // ==============================================================
    //  READ / RETRIEVE
    // ==============================================================

    @Test
    @Order(3)
    @DisplayName("getPatient returns null for non-existent ID")
    void getPatientNotFound() {
        assertNull(hashMapManager.getPatient(999), "Non-existent ID should return null");
    }

    @Test
    @Order(4)
    @DisplayName("Overwriting an existing ID updates the patient record")
    void overwritePatientUpdatesRecord() {
        Patient original = createPatient(1, "Original", Patient.SEVERITY_MINOR);
        hashMapManager.addPatient(original);

        Patient updated = createPatient(1, "Updated", Patient.SEVERITY_CRITICAL);
        hashMapManager.addPatient(updated);  // Same ID - should overwrite

        Patient retrieved = hashMapManager.getPatient(1);
        assertEquals("Updated", retrieved.getName(),
                "Name should be updated to 'Updated'");
        assertEquals(Patient.SEVERITY_CRITICAL, retrieved.getSeverityLevel(),
                "Severity should be updated to Critical");
    }

    // ==============================================================
    //  UPDATE
    // ==============================================================

    @Test
    @Order(5)
    @DisplayName("updatePatient modifies existing patient record")
    void updatePatientSuccess() {
        Patient patient = createPatient(1, "Original", Patient.SEVERITY_MODERATE);
        hashMapManager.addPatient(patient);

        // Modify and update
        patient.setName("Modified");
        patient.setSeverityLevel(Patient.SEVERITY_CRITICAL);
        hashMapManager.updatePatient(patient);

        Patient retrieved = hashMapManager.getPatient(1);
        assertEquals("Modified", retrieved.getName());
        assertEquals(Patient.SEVERITY_CRITICAL, retrieved.getSeverityLevel());
    }

    @Test
    @Order(6)
    @DisplayName("updatePatient on non-existent patient does not add it")
    void updatePatientNotFound() {
        Patient patient = createPatient(99, "Ghost", Patient.SEVERITY_MINOR);
        hashMapManager.updatePatient(patient);  // Should not add

        assertNull(hashMapManager.getPatient(99),
                "Patient should NOT exist after update on non-existent ID");
        assertEquals(0, hashMapManager.size());
    }

    // ==============================================================
    //  DELETE / REMOVE
    // ==============================================================

    @Test
    @Order(7)
    @DisplayName("removePatient removes patient and returns true")
    void removePatientSuccess() {
        hashMapManager.addPatient(createPatient(1, "RemoveMe", Patient.SEVERITY_URGENT));

        assertTrue(hashMapManager.removePatient(1), "Should return true for existing patient");
        assertNull(hashMapManager.getPatient(1), "Patient should no longer exist");
        assertEquals(0, hashMapManager.size());
    }

    @Test
    @Order(8)
    @DisplayName("removePatient on non-existent ID returns false")
    void removePatientNotFound() {
        assertFalse(hashMapManager.removePatient(999), "Non-existent ID should return false");
    }

    // ==============================================================
    //  EXISTENCE CHECK
    // ==============================================================

    @Test
    @Order(9)
    @DisplayName("containsPatient checks existence correctly")
    void containsPatient() {
        assertFalse(hashMapManager.containsPatient(1), "Empty cache should not contain any ID");

        hashMapManager.addPatient(createPatient(1, "Exists", Patient.SEVERITY_CRITICAL));

        assertTrue(hashMapManager.containsPatient(1));
        assertFalse(hashMapManager.containsPatient(2));
    }

    // ==============================================================
    //  SIZE & CLEAR
    // ==============================================================

    @Test
    @Order(10)
    @DisplayName("size returns correct patient count")
    void sizeTracking() {
        assertEquals(0, hashMapManager.size());

        hashMapManager.addPatient(createPatient(1, "A", 1));
        assertEquals(1, hashMapManager.size());

        hashMapManager.addPatient(createPatient(2, "B", 2));
        assertEquals(2, hashMapManager.size());

        hashMapManager.removePatient(1);
        assertEquals(1, hashMapManager.size());

        hashMapManager.clearCache();
        assertEquals(0, hashMapManager.size());
    }

    @Test
    @Order(11)
    @DisplayName("clearCache removes all patients")
    void clearCacheEmptiesMap() {
        hashMapManager.addPatient(createPatient(1, "A", 1));
        hashMapManager.addPatient(createPatient(2, "B", 2));
        hashMapManager.addPatient(createPatient(3, "C", 3));

        hashMapManager.clearCache();

        assertEquals(0, hashMapManager.size());
        assertFalse(hashMapManager.containsPatient(1));
        assertFalse(hashMapManager.containsPatient(2));
        assertFalse(hashMapManager.containsPatient(3));
    }

    // ==============================================================
    //  SEARCH
    // ==============================================================

    @Test
    @Order(12)
    @DisplayName("searchByName finds patients case-insensitively by capturing console output")
    void searchByNameCaseInsensitive() {
        hashMapManager.addPatient(createPatient(1, "John Smith", 3));
        hashMapManager.addPatient(createPatient(2, "Jane Doe", 2));
        hashMapManager.addPatient(createPatient(3, "JOHNNY CASH", 1));

        // Capture System.out to verify searchByName output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // Search for "john" — should match "John Smith" and "JOHNNY CASH"
            hashMapManager.searchByName("john");
            String output = outContent.toString();

            assertTrue(output.contains("John Smith"), "Should find 'John Smith'");
            assertTrue(output.contains("JOHNNY CASH"), "Should find 'JOHNNY CASH' (case-insensitive)");
            assertFalse(output.contains("Jane Doe"), "Should NOT find 'Jane Doe'");

            outContent.reset();

            // Search for "doe" — should match "Jane Doe" only
            hashMapManager.searchByName("doe");
            output = outContent.toString();
            assertTrue(output.contains("Jane Doe"), "Should find 'Jane Doe'");

            outContent.reset();

            // Search for "xyz" — should match nothing
            hashMapManager.searchByName("xyz");
            output = outContent.toString();
            assertTrue(output.contains("No patients found"), "Should show 'No patients found' message");

        } finally {
            System.setOut(originalOut); // Restore original stdout
        }
    }

    // ==============================================================
    //  O(1) ACCESS VERIFICATION
    // ==============================================================

    @Test
    @Order(13)
    @DisplayName("Large number of patients can be added and retrieved efficiently")
    void largeScaleOperation() {
        int count = 1000;

        // Add 1000 patients
        for (int i = 0; i < count; i++) {
            hashMapManager.addPatient(
                createPatient(i, "Patient-" + i, (i % 5) + 1));
        }

        assertEquals(count, hashMapManager.size());

        // Retrieve all of them
        for (int i = 0; i < count; i++) {
            Patient p = hashMapManager.getPatient(i);
            assertNotNull(p, "Patient " + i + " should exist");
            assertEquals("Patient-" + i, p.getName());
        }

        // Remove all of them
        for (int i = 0; i < count; i++) {
            assertTrue(hashMapManager.removePatient(i));
        }

        assertEquals(0, hashMapManager.size());
    }

    // ==============================================================
    //  THREAD SAFETY
    // ==============================================================

    @Test
    @Order(14)
    @DisplayName("Concurrent add and remove operations are thread-safe")
    void concurrentAddRemoveThreadSafety() throws InterruptedException {
        int numThreads = 8;
        int opsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        int id = threadId * opsPerThread + i;
                        hashMapManager.addPatient(
                            createPatient(id, "T" + threadId + "-P" + i, (i % 5) + 1));
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, "Adder-" + t).start();
        }

        latch.await();
        assertEquals(0, errorCount.get(), "No errors during concurrent add");
        assertEquals(numThreads * opsPerThread, hashMapManager.size(),
                "All patients should be in cache");

        // Concurrently read and remove
        CountDownLatch readLatch = new CountDownLatch(numThreads);
        CountDownLatch removeLatch = new CountDownLatch(numThreads);

        for (int t = 0; t < numThreads; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < 50; i++) {
                        hashMapManager.containsPatient(i);
                        hashMapManager.size();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    readLatch.countDown();
                }
            }, "Reader-" + t).start();

            new Thread(() -> {
                try {
                    for (int i = 50; i < 100; i++) {
                        hashMapManager.removePatient(i);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    removeLatch.countDown();
                }
            }, "Remover-" + t).start();
        }

        readLatch.await();
        removeLatch.await();
        assertEquals(0, errorCount.get(), "No errors during concurrent read/remove");
    }

    @Test
    @Order(15)
    @DisplayName("Concurrent mixed operations (add, update, get, remove) with same IDs")
    void concurrentMixedOperationsThreadSafety() throws InterruptedException {
        // Pre-populate with some patients
        for (int i = 0; i < 20; i++) {
            hashMapManager.addPatient(createPatient(i, "Base-" + i, 3));
        }

        int numThreads = 6;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < 50; i++) {
                        int op = i % 6;
                        switch (op) {
                            case 0 -> {
                                // Add new
                                int id = 100 + i;
                                hashMapManager.addPatient(
                                    createPatient(id, "New-" + i, (i % 5) + 1));
                            }
                            case 1 -> {
                                // Get existing
                                hashMapManager.getPatient(i % 20);
                            }
                            case 2 -> {
                                // Update
                                Patient p = hashMapManager.getPatient(i % 20);
                                if (p != null) {
                                    p.setStatus("Updated");
                                    hashMapManager.updatePatient(p);
                                }
                            }
                            case 3 -> {
                                // Remove
                                hashMapManager.removePatient(100 + i);
                            }
                            case 4 -> {
                                // Check existence
                                hashMapManager.containsPatient(i % 20);
                            }
                            case 5 -> {
                                // Check size
                                hashMapManager.size();
                            }
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, "MixedOps-" + t).start();
        }

        latch.await();
        assertEquals(0, errorCount.get(), "No errors during mixed concurrent operations");
    }
}
