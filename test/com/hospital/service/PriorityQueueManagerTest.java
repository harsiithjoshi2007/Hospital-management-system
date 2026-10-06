package com.hospital.service;

import com.hospital.model.Patient;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive unit tests for PriorityQueueManager.
 * Tests:
 * - Severity-based ordering (critical patients first)
 * - FIFO within same severity level
 * - Queue operations (enqueue, dequeue, peek, size, isEmpty)
 * - Patient removal by ID
 * - Thread safety under concurrent access
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PriorityQueueManagerTest {

    private PriorityQueueManager queueManager;

    @BeforeEach
    void setUp() {
        queueManager = new PriorityQueueManager();
    }

    // ==============================================================
    //  CRITICAL: Severity-Based Ordering
    // ==============================================================

    @Test
    @Order(1)
    @DisplayName("Critical patient is dequeued before Minor patient")
    void criticalPatientBeforeMinor() {
        Patient minor = new Patient(1, "Alice", 30, "Female", "111", "Addr", Patient.SEVERITY_MINOR);
        Patient critical = new Patient(2, "Bob", 45, "Male", "222", "Addr", Patient.SEVERITY_CRITICAL);

        queueManager.enqueuePatient(minor);
        queueManager.enqueuePatient(critical);

        // Wait a tiny bit to ensure both timestamps are set
        Patient first = queueManager.dequeuePatient();
        assertEquals(2, first.getId(), "Critical patient (Bob) should be dequeued first");
        assertEquals("Bob", first.getName());

        Patient second = queueManager.dequeuePatient();
        assertEquals(1, second.getId(), "Minor patient (Alice) should be dequeued second");
    }

    @Test
    @Order(2)
    @DisplayName("Full severity ordering: Critical > Serious > Urgent > Moderate > Minor")
    void fullSeverityOrdering() {
        List<Patient> patients = List.of(
            new Patient(1, "Minor", 20, "Male", "1", "Addr", Patient.SEVERITY_MINOR),
            new Patient(2, "Moderate", 25, "Female", "2", "Addr", Patient.SEVERITY_MODERATE),
            new Patient(3, "Urgent", 30, "Male", "3", "Addr", Patient.SEVERITY_URGENT),
            new Patient(4, "Serious", 35, "Female", "4", "Addr", Patient.SEVERITY_SERIOUS),
            new Patient(5, "Critical", 40, "Male", "5", "Addr", Patient.SEVERITY_CRITICAL)
        );

        // Enqueue in random order (not sorted)
        int[] order = {2, 5, 1, 4, 3}; // Moderate, Critical, Minor, Serious, Urgent
        for (int i : order) {
            queueManager.enqueuePatient(patients.get(i - 1));
        }

        // Dequeue should be in severity order: Critical, Serious, Urgent, Moderate, Minor
        assertAll("Severity ordering",
            () -> assertEquals("Critical", queueManager.dequeuePatient().getName(),
                    "First should be Critical"),
            () -> assertEquals("Serious", queueManager.dequeuePatient().getName(),
                    "Second should be Serious"),
            () -> assertEquals("Urgent", queueManager.dequeuePatient().getName(),
                    "Third should be Urgent"),
            () -> assertEquals("Moderate", queueManager.dequeuePatient().getName(),
                    "Fourth should be Moderate"),
            () -> assertEquals("Minor", queueManager.dequeuePatient().getName(),
                    "Fifth should be Minor")
        );
    }

    // ==============================================================
    //  FIFO Within Same Severity
    // ==============================================================

    @Test
    @Order(3)
    @DisplayName("Patients with same severity are served FIFO (first come, first served)")
    void sameSeverityFifoOrdering() {
        // Set explicit timestamps to avoid flaky Thread.sleep() calls
        Patient p1 = new Patient(1, "First", 20, "Male", "1", "Addr", Patient.SEVERITY_URGENT);
        p1.setArrivalTimestamp(1000);  // Arrived first

        Patient p2 = new Patient(2, "Second", 30, "Female", "2", "Addr", Patient.SEVERITY_URGENT);
        p2.setArrivalTimestamp(2000);  // Arrived second

        Patient p3 = new Patient(3, "Third", 40, "Male", "3", "Addr", Patient.SEVERITY_URGENT);
        p3.setArrivalTimestamp(3000);  // Arrived third

        queueManager.enqueuePatient(p3); // Enqueued 3rd, arrived 3rd
        queueManager.enqueuePatient(p1); // Enqueued 1st, arrived 1st
        queueManager.enqueuePatient(p2); // Enqueued 2nd, arrived 2nd

        // Should still be FIFO by arrival timestamp, not by enqueue order
        assertEquals("First", queueManager.dequeuePatient().getName());
        assertEquals("Second", queueManager.dequeuePatient().getName());
        assertEquals("Third", queueManager.dequeuePatient().getName());
    }

    // ==============================================================
    //  Queue Operations
    // ==============================================================

    @Test
    @Order(4)
    @DisplayName("Dequeue from empty queue returns null")
    void dequeueFromEmptyQueue() {
        assertNull(queueManager.dequeuePatient(), "Dequeue from empty queue should return null");
    }

    @Test
    @Order(5)
    @DisplayName("Peek returns highest-priority patient without removing")
    void peekReturnsWithoutRemoving() {
        Patient p1 = new Patient(1, "PeekTest", 25, "Female", "1", "Addr", Patient.SEVERITY_CRITICAL);
        queueManager.enqueuePatient(p1);

        Patient peeked = queueManager.peekNextPatient();
        assertNotNull(peeked);
        assertEquals(1, peeked.getId());
        assertEquals(1, queueManager.size(), "Queue size should remain 1 after peek");
    }

    @Test
    @Order(6)
    @DisplayName("Peek on empty queue returns null")
    void peekOnEmptyQueue() {
        assertNull(queueManager.peekNextPatient());
    }

    @Test
    @Order(7)
    @DisplayName("isEmpty returns true for empty queue, false otherwise")
    void isEmptyWorks() {
        assertTrue(queueManager.isEmpty(), "New queue should be empty");

        queueManager.enqueuePatient(new Patient(1, "Test", 20, "Male", "1", "Addr", Patient.SEVERITY_MINOR));
        assertFalse(queueManager.isEmpty(), "Queue with patient should not be empty");

        queueManager.dequeuePatient();
        assertTrue(queueManager.isEmpty(), "Queue should be empty after dequeue");
    }

    @Test
    @Order(8)
    @DisplayName("Size tracks patient count correctly")
    void sizeTracking() {
        assertEquals(0, queueManager.size());

        queueManager.enqueuePatient(new Patient(1, "A", 20, "M", "1", "Addr", 1));
        assertEquals(1, queueManager.size());

        queueManager.enqueuePatient(new Patient(2, "B", 30, "F", "2", "Addr", 2));
        assertEquals(2, queueManager.size());

        queueManager.dequeuePatient();
        assertEquals(1, queueManager.size());

        queueManager.dequeuePatient();
        assertEquals(0, queueManager.size());
    }

    // ==============================================================
    //  Patient Removal
    // ==============================================================

    @Test
    @Order(9)
    @DisplayName("RemovePatient removes specific patient by ID")
    void removePatientById() {
        Patient p1 = new Patient(1, "Keep", 20, "M", "1", "Addr", Patient.SEVERITY_CRITICAL);
        Patient p2 = new Patient(2, "Remove", 30, "F", "2", "Addr", Patient.SEVERITY_URGENT);
        Patient p3 = new Patient(3, "Keep2", 40, "M", "3", "Addr", Patient.SEVERITY_MODERATE);

        queueManager.enqueuePatient(p1);
        queueManager.enqueuePatient(p2);
        queueManager.enqueuePatient(p3);

        assertTrue(queueManager.removePatient(2), "Should find and remove patient ID 2");
        assertEquals(2, queueManager.size(), "Queue should have 2 patients remaining");

        // Verify remaining patients are p1 and p3
        Patient first = queueManager.dequeuePatient();
        assertEquals(1, first.getId(), "Critical patient (ID 1) should remain");

        Patient second = queueManager.dequeuePatient();
        assertEquals(3, second.getId(), "Moderate patient (ID 3) should remain");
    }

    @Test
    @Order(10)
    @DisplayName("RemovePatient returns false for non-existent ID")
    void removePatientNotFound() {
        Patient p1 = new Patient(1, "Only", 20, "M", "1", "Addr", Patient.SEVERITY_MINOR);
        queueManager.enqueuePatient(p1);

        assertFalse(queueManager.removePatient(99), "Non-existent ID should return false");
        assertEquals(1, queueManager.size(), "Queue size should remain unchanged");
    }

    // ==============================================================
    //  Thread Safety (Multithreading)
    // ==============================================================

    @Test
    @Order(11)
    @DisplayName("Concurrent enqueue/dequeue by multiple threads is thread-safe")
    void concurrentAccessThreadSafety() throws InterruptedException {
        int numThreads = 10;
        int opsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Concurrently enqueue patients
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        Patient p = new Patient(
                            threadId * opsPerThread + i,
                            "Patient-" + threadId + "-" + i,
                            20 + (i % 50),
                            "Male",
                            "Contact-" + i,
                            "Addr",
                            (i % 5) + 1  // Vary severity 1-5
                        );
                        queueManager.enqueuePatient(p);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, "Enqueuer-" + t).start();
        }

        latch.await();
        assertEquals(0, errorCount.get(), "No errors during concurrent enqueue");
        assertEquals(numThreads * opsPerThread, queueManager.size(),
                "All patients should be in queue after concurrent enqueue");

        // Concurrently dequeue all patients
        CountDownLatch dequeueLatch = new CountDownLatch(numThreads);
        AtomicInteger totalDequeued = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            new Thread(() -> {
                try {
                    while (true) {
                        Patient p = queueManager.dequeuePatient();
                        if (p == null) break;
                        totalDequeued.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    dequeueLatch.countDown();
                }
            }, "Dequeuer-" + t).start();
        }

        dequeueLatch.await();
        assertEquals(0, errorCount.get(), "No errors during concurrent dequeue");
        assertEquals(numThreads * opsPerThread, totalDequeued.get(),
                "All patients should be successfully dequeued");
        assertTrue(queueManager.isEmpty(), "Queue should be empty after full dequeue");
    }

    @Test
    @Order(12)
    @DisplayName("Multiple thread types (enqueue + remove + peek) don't cause deadlocks")
    void mixedOperationsThreadSafety() throws InterruptedException {
        // Prime the queue with some patients
        for (int i = 0; i < 50; i++) {
            queueManager.enqueuePatient(
                new Patient(i, "Base-" + i, 30, "F", "C", "Addr", (i % 5) + 1));
        }

        int numThreads = 5;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < 20; i++) {
                        // Enqueue
                        queueManager.enqueuePatient(
                            new Patient(100 + threadId * 20 + i, "New-" + threadId + "-" + i,
                                        25, "M", "C", "Addr", (i % 5) + 1));
                        // Peek
                        queueManager.peekNextPatient();
                        // Remove some
                        if (i % 5 == 0) {
                            queueManager.removePatient(i);
                        }
                        // Check size
                        queueManager.size();
                        // Check isEmpty
                        queueManager.isEmpty();
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
