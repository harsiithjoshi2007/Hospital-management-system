package com.hospital.model;

/**
 * Medicine class representing pharmaceutical items in inventory.
 * Used for centralized medicine inventory management.
 */
public class Medicine {

    private int medicineId;
    private String name;
    private String category;
    private String manufacturer;
    private double unitPrice;
    private int stockQuantity;
    private int minStockLevel;
    private String expiryDate;
    private String lastRestocked;

    public Medicine() {
        this.minStockLevel = 10;
    }

    public Medicine(int medicineId, String name, String category, String manufacturer,
                    double unitPrice, int stockQuantity, int minStockLevel,
                    String expiryDate, String lastRestocked) {
        this.medicineId = medicineId;
        this.name = name;
        this.category = category;
        this.manufacturer = manufacturer;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.minStockLevel = minStockLevel;
        this.expiryDate = expiryDate;
        this.lastRestocked = lastRestocked;
    }

    // ---- Getters and Setters ----

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public int getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(int minStockLevel) { this.minStockLevel = minStockLevel; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getLastRestocked() { return lastRestocked; }
    public void setLastRestocked(String lastRestocked) { this.lastRestocked = lastRestocked; }

    /**
     * Checks if stock is low (below minimum level).
     *
     * @return true if stock is low
     */
    public boolean isLowStock() {
        return stockQuantity <= minStockLevel;
    }

    /**
     * Checks if medicine is available in desired quantity.
     *
     * @param requiredQuantity Quantity needed
     * @return true if sufficient stock available
     */
    public boolean isAvailable(int requiredQuantity) {
        return stockQuantity >= requiredQuantity;
    }

    @Override
    public String toString() {
        return "ID: " + medicineId +
               " | Name: " + name +
               " | Category: " + category +
               " | Stock: " + stockQuantity +
               " | Price: Rs." + unitPrice +
               (isLowStock() ? " [LOW STOCK!]" : "");
    }
}
