import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.StringTokenizer;
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
        }
        catch(ClassNotFoundException ex) {
            System.out.println("Error: unable to load driver class!");
            System.exit(1);
        }
        try {
            conn = DriverManager.getConnection(url, user, password);

            if (conn != null) {
                System.out.println("Connected to the PostgreSQL server successfully.");
            } else {
                System.out.println("Failed to make connection!");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return conn;
    }
}

class QueryRunner implements Runnable
{
    //  Declare socket for client access
    protected Socket socketConnection;

    public QueryRunner(Socket clientSocket)
    {
        this.socketConnection =  clientSocket;
    }

    public void run()
    {
        JDBCPostgreSQLConnection app = new JDBCPostgreSQLConnection();
        Connection conn=null;
        try {
            conn= app.connect();
//                System.out.println("Transaction Isolation Level: " + conn.getTransactionIsolation());
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            // Get the transaction isolation again
//                System.out.println("Transaction Isolation Level: "+ conn.getTransactionIsolation());
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        try
        {
            //  Reading data from client
            InputStreamReader inputStream = new InputStreamReader(socketConnection
                    .getInputStream()) ;
            BufferedReader bufferedInput = new BufferedReader(inputStream) ;
            OutputStreamWriter outputStream = new OutputStreamWriter(socketConnection
                    .getOutputStream()) ;
            BufferedWriter bufferedOutput = new BufferedWriter(outputStream) ;
            PrintWriter printWriter = new PrintWriter(bufferedOutput, true) ;

            String clientCommand = "" ;
            String responseQuery = "" ;
            String queryInput = "" ;

            while(true)
            {
                // Read client query
                clientCommand = bufferedInput.readLine();
//                 System.out.println("Received data <" + clientCommand + "> from client : "
//                                     + socketConnection.getRemoteSocketAddress().toString());

                //  Tokenize here
                StringTokenizer tokenizer = new StringTokenizer(clientCommand);
                queryInput = tokenizer.nextToken();

                if(queryInput.equals("Finish"))
                {
//                    String returnMsg = "Connection Terminated - client : "
//                            + socketConnection.getRemoteSocketAddress().toString();
//                    System.out.println(returnMsg);
                    inputStream.close();
                    bufferedInput.close();
                    outputStream.close();
                    bufferedOutput.close();
                    printWriter.close();
                    socketConnection.close();
                    return;
                }
                try {
                    Statement st = conn.createStatement();
                    ResultSet rs;
                    CallableStatement bookTicket = conn.prepareCall("{? = call bookTicket(?,?,?,?,?,?,?) }");
                    bookTicket.registerOutParameter(1,Types.BIGINT);
                    rs = st.executeQuery("SELECT * FROM trains");
                    while (rs.next()) {
                        System.out.print("Column 1 returned\n");
                        printWriter.println(rs);
                    }
                    rs.close();
                    st.close();
                    responseQuery = "******* Dummy result ******";
                }
                catch (SQLException e) {
                    System.out.print(e.getMessage());
                }


                //----------------------------------------------------------------
                //  Sending data back to the client
                printWriter.println(responseQuery);
                // System.out.println("\nSent results to client - "
                //                     + socketConnection.getRemoteSocketAddress().toString() );

            }
        }
        catch(IOException e)
        {
            return;
        }

    }
}

/**
 * Main Class to control the program flow
 */
public class ServiceModule
{
    static int serverPort = 7005;
    static int numServerCores = 2 ;
    //------------ Main----------------------
    public static void main(String[] args) throws IOException
    {


        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);

        //Creating a server socket to listen for clients
        ServerSocket serverSocket = new ServerSocket(serverPort); //need to close the port
        Socket socketConnection = null;

        // Always-ON server
        while(true){
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
