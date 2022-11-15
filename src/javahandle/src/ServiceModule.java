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
    private final String password = "admin";

    /**
     * Connect to the PostgreSQL database
     *
     * @return a Connection object
     */
    public Connection connect() {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            System.out.println("Error: unable to load driver class!");
            System.exit(1);
        }
        try {
            conn = DriverManager.getConnection(url, user, password);

            if (conn == null) {
                System.out.println("Failed to make connection!");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return conn;
    }
}

class QueryRunner implements Runnable {
    //  Declare socket for client access
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
    public static String fixedLengthString(String string, int length) {
        return String.format("%1$-" + length + "s", string);
    }

    public void run() {

        try {
            //  Reading data from client
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

            boolean is_retry = false;

            JDBCPostgreSQLConnection app = new JDBCPostgreSQLConnection();
            Connection conn = null;
            try {
                conn = app.connect();
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
//                    System.out.println("Transaction Isolation Level: " + conn.getTransactionIsolation());
            } catch (SQLException e) {
                printSQLException(e);
            }

            CallableStatement bookTicket = conn.prepareCall("{? = call bookTicket(?,?,?,?,?)}");

            int numberofTickets = 0;
            String[] passengerName = null;
            String coachType = "";
            String date = "";
            int trainID = 0;
            String[] tokens;

            while (!clientCommand.equals("#")) {

                if (!is_retry) {
                    try {
                        tokens = clientCommand.split(" ");
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

                        bookTicket.registerOutParameter(1, Types.VARCHAR);
                        bookTicket.setInt(2, trainID);
                        bookTicket.setDate(3, Date.valueOf(date));
                        bookTicket.setInt(4, numberofTickets);
                        bookTicket.setString(5, coachType);
                        Array array = conn.createArrayOf("VARCHAR", passengerName);
                        bookTicket.setArray(6, array);

                    } catch (Exception ex) {
                        System.out.println("Ill Formatted Input");
                        ex.printStackTrace();
                    }
                }
                try {
                    bookTicket.execute();
                    conn.commit();
                    String bookingInfo = bookTicket.getString(1);
                    String[] tokensBookingInfo = bookingInfo.split("\\|");
                    String PNR = tokensBookingInfo[0];
                    responseQuery = "PNR Number: " + PNR + "\t\t\t\t" + "Train Number :" + trainID + "\t\t\t\t"
                                    + "Date of Journey:"
                                    + Date.valueOf(date) + "\n\t" + "Passenger Name" + "\t\t\t\t\t\t\t" + "Coach" + "\t\t\t\t" + "Berth"
                                    + "\t\t\t" + "Berth Type" + "\n\n";
                    for (int i = 0; i < numberofTickets; i++) {
                        responseQuery += fixedLengthString(passengerName[i], 28);
                        responseQuery += "\t\t\t\t";
                        responseQuery += coachType;
                        responseQuery += tokensBookingInfo[i * 3 + 1]; //Coach number
                        responseQuery += "\t\t\t\t";
                        responseQuery += tokensBookingInfo[i * 3 + 2];
                        responseQuery += "\t\t\t\t";
                        responseQuery += tokensBookingInfo[i * 3 + 3];
                        responseQuery += "\n";
                    }
                    responseQuery += "\n\n\n";
//            ----------------------------------------------------------------
//              Sending data back to the client
                    printWriter.println(responseQuery);
                    clientCommand = bufferedInput.readLine();
                } catch (SQLException e) {
                    if (Objects.equals(e.getSQLState(), "40001")) {
                        conn.rollback();
                        continue;
                    } else {
                        try {
//                            System.out.println("Transaction is being rolled back.");
                            conn.rollback();
                            clientCommand = bufferedInput.readLine();
//                            printSQLException(e);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            conn.close();
            inputStream.close();
            bufferedInput.close();
            outputStream.close();
            bufferedOutput.close();
            printWriter.close();
            socketConnection.close();
        } catch (IOException | RuntimeException | SQLException ex) {
            ex.printStackTrace();
        }

    }
}

/**
 * Main Class to control the program flow
 */
public class ServiceModule {
    static int serverPort = 7008;
    static int numServerCores = 150;

    //------------ Main----------------------
    public static void main(String[] args) throws IOException {


        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);

        //Creating a server socket to listen for clients
        ServerSocket serverSocket = new ServerSocket(serverPort); //need to close the port
        Socket socketConnection = null;

        // Always-ON server
        while (true) {
            System.out.println("Listening port : " + serverPort
                               + "\nWaiting for clients...");
            socketConnection = serverSocket.accept();   // Accept a connection from a client
            System.out.println("Accepted client :"
                               + socketConnection.getRemoteSocketAddress().toString()
                               + "\n");
            //  Create a runnable task
            Runnable runnableTask = new QueryRunner(socketConnection);
            //  Submit task for execution
            executorService.submit(runnableTask);
        }
    }

}


