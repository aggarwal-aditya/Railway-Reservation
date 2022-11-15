import java.io.File;
import java.io.FileNotFoundException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Scanner;

public class loadTrain {
    static public  void main(String[] args) throws FileNotFoundException, SQLException {
        String inputfile = "../Input/" + "Trainschedule.txt" ;
        File queries = new File(inputfile);
        Scanner queryScanner = new Scanner(queries);
        String query;
        JDBCPostgreSQLConnection app = new JDBCPostgreSQLConnection();
        Connection conn;
        conn = app.connect();
        while(queryScanner.hasNextLine()){
            query = queryScanner.nextLine();
            if(Objects.equals(query, "#")){
                break;
            }
            String[] tokens =query.split(" ");
            int trainID= Integer.parseInt(tokens[0]);
            String date=tokens[1];
            int numAC= Integer.parseInt(tokens[2]);
            int numSL= Integer.parseInt(tokens[3]);
            try{
                CallableStatement releaseTrain = conn.prepareCall("{call releaseTrain(?,?,?,?)}");
                releaseTrain.setInt(1, trainID);
                releaseTrain.setDate(2, Date.valueOf(date));
                releaseTrain.setInt(3, numAC);
                releaseTrain.setInt(4, numSL);
                releaseTrain.execute();
                System.out.println("Train Released");
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        conn.close();
    }
}
