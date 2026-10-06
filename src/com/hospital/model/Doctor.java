package com.hospital.model;

/**
 * Doctor class extending Person.
 * Demonstrates Inheritance and Polymorphism.
 */
public class Doctor extends Person {

    private String specialization;
    private String email;
    private boolean available;

    public Doctor() {
        super();
        this.available = true;
    }

    public Doctor(int id, String name, int age, String gender, String contact,
                  String specialization, String email) {
        super(id, name, age, gender, contact);
        this.specialization = specialization;
        this.email = email;
        this.available = true;
    }

    public Doctor(int id, String name, String specialization, String contact, String email) {
        super(id, name, 0, "", contact);
        this.specialization = specialization;
        this.email = email;
        this.available = true;
    }

    // ---- Getters and Setters ----

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String getRole() {
        return "Doctor";
    }

    @Override
    public String toString() {
        return super.toString() +
               " | Specialization: " + specialization +
               " | Available: " + (available ? "Yes" : "No");
    }
}
