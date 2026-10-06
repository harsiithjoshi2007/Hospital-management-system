package com.hospital.model;

/**
 * Abstract base class demonstrating Abstraction in OOP.
 * Provides common attributes and behavior for all person types in the system.
 */
public abstract class Person {

    // Encapsulated fields - private access with getters/setters
    private int id;
    private String name;
    private int age;
    private String gender;
    private String contact;

    /**
     * Default constructor.
     */
    public Person() {
    }

    /**
     * Parameterized constructor to initialize common person fields.
     *
     * @param id      Unique identifier
     * @param name    Full name
     * @param age     Age in years
     * @param gender  Gender
     * @param contact Contact number
     */
    public Person(int id, String name, int age, String gender, String contact) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.contact = contact;
    }

    // ---- Getters and Setters (Encapsulation) ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    /**
     * Abstract method that must be implemented by subclasses.
     * Each person type should provide its role description.
     *
     * @return Role description string
     */
    public abstract String getRole();

    /**
     * Returns basic person information as a formatted string.
     * Polymorphic - can be overridden by subclasses.
     *
     * @return Formatted person details
     */
    @Override
    public String toString() {
        return "ID: " + id +
               " | Name: " + name +
               " | Age: " + age +
               " | Gender: " + gender +
               " | Contact: " + contact;
    }
}
