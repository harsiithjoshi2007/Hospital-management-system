# Intelligent Hospital Resource Management System

A purely console-based Java application with MySQL backend that intelligently manages hospital resources using Priority Queues, HashMaps, Multithreading, and automated bed allocation.

## 🏥 Features

### Core Intelligence Modules
- **Priority Queue Triage** - Patients are prioritized based on medical severity. Critical patients are served first, ensuring emergency cases get immediate attention.
- **HashMap Patient Cache** - O(1) lookup of patient records for instant access during consultations, billing, and pharmacy operations.
- **Multithreaded Operations** - Registration, Pharmacy, and Billing modules run as independent threads, enabling concurrent hospital workflows.
- **Automated Bed Allocation** - Severity-based bed assignment. Critical patients get ICU priority, while minor cases go to General wards.
- **Centralized Inventory Management** - Real-time medicine stock tracking with low-stock alerts and automated restocking triggers.

### Standard CRUD Operations
- Patient registration, update, discharge, and deletion
- Doctor management
- Medicine inventory management
- Bill generation and payment processing

### OOP Principles Demonstrated
| Principle | Implementation |
|-----------|---------------|
| **Encapsulation** | Private fields with public getters/setters in all model classes |
| **Inheritance** | `Patient` and `Doctor` extend `Person` base class |
| **Polymorphism** | Overridden `toString()`, `getRole()` methods |
| **Abstraction** | Abstract `Person` class defining common contract |

## 🛠️ Technology Stack

- **Java** 17+ (JDK 17 or higher recommended)
- **MySQL** 8.0+
- **JDBC** (Java Database Connectivity)
- **MySQL Connector/J** (mysql-connector-java)

## 📋 Prerequisites

1. **Java Development Kit (JDK) 17+**
   - Download: https://adoptium.net/ or https://www.oracle.com/java/technologies/downloads/
   - Verify: `java -version` and `javac -version`

2. **MySQL Server 8.0+**
   - Download: https://dev.mysql.com/downloads/
   - Ensure MySQL service is running

3. **MySQL Connector/J JAR**
   - Download: https://dev.mysql.com/downloads/connector/j/
   - File: `mysql-connector-j-8.x.x.jar`

## 🚀 Quick Start (Windows)

### Step 1: Install Prerequisites

1. **Install Java JDK 17+** from [Adoptium.net](https://adoptium.net/)
   - Verify: Open `cmd` and run `java -version` and `javac -version`

2. **Install MySQL 8.0+** from [dev.mysql.com/downloads/](https://dev.mysql.com/downloads/)
   - During installation, note your **root password**
   - Ensure MySQL service is running (`services.msc` → look for MySQL)

3. **Download MySQL Connector/J**
   - Go to: https://dev.mysql.com/downloads/connector/j/
   - Download the **Platform Independent** ZIP file
   - Extract it and copy the `mysql-connector-j-8.x.x.jar` file into this project's root folder

### Step 2: Create the Database

Open **MySQL Command Line Client** or **cmd** and run:
```sql
mysql -u root -p < sql\schema.sql
```
Enter your MySQL root password when prompted.

This creates the `hospital_db` database with all tables and sample data.

### Step 3: Update Database Credentials

Edit the file `src\com\hospital\database\DatabaseConnection.java`
and change these lines to match your MySQL username and password:
```java
private static final String DB_USER = "root";        // ← Your MySQL username
private static final String DB_PASSWORD = "root";    // ← Your MySQL password
```

> **Alternative:** Set these environment variables instead (no code changes needed):
> - `HOSPITAL_DB_USER` = your MySQL username
> - `HOSPITAL_DB_PASSWORD` = your MySQL password

### Step 4: Compile & Run

**Option A — Double-click the build script:**
> Just double-click `build.bat` in File Explorer!

**Option B — Run manually in cmd:**
```cmd
REM Step 1: List all Java source files
dir /s /b src\*.java > sources.txt

REM Step 2: Compile
javac -cp "mysql-connector-j-8.4.0.jar" -d out @sources.txt

REM Step 3: Clean up temp file
del sources.txt

REM Step 4: Run
java -cp "mysql-connector-j-8.4.0.jar;out" com.hospital.Main
```

**Option C — Using PowerShell:**
```powershell
.\build.ps1
```

---

## 🚀 Setup Instructions (Linux/Mac)

### Step 1: Install Prerequisites

```bash
# Install Java
sudo apt install openjdk-17-jdk   # Debian/Ubuntu
brew install openjdk@17           # macOS

# Install MySQL
sudo apt install mysql-server      # Debian/Ubuntu
brew install mysql                 # macOS
```

### Step 2: Create Database & Set Credentials
```bash
# Create database
mysql -u root -p < sql/schema.sql

# Edit credentials (or use environment variables)
nano src/com/hospital/database/DatabaseConnection.java
```

### Step 3: Compile & Run
```bash
# Download MySQL Connector JAR into project root
# https://dev.mysql.com/downloads/connector/j/

# Compile
export MYSQL_JAR=mysql-connector-j-8.4.0.jar
javac -cp ".:$MYSQL_JAR" -d out src/com/hospital/**/*.java

# Run
java -cp ".:$MYSQL_JAR:out" com.hospital.Main
```

## 📁 Project Structure

```
├── sql/
│   └── schema.sql                    # Database schema & sample data
├── src/
│   └── com/
│       └── hospital/
│           ├── Main.java             # Entry point with menu system
│           ├── database/
│           │   ├── DatabaseConnection.java  # Singleton DB connection
│           │   └── DatabaseSetup.java       # Table creation & seeding
│           ├── model/
│           │   ├── Person.java       # Abstract base class
│           │   ├── Patient.java      # Patient with severity triage
│           │   ├── Doctor.java       # Doctor details
│           │   ├── Medicine.java     # Inventory item
│           │   ├── Bed.java          # Hospital bed
│           │   └── Bill.java         # Billing record
│           ├── service/
│           │   ├── PriorityQueueManager.java  # Patient triage queue
│           │   ├── HashMapManager.java        # O(1) patient lookup
│           │   ├── BedAllocator.java          # Automated bed allocation
│           │   ├── InventoryManager.java      # Centralized inventory
│           │   ├── PatientService.java        # CRUD operations
│           │   └── BillingService.java        # Bill generation
│           └── thread/
│               ├── RegistrationThread.java    # Registration module
│               ├── PharmacyThread.java        # Pharmacy module
│               └── BillingThread.java         # Billing module
└── README.md
```

## 📊 Database Schema

The system uses 7 MySQL tables:
- `patients` - Patient records with severity triage levels
- `doctors` - Doctor information and availability
- `medicines` - Centralized inventory with stock tracking
- `beds` - Bed management with ward assignment
- `prescriptions` - Medicine prescriptions linked to patients
- `bills` - Billing records with payment tracking
- `audit_log` - System operation logs

## 🧪 Sample Data

The system automatically seeds the following sample data on first run:
- **5 Doctors** across different specializations
- **14 Beds** across 5 wards (General, ICU, Emergency, Maternity, Pediatric)
- **8 Medicines** with initial stock levels and pricing

## 🎯 System Modules

### 1. Registration Module (Thread)
- Register new patients with severity assessment
- View/update patient details
- Patient search by name/ID
- Automatic triage queue management
- Auto bed allocation for high-severity patients

### 2. Pharmacy Module (Thread)
- Dispense medicines to patients
- Stock management and restocking
- Low-stock alerts
- Add new medicines to inventory

### 3. Billing Module (Thread)
- Generate itemized bills
- Process payments
- Track unpaid/paid bills
- Patient billing history

### 4. Bed Management
- View real-time bed availability
- Severity-based auto-allocation
- Manual bed assignment/release
- Ward-wise bed status

### 5. Inventory Management
- Full inventory view
- Low-stock monitoring
- Medicine addition/restocking
- Category-wise organization

## 🔧 Troubleshooting

### "MySQL JDBC Driver not found"
Ensure `mysql-connector-java.jar` is in the classpath when compiling and running.

### "Cannot connect to database"
1. Verify MySQL service is running
2. Check credentials in `DatabaseConnection.java`
3. Ensure `hospital_db` database exists

### "Access denied for user"
Update the username and password in `DatabaseConnection.java` to match your MySQL credentials.

## 📜 License

This project is developed as a demonstration of Java and MySQL integration for hospital resource management.
