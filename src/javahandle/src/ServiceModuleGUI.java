import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class ServicereleaseQuery implements Runnable{
    int trainID;
    String day;
    int numAC;
    int numSL;
    PrintWriter ostream;
    public ServicereleaseQuery(int arg1,String arg2,int arg3,int arg4,PrintWriter arg5)            // constructor to get arguments from the main thread
     {
        trainID=arg1;
        day=arg2;
        numAC=arg3;
        numSL=arg4;
        ostream=arg5;
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
        return String.format("%1$-"+length+ "s", string);
    }

    public void run() {
        try {
            JDBCPostgreSQLConnection app = new JDBCPostgreSQLConnection();
            Connection conn;
            conn = app.connect();
            try {
                CallableStatement releaseTrain = conn.prepareCall("{call releaseTrain(?,?,?,?)}");
                releaseTrain.setInt(1, trainID);
                releaseTrain.setDate(2, Date.valueOf(day));
                releaseTrain.setInt(3, numAC);
                releaseTrain.setInt(4, numSL);
                releaseTrain.execute();
                ostream.println("Train Released\n");
                ostream.println("#\n");
                conn.close();
            } catch (SQLException e) {
                try {
                    ostream.println("Could Not release Train\n");
                    ostream.println("#\n");
                    conn.close();
                } catch (SQLException ex) {
                    ostream.println("Could Not release Train\n");
                    ostream.println("#\n");
                    printSQLException(ex);
                }
                printSQLException(e);
            }
        }catch (Exception |AssertionError e){
            ostream.println("Could Not release Train\n");
            ostream.println("#\n");
        }
    }
}

class ServicefindQuery implements Runnable{

    String PNR;
    PrintStream out;
    PrintWriter ostream=null;
    public ServicefindQuery(String arg1,PrintWriter arg2) throws IOException {
            PNR=arg1;
            ostream=arg2;
    }
//    PrintStream outStream=new PrintStream(socketConnection.getOutputStream());
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
        return String.format("%1$-"+length+ "s", string);
    }

    public void run() {
        JDBCPostgreSQLConnection app = new JDBCPostgreSQLConnection();
        Connection conn = null;
        conn = app.connect();
        try {
        String[]tokens=PNR.split("_");
        assert (tokens.length>=5);
        int trainID= Integer.parseInt(tokens[0]);
        String coachType="";
        String date=tokens[1];
        if(Objects.equals(tokens[2], "1"))
            coachType="AC";
        else
            coachType="SL";


        PreparedStatement getTicket=conn.prepareStatement("select * from getTicket(?,?,?,?)");
            getTicket.setString(1,PNR);
            getTicket.setDate(3, Date.valueOf(date));
            getTicket.setInt(2,trainID);
            getTicket.setString(4,coachType);
            ResultSet results=getTicket.executeQuery();
            Array passengers = null;
            String bookingInfo = null;
            while (results.next()){
                passengers=results.getArray(1);
                bookingInfo=results.getString(2);
                break;
            }
            conn.close();
            assert passengers != null;
            String[] passengerName = (String[])passengers.getArray();
            assert bookingInfo != null;
            String[] tokensBookingInfo =bookingInfo.split("\\|");
            int numberofTickets=passengerName.length;
            StringBuilder responseQuery= new StringBuilder();
            String PNR=tokensBookingInfo[0];
            responseQuery = new StringBuilder("PNR Number: " + PNR + "\t\t\t\t" + "Train Number :" + trainID + "\t\t\t\t"
                    + "Date of Journey:"
                    + Date.valueOf(date) + "\n" + fixedLengthString("Passenger Name",28) + "\t\t\t\t" + "Coach" + "\t\t\t\t" + "Berth"
                    + "\t\t\t" + "Berth Type" + "\n\n");
            for (int i = 0; i < numberofTickets; i++) {
                responseQuery.append(fixedLengthString(passengerName[i], 28));
                responseQuery.append("\t\t\t\t");
                responseQuery.append(coachType);
                responseQuery.append(tokensBookingInfo[i * 3 + 1]);//Coach number
                responseQuery.append("\t\t\t\t\t");
                responseQuery.append(tokensBookingInfo[i * 3 + 2]);
                responseQuery.append("\t\t\t\t");
                responseQuery.append(tokensBookingInfo[i * 3 + 3]);
                responseQuery.append("\n");
            }
            responseQuery.append("\n\n\n");
            responseQuery.append("#\n");
            ostream.println(responseQuery);
            System.out.println(responseQuery);
        } catch (Exception|AssertionError e) {
            ostream.println("Could Not Find Your Ticket\n");
            ostream.println("#\n");
            try {
                conn.close();
            } catch (SQLException ex) {
                printSQLException(ex);
            }
        }
    }
}

class ServiceBookQuery implements Runnable{

    int trainID;
    String date;
    int numSeat;
    String coachType;
    String[]passengerName;

    PrintWriter ostream;
    public ServiceBookQuery(int arg1,String arg2,int arg3,String arg4,String[]arg5,PrintWriter arg6){
        trainID=arg1;
        date=arg2;
        numSeat=arg3;
        coachType=arg4;
        passengerName=arg5;
        ostream=arg6;
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
        return String.format("%1$-"+length+ "s", string);
    }

    @Override
    public void run() {
        try{
        JDBCPostgreSQLConnection app = new JDBCPostgreSQLConnection();
        Connection conn = null;
        conn = app.connect();
        StringBuilder responseQuery = new StringBuilder();
        try {
            CallableStatement bookTicket = conn.prepareCall("{? = call bookTicket(?,?,?,?,?)}");
            bookTicket.registerOutParameter(1, Types.VARCHAR);
            bookTicket.setInt(2, trainID);
            bookTicket.setDate(3, Date.valueOf(date));
            bookTicket.setInt(4, numSeat);
            bookTicket.setString(5, coachType);
            Array array = conn.createArrayOf("VARCHAR", passengerName);
            bookTicket.setArray(6, array);
            bookTicket.execute();
            String bookingInfo = bookTicket.getString(1);
            String[] tokensBookingInfo = bookingInfo.split("\\|");
            String PNR = tokensBookingInfo[0];
            responseQuery = new StringBuilder("PNR Number: " + PNR + "\t\t\t\t" + "Train Number :" + trainID + "\t\t\t\t"
                    + "Date of Journey:"
                    + Date.valueOf(date) + "\n" + fixedLengthString("Passenger Name",28) + "\t\t\t\t" + "Coach" + "\t\t\t\t" + "Berth"
                    + "\t\t\t" + "Berth Type" + "\n\n");
            for (int i = 0; i < numSeat; i++) {
                responseQuery.append(fixedLengthString(passengerName[i], 28));
                responseQuery.append("\t\t\t\t");
                responseQuery.append(coachType);
                responseQuery.append(tokensBookingInfo[i * 3 + 1]);//Coach number
                responseQuery.append("\t\t\t\t");
                responseQuery.append(tokensBookingInfo[i * 3 + 2]);
                responseQuery.append("\t\t\t\t");
                responseQuery.append(tokensBookingInfo[i * 3 + 3]);
                responseQuery.append("\n");
            }
            responseQuery.append("\n\n\n");
            System.out.println(responseQuery);
            ostream.println(responseQuery);
            ostream.println("#\n");

        } catch (SQLException e) {
            ostream.println("\nCould Not Book Ticket\n");
            ostream.println("#\n");
            printSQLException(e);
        }
    } catch (Exception| AssertionError e) {
            ostream.println("\nCould Not Book Ticket\n");
            ostream.println("#\n");
        }

    }
}


public class ServiceModuleGUI {
    static int serverPort = 7008;
    static int numServerCores = 1;
    //------------ Main----------------------
    public static void main(String[] args) throws IOException {

        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);
        ServerSocket serverSocket = new ServerSocket(serverPort); //need to close the port
        Socket socketConnection = null;

        // Always-ON server
        //1-Release Train 2- Book Ticket 3-Get Ticket
        System.out.println("Listening port : " + serverPort
                + "\nWaiting for clients...");
        socketConnection = serverSocket.accept();   // Accept a connection from a client
        System.out.println("Accepted client :"
                + socketConnection.getRemoteSocketAddress().toString()
                + "\n");
        BufferedReader inStream=new BufferedReader(new InputStreamReader(socketConnection.getInputStream()));
        OutputStreamWriter outStream=new OutputStreamWriter(socketConnection
                .getOutputStream());
        BufferedWriter bufferedOutput = new BufferedWriter(outStream);
        PrintWriter printWriter = new PrintWriter(bufferedOutput, true);
        while (true) {
            String str;
            while((str=inStream.readLine())!=null){
                if(Objects.equals(str, "1")){
//                    release train
                    int trainID= Integer.parseInt(inStream.readLine());
                    String date=inStream.readLine();
                    int numberAC= Integer.parseInt(inStream.readLine());
                    int numberSL= Integer.parseInt(inStream.readLine());
                    Runnable runnableTask=new ServicereleaseQuery(trainID,date,numberAC,numberSL,printWriter);
                    executorService.submit(runnableTask);
                    break;
                }
                else if(Objects.equals(str,"2")){
//                    find ticket
                    String PNR=inStream.readLine();
                    Runnable runnableTask=new ServicefindQuery(PNR,printWriter);
                    executorService.submit(runnableTask);
                    printWriter.flush();
                    break;

                } else if (Objects.equals(str,"3")) {
                    int trainID= Integer.parseInt(inStream.readLine());
                    String date=inStream.readLine();
                    int seats=Integer.parseInt(inStream.readLine());
                    String coachType=inStream.readLine();
                    String[]passengerNames = new String[seats];
                    for(int i=0;i<seats;i++){
                        passengerNames[i]=inStream.readLine();
                    }
                    Runnable runnableTask=new ServiceBookQuery(trainID,date,seats,coachType,passengerNames,printWriter);
                    executorService.submit(runnableTask);
                }
            }
//            PrintStream outStream=new PrintStream(socketConnection.getOutputStream());
        }



    }
}
