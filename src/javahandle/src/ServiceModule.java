import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class JDBCPostgreSQLConnection {
    private final String url = "jdbc:postgresql://localhost:5432/railwaydb";
    private final String user = "postgres";
    private final String password = "hiprashant";

    /**
     * Connect to the PostgreSQL database
     *
     * @return a Connection object
     */
    public Connection connect() {
        Connection conn1 = null;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            System.out.println("Error: unable to load driver class!");
            System.exit(1);
        }
        try {
            conn1 = DriverManager.getConnection(url, user, password);

            if (conn1 == null) {
                System.out.println("Failed to make connection!");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return conn1;
    }
}

class QueryRunner implements Runnable {
    // Declare socket for client access
    protected Socket socketConnection;

    public QueryRunner(Socket clientSocket) {
        this.socketConnection = clientSocket;
    }

    public void printSQLException(SQLException ex) {
        for (Throwable e : ex) {
            if (e instanceof SQLException) {
                e.printStackTrace(System.err);
                System.err.println("SQLState: " + ((SQLException) e).getSQLState());
                System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
                System.err.println("Message: " + e.getMessage());
                Throwable t = ex.getCause();
                while (t != null) {
                    System.out.println("Cause: " + t);
                    t = t.getCause();
                }
            }
        }
    }

    public void run() {

        try {
            // Reading data from client
            InputStreamReader inputStream = new InputStreamReader(socketConnection
                    .getInputStream());
            BufferedReader bufferedInput = new BufferedReader(inputStream);
            OutputStreamWriter outputStream = new OutputStreamWriter(socketConnection
                    .getOutputStream());
            BufferedWriter bufferedOutput = new BufferedWriter(outputStream);
            PrintWriter printWriter = new PrintWriter(bufferedOutput, true);

            String clientCommand = bufferedInput.readLine();
            String responseQuery = "";
            String queryInput = "";
            while (!clientCommand.equals("#")) {
                String[] tokens = clientCommand.split(" ");
                int numberofTickets = 0;
                String[] passengerName = null;
                String coachType = "";
                String date = "";
                int trainID = 0;
                try {
                    numberofTickets = Integer.parseInt(tokens[0]);
                    passengerName = new String[numberofTickets];
                    coachType = tokens[tokens.length - 1];
                    date = tokens[tokens.length - 2];
                    trainID = Integer.parseInt(tokens[tokens.length - 3]);
                    tokens[tokens.length - 4] += ',';
                    int p_count = 0;
                    String p_name = "";
                    for (int i = 1; i < tokens.length - 3; i++) {
                        if (tokens[i].charAt(tokens[i].length() - 1) == ',') {
                            p_name += tokens[i].substring(0, tokens[i].length() - 1);
                            passengerName[p_count] = p_name;
                            p_name = "";
                            p_count++;
                        } else {
                            p_name += " " + tokens[i];
                        }
                    }

                } catch (Exception ex) {
                    System.out.println("Ill Formatted Input");
                }
                JDBCPostgreSQLConnection app1 = new JDBCPostgreSQLConnection();
                Connection conn1 = null;
                JDBCPostgreSQLConnection app2 = new JDBCPostgreSQLConnection();
                Connection conn2 = null;
                try {
                    conn1 = app1.connect();
                    conn1.setAutoCommit(false);
                    conn1.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                    conn2 = app2.connect();
                    conn2.setAutoCommit(false);
                    // System.out.println("Transaction Isolation Level: " +
                    // conn1.getTransactionIsolation());
                } catch (SQLException e) {
                    printSQLException(e);
                }
                try {
                    CallableStatement bookTicket = conn1.prepareCall("{? = call bookTicket(?,?,?,?,?)}");
                    bookTicket.registerOutParameter(1, Types.VARCHAR);
                    bookTicket.setInt(2, trainID);
                    bookTicket.setDate(3, Date.valueOf(date));
                    bookTicket.setInt(4, numberofTickets);
                    bookTicket.setString(5, coachType);
                    Array array = conn1.createArrayOf("VARCHAR", passengerName);
                    bookTicket.setArray(6, array);
                    bookTicket.execute();
                    conn1.commit();
                    String PNR = bookTicket.getString(1);
                    String query = "select * from getTicket(" + PNR + ")";
                    PreparedStatement getTicket = conn2.prepareStatement(query);
                    ResultSet rs = null;
                    rs = getTicket.executeQuery();
                    conn2.commit();
                    while (rs.next()) {
                        System.out.println(rs.getString("coach"));
                    }
                    // getTicket.registerOutParameter(1, Types.VARCHAR);
                    // getTicket.registerOutParameter(2, Types.INTEGER);
                    // getTicket.registerOutParameter(3, Types.VARCHAR);
                    // getTicket.setString(4, PNR);
                    responseQuery = "PNR Number: " + PNR + "\t\t\t\t" + "Train Number :" + trainID + "\t\t\t\t"
                            + "Date of Journey:"
                            + Date.valueOf(date) + "\n" + "Passenger Name" + "\t\t\t\t" + "Coach" + "\t\t\t\t" + "Berth"
                            + "\t\t\t\t" + "Berth Type" + "\n";
                    for (int i = 0; i < numberofTickets; i++) {
                        responseQuery += passengerName[i];
                        responseQuery += "\t\t\t\t";
                        responseQuery += "\t\t\t\t";
                        responseQuery += "\n";
                    }
                    // ----------------------------------------------------------------
                    // Sending data back to the client
                    printWriter.println(responseQuery);
                    // System.out.println("\nSent results to client - "
                    // + socketConnection.getRemoteSocketAddress().toString() );
                    clientCommand = bufferedInput.readLine();
                } catch (SQLException e) {
                    if (Objects.equals(e.getSQLState(), "40001")) {
                        continue;
                    } else {
                        try {
                            // System.out.println("Transaction is being rolled back.");
                            conn1.rollback();
                            clientCommand = bufferedInput.readLine();
                            // printSQLException(e);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            // System.out.println("Shit2");
                        }
                    }
                } finally {
                    conn1.close();
                }
            }
            inputStream.close();
            bufferedInput.close();
            outputStream.close();
            bufferedOutput.close();
            printWriter.close();
            socketConnection.close();
        } catch (IOException | SQLException e) {
            return;
        }

    }
}

/**
 * Main Class to control the program flow
 */
public class ServiceModule {
    static int serverPort = 7008;
    static int numServerCores = 2;

    // ------------ Main----------------------
    public static void main(String[] args) throws IOException {

        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);

        // Creating a server socket to listen for clients
        ServerSocket serverSocket = new ServerSocket(serverPort); // need to close the port
        Socket socketConnection = null;

        // Always-ON server
        while (true) {
            System.out.println("Listening port : " + serverPort
                    + "\nWaiting for clients...");
            socketConnection = serverSocket.accept(); // Accept a connection from a client
            System.out.println("Accepted client :"
                    + socketConnection.getRemoteSocketAddress().toString()
                    + "\n");
            // Create a runnable task
            Runnable runnableTask = new QueryRunner(socketConnection);
            // Submit task for execution
            executorService.submit(runnableTask);
        }
    }

}
