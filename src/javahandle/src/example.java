import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class example {
    static final String DB_URL = "jdbc:postgresql://localhost:5432/railwaydb";
    static final String USER = "postgres";
    static final String PASS = "admin";

    static final String QUERY = "{call getEmpName (?, ?)}";

    public static void main(String[] args) {
        // Open a connection
        try(Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            CallableStatement stmt = conn.prepareCall(QUERY);
        ) {
            // Bind values into the parameters.
            stmt.setInt(1, 1);  // This would set ID
            // Because second parameter is OUT so register it
            stmt.registerOutParameter(2, java.sql.Types.VARCHAR);
            //Use execute method to run stored procedure.
            System.out.println("Executing stored procedure..." );
            stmt.execute();
            //Retrieve employee name with getXXX method
            String empName = stmt.getString(2);
            System.out.println("Emp Name with ID: 1 is " + empName);
        } catch (SQLException e) {
            System.out.println("Hi");
//            e.printStackTrace();
        }
    }
}