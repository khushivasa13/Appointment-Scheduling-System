# Appointment Scheduling System

A Java JDBC application using Oracle Database to manage faculty appointment slots and student bookings.

## Features
- Add and view faculty appointment slots
- Book appointments
- Cancel appointments
- View appointment records
- Prevent duplicate bookings for the same slot

## Technologies
- Java
- JDBC
- Oracle Database
- SQL

## Database Setup
Run `database.sql` in your Oracle SQL worksheet to create the required tables and appointment sequence.

## Run the Application
1. Install the JDK and download an Oracle JDBC driver.
2. Configure your Oracle database connection details in `AppointmentApp.java`.
3. Place the JDBC JAR in the project folder.
4. Compile and run on Windows:

```powershell
javac -cp ".;ojdbc17.jar" AppointmentApp.java
java -cp ".;ojdbc17.jar" AppointmentApp
```

Use the actual JAR filename if it differs.

## Security
Database credentials should not be committed to GitHub. The JDBC driver and compiled `.class` files are excluded from this repository.