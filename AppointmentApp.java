
import java.sql.*;
import java.util.Scanner;

public class AppointmentApp {
    static final String URL =
        "jdbc:oracle:thin:@tcps://db.freesql.com:2484/26ai_un3c1";
    static final String USER = "VASAKHUSHI13_SCHEMA_XS6YC";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            System.out.print("Enter Oracle schema password: ");
            String password = sc.nextLine();

            try (Connection con =
                     DriverManager.getConnection(URL, USER, password)) {
                int choice;

                do {
                    System.out.println("\n--- APPOINTMENT SYSTEM ---");
                    System.out.println("1. Add available slot");
                    System.out.println("2. View available slots");
                    System.out.println("3. Book appointment");
                    System.out.println("4. Cancel appointment");
                    System.out.println("5. View all appointments");
                    System.out.println("6. Exit");
                    System.out.print("Enter choice: ");

                    choice = Integer.parseInt(sc.nextLine());

                    switch (choice) {
                        case 1:
                            addSlot(con);
                            break;
                        case 2:
                            viewSlots(con);
                            break;
                        case 3:
                            bookAppointment(con);
                            break;
                        case 4:
                            cancelAppointment(con);
                            break;
                        case 5:
                            viewAppointments(con);
                            break;
                        case 6:
                            System.out.println("Goodbye!");
                            break;
                        default:
                            System.out.println("Invalid choice.");
                    }
                } while (choice != 6);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void addSlot(Connection con) throws SQLException {
        System.out.print("Enter slot ID: ");
        int id = Integer.parseInt(sc.nextLine());

        System.out.print("Enter doctor/faculty name: ");
        String name = sc.nextLine();

        System.out.print("Enter date and time (yyyy-mm-dd hh:mm:ss): ");
        String time = sc.nextLine();

        String sql =
            "INSERT INTO APPOINTMENT_SLOT " +
            "(SLOT_ID, FACULTY_NAME, SLOT_TIME, STATUS) " +
            "VALUES (?, ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'), 'AVAILABLE')";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, time);
            ps.executeUpdate();
            System.out.println("Slot added successfully.");
        }
    }

    static void viewSlots(Connection con) throws SQLException {
        String sql =
            "SELECT SLOT_ID, FACULTY_NAME, SLOT_TIME " +
            "FROM APPOINTMENT_SLOT WHERE STATUS = 'AVAILABLE' " +
            "ORDER BY SLOT_TIME";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\nAvailable slots:");
            while (rs.next()) {
                System.out.println(
                    "ID: " + rs.getInt("SLOT_ID") +
                    " | Faculty: " + rs.getString("FACULTY_NAME") +
                    " | Time: " + rs.getTimestamp("SLOT_TIME")
                );
            }
        }
    }

    static void bookAppointment(Connection con) throws SQLException {
        System.out.print("Enter slot ID to book: ");
        int slotId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter your name: ");
        String student = sc.nextLine();

        System.out.print("Enter reason: ");
        String reason = sc.nextLine();

        try {
            con.setAutoCommit(false);

            String lockSql =
                "SELECT STATUS FROM APPOINTMENT_SLOT " +
                "WHERE SLOT_ID = ? FOR UPDATE";

            try (PreparedStatement lock = con.prepareStatement(lockSql)) {
                lock.setInt(1, slotId);

                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next() ||
                        !rs.getString("STATUS").equals("AVAILABLE")) {
                        System.out.println("Slot unavailable or not found.");
                        con.rollback();
                        return;
                    }
                }
            }

            String insertSql =
                "INSERT INTO APPOINTMENT " +
                "(APPOINTMENT_ID, SLOT_ID, STUDENT_NAME, REASON, BOOKING_STATUS) " +
                "VALUES (APPOINTMENT_SEQ.NEXTVAL, ?, ?, ?, 'BOOKED')";

            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setInt(1, slotId);
                ps.setString(2, student);
                ps.setString(3, reason);
                ps.executeUpdate();
            }

            String updateSql =
                "UPDATE APPOINTMENT_SLOT SET STATUS = 'BOOKED' " +
                "WHERE SLOT_ID = ?";

            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setInt(1, slotId);
                ps.executeUpdate();
            }

            con.commit();
            System.out.println("Appointment booked successfully.");

        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    static void cancelAppointment(Connection con) throws SQLException {
        System.out.print("Enter appointment ID to cancel: ");
        int appointmentId = Integer.parseInt(sc.nextLine());

        try {
            con.setAutoCommit(false);

            String findSql =
                "SELECT SLOT_ID FROM APPOINTMENT " +
                "WHERE APPOINTMENT_ID = ? AND BOOKING_STATUS = 'BOOKED'";

            int slotId;

            try (PreparedStatement ps = con.prepareStatement(findSql)) {
                ps.setInt(1, appointmentId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Active appointment not found.");
                        con.rollback();
                        return;
                    }
                    slotId = rs.getInt("SLOT_ID");
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE APPOINTMENT SET BOOKING_STATUS = 'CANCELLED' " +
                    "WHERE APPOINTMENT_ID = ?")) {
                ps.setInt(1, appointmentId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE APPOINTMENT_SLOT SET STATUS = 'AVAILABLE' " +
                    "WHERE SLOT_ID = ?")) {
                ps.setInt(1, slotId);
                ps.executeUpdate();
            }

            con.commit();
            System.out.println("Appointment cancelled. Slot is available again.");

        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    static void viewAppointments(Connection con) throws SQLException {
        String sql =
            "SELECT A.APPOINTMENT_ID, A.STUDENT_NAME, A.REASON, " +
            "A.BOOKING_STATUS, S.FACULTY_NAME, S.SLOT_TIME " +
            "FROM APPOINTMENT A JOIN APPOINTMENT_SLOT S " +
            "ON A.SLOT_ID = S.SLOT_ID ORDER BY A.APPOINTMENT_ID";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println(
                    "Appointment ID: " + rs.getInt("APPOINTMENT_ID") +
                    " | Student: " + rs.getString("STUDENT_NAME") +
                    " | Faculty: " + rs.getString("FACULTY_NAME") +
                    " | Time: " + rs.getTimestamp("SLOT_TIME") +
                    " | Status: " + rs.getString("BOOKING_STATUS") +
                    " | Reason: " + rs.getString("REASON")
                );
            }
        }
    }
}
