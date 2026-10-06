package com.hospital.service;

import com.hospital.database.DatabaseConnection;
import com.hospital.model.Medicine;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Centralized medicine inventory management system.
 * Handles stock tracking, dispensing, restocking, and low-stock alerts.
 * Thread-safe for concurrent pharmacy operations.
 *
 * Demonstrates: CRUD operations, inventory management, thread-safety
 */
public class InventoryManager {

    private final DatabaseConnection dbConnection;
    private final ReentrantLock lock = new ReentrantLock();

    public InventoryManager() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Adds a new medicine to the inventory.
     *
     * @param medicine Medicine to add
     * @return true if added successfully
     */
    public boolean addMedicine(Medicine medicine) {
        lock.lock();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return false;

            String query = "INSERT INTO medicines (name, category, manufacturer, unit_price, stock_quantity, min_stock_level, expiry_date) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, medicine.getName());
                pstmt.setString(2, medicine.getCategory());
                pstmt.setString(3, medicine.getManufacturer());
                pstmt.setDouble(4, medicine.getUnitPrice());
                pstmt.setInt(5, medicine.getStockQuantity());
                pstmt.setInt(6, medicine.getMinStockLevel());
                pstmt.setString(7, medicine.getExpiryDate());

                int inserted = pstmt.executeUpdate();
                if (inserted > 0) {
                    System.out.println("[INVENTORY] Medicine added: " + medicine.getName());
                    return true;
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[INVENTORY ERROR] " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Dispenses medicine (reduces stock) for a prescription.
     * Checks availability before dispensing.
     *
     * @param medicineId Medicine ID to dispense
     * @param quantity Quantity to dispense
     * @return true if dispensed successfully
     */
    public boolean dispenseMedicine(int medicineId, int quantity) {
        lock.lock();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return false;

            // Check current stock
            String checkQuery = "SELECT name, stock_quantity FROM medicines WHERE medicine_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setInt(1, medicineId);
                ResultSet rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    System.out.println("[INVENTORY] Medicine ID " + medicineId + " not found.");
                    return false;
                }

                String name = rs.getString("name");
                int currentStock = rs.getInt("stock_quantity");

                if (currentStock < quantity) {
                    System.out.println("[INVENTORY] Insufficient stock for " + name +
                                       ". Required: " + quantity + ", Available: " + currentStock);
                    return false;
                }

                // Reduce stock
                String updateQuery = "UPDATE medicines SET stock_quantity = stock_quantity - ? WHERE medicine_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                    updateStmt.setInt(1, quantity);
                    updateStmt.setInt(2, medicineId);
                    updateStmt.executeUpdate();
                }

                System.out.println("[INVENTORY] Dispensed " + quantity + "x " + name +
                                   ". Remaining stock: " + (currentStock - quantity));

                // Check low stock alert
                if ((currentStock - quantity) <= getMinStockLevel(conn, medicineId)) {
                    System.out.println("[INVENTORY ALERT] LOW STOCK: " + name +
                                       " - Only " + (currentStock - quantity) + " remaining!");
                }

                return true;
            }

        } catch (SQLException e) {
            System.err.println("[INVENTORY ERROR] " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Restocks medicine (increases stock).
     *
     * @param medicineId Medicine ID to restock
     * @param quantity Quantity to add
     * @return true if restocked successfully
     */
    public boolean restockMedicine(int medicineId, int quantity) {
        lock.lock();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return false;

            String query = "UPDATE medicines SET stock_quantity = stock_quantity + ?, " +
                          "last_restocked = NOW() WHERE medicine_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, quantity);
                pstmt.setInt(2, medicineId);

                int updated = pstmt.executeUpdate();
                if (updated > 0) {
                    System.out.println("[INVENTORY] Restocked +" + quantity + " units (Medicine ID: " + medicineId + ")");
                    return true;
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[INVENTORY ERROR] " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the minimum stock level for a medicine.
     */
    private int getMinStockLevel(Connection conn, int medicineId) throws SQLException {
        String query = "SELECT min_stock_level FROM medicines WHERE medicine_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, medicineId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("min_stock_level");
            }
        }
        return 10;
    }

    /**
     * Gets a medicine by ID.
     *
     * @param medicineId Medicine ID
     * @return Medicine object or null
     */
    public Medicine getMedicine(int medicineId) {
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return null;

            String query = "SELECT * FROM medicines WHERE medicine_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, medicineId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return extractMedicine(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[INVENTORY ERROR] " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets all medicines in inventory.
     *
     * @return List of all medicines
     */
    public List<Medicine> getAllMedicines() {
        List<Medicine> medicines = new ArrayList<>();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return medicines;

            String query = "SELECT * FROM medicines ORDER BY name";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    medicines.add(extractMedicine(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[INVENTORY ERROR] " + e.getMessage());
        }
        return medicines;
    }

    /**
     * Gets all low-stock medicines.
     *
     * @return List of medicines with stock below minimum level
     */
    public List<Medicine> getLowStockMedicines() {
        List<Medicine> lowStock = new ArrayList<>();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return lowStock;

            String query = "SELECT * FROM medicines WHERE stock_quantity <= min_stock_level ORDER BY stock_quantity";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    lowStock.add(extractMedicine(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[INVENTORY ERROR] " + e.getMessage());
        }
        return lowStock;
    }

    /**
     * Updates medicine details.
     *
     * @param medicine Updated medicine object
     * @return true if updated successfully
     */
    public boolean updateMedicine(Medicine medicine) {
        lock.lock();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return false;

            String query = "UPDATE medicines SET name = ?, category = ?, manufacturer = ?, " +
                          "unit_price = ?, stock_quantity = ?, min_stock_level = ?, expiry_date = ? " +
                          "WHERE medicine_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, medicine.getName());
                pstmt.setString(2, medicine.getCategory());
                pstmt.setString(3, medicine.getManufacturer());
                pstmt.setDouble(4, medicine.getUnitPrice());
                pstmt.setInt(5, medicine.getStockQuantity());
                pstmt.setInt(6, medicine.getMinStockLevel());
                pstmt.setString(7, medicine.getExpiryDate());
                pstmt.setInt(8, medicine.getMedicineId());

                int updated = pstmt.executeUpdate();
                return updated > 0;
            }
        } catch (SQLException e) {
            System.err.println("[INVENTORY ERROR] " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Deletes a medicine from inventory.
     *
     * @param medicineId Medicine ID to delete
     * @return true if deleted successfully
     */
    public boolean deleteMedicine(int medicineId) {
        lock.lock();
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return false;

            String query = "DELETE FROM medicines WHERE medicine_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, medicineId);
                int deleted = pstmt.executeUpdate();
                if (deleted > 0) {
                    System.out.println("[INVENTORY] Medicine ID " + medicineId + " deleted.");
                    return true;
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[INVENTORY ERROR] " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Displays full inventory status.
     */
    public void displayInventory() {
        try {
            Connection conn = dbConnection.getConnection();
            if (conn == null) return;

            String query = "SELECT * FROM medicines ORDER BY category, name";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                System.out.println("\n========== MEDICINE INVENTORY ==========");
                
                String currentCategory = "";
                int totalItems = 0;
                int lowStockItems = 0;

                while (rs.next()) {
                    String category = rs.getString("category");
                    if (!category.equals(currentCategory)) {
                        currentCategory = category;
                        System.out.println("\n--- " + category + " ---");
                    }

                    String name = rs.getString("name");
                    int stock = rs.getInt("stock_quantity");
                    int minStock = rs.getInt("min_stock_level");
                    double price = rs.getDouble("unit_price");

                    String lowStockMarker = (stock <= minStock) ? " [LOW STOCK!]" : "";
                    if (stock <= minStock) lowStockItems++;

                    System.out.println("  " + name +
                                       " | Stock: " + stock +
                                       " | Price: Rs." + String.format("%.2f", price) +
                                       lowStockMarker);
                    totalItems++;
                }

                System.out.println("\nSummary: " + totalItems + " medicine types | " +
                                   lowStockItems + " low-stock items");
                System.out.println("=========================================\n");
            }

        } catch (SQLException e) {
            System.err.println("[INVENTORY ERROR] " + e.getMessage());
        }
    }

    /**
     * Displays low stock alerts.
     */
    public void displayLowStockAlerts() {
        List<Medicine> lowStock = getLowStockMedicines();
        if (lowStock.isEmpty()) {
            System.out.println("[INVENTORY] No low-stock medicines. Inventory is healthy.");
            return;
        }

        System.out.println("\n========== LOW STOCK ALERTS ==========");
        for (Medicine m : lowStock) {
            System.out.println("[ALERT] " + m.getName() +
                               " - Only " + m.getStockQuantity() +
                               " remaining (Min: " + m.getMinStockLevel() + ")");
        }
        System.out.println("======================================\n");
    }

    /**
     * Extracts Medicine object from ResultSet.
     */
    private Medicine extractMedicine(ResultSet rs) throws SQLException {
        return new Medicine(
            rs.getInt("medicine_id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("manufacturer"),
            rs.getDouble("unit_price"),
            rs.getInt("stock_quantity"),
            rs.getInt("min_stock_level"),
            rs.getString("expiry_date"),
            rs.getString("last_restocked")
        );
    }
}
