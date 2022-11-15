import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Objects;



public class clientGUI {
    public static void main(String args[]) throws IOException {
        int sockPort = 7008;
        Socket socketConnection = new Socket("localhost", sockPort);
        DataOutputStream outStream = new DataOutputStream(socketConnection.getOutputStream());
        BufferedReader inStream=new BufferedReader(new InputStreamReader(socketConnection.getInputStream()));
        BufferedReader keyStream = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Hi Welcome to Railway Reservation System\n\n");
        String str;
        while (true) {
            System.out.println("\n1. Press 1 to release train\n2. Press 2 to get your ticket" +
                            "\n3. Press 3 to book a ticket" +
                    "\nType exit to quit");
            str = keyStream.readLine();
            outStream.writeBytes(str + "\n");
            System.out.printf("%s",str);
            if (Objects.equals(str, "1")) {
                System.out.println("\nPlease Enter the Train Number:");
                str = keyStream.readLine();
                outStream.writeBytes(str + "\n");
                System.out.println("\nPlease Enter date of train(yyyy-mm-dd)");
                str = keyStream.readLine();
                outStream.writeBytes(str + "\n");
                System.out.println("\nPlease enter number of AC Coach");
                str = keyStream.readLine();
                outStream.writeBytes(str + "\n");
                System.out.println("\nPlease enter number of Sleeper Coach");
                str = keyStream.readLine();
                outStream.writeBytes(str + "\n");
                String readStr="";
                while(!Objects.equals(readStr = inStream.readLine(), "#")) {
                    if(!Objects.equals(readStr, "#")) {
                        System.out.println(readStr);
                    }
                }

            }
            else if(Objects.equals(str, "2")){
                System.out.println("\nPlease Enter Your PNR Number:");
                str = keyStream.readLine();
                outStream.writeBytes(str + "\n");
                StringBuilder ticket= new StringBuilder();
                String readStr="";
                while(!Objects.equals(readStr = inStream.readLine(), "#")) {
                    ticket.append(readStr);
                    ticket.append(System.getProperty("line.separator"));
                }
                System.out.println(ticket);
            } else if (Objects.equals(str,"3")) {
                int seats;
                System.out.println("\nPlease Enter train Number");
                str = keyStream.readLine();
                outStream.writeBytes(str + "\n");
                System.out.println("\nPlease Enter train date(yyyy-mm-dd)");
                str = keyStream.readLine();
                outStream.writeBytes(str + "\n");
                System.out.println("\nPlease Enter Number of seats");
                str = keyStream.readLine();
                seats= Integer.parseInt(str);
                outStream.writeBytes(str + "\n");
                System.out.println("\nPlease Enter Coach Type (AC/SL)");
                str = keyStream.readLine();
                outStream.writeBytes(str + "\n");
                System.out.println("\nPlease Enter name of  passengers (each in a new line)");
                for(int i=0;i<seats;i++) {
                    str = keyStream.readLine();
                    outStream.writeBytes(str + "\n");
                }
                StringBuilder ticket= new StringBuilder();
                String readStr="";
                while(!Objects.equals(readStr = inStream.readLine(), "#")) {
                    ticket.append(readStr);
                    ticket.append(System.getProperty("line.separator"));
                }
                System.out.println(ticket);
            } else if (Objects.equals(str,"exit")) {
                break;
            }
            else{
                System.out.println("Invalid Input");
            }

        }

    }
}
