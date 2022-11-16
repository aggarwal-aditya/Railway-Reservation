# Railway-Reservation Portal

Steps to Follow to run the program :

1. Download and extract the entire source code in your PC.
2. Load the entire init.sql file in the PSQL Terminal.
3. Change the username and password in the ServiceModule.java file to the credentials of your PSQL account.
4. Open the directory src\javahandle\src in the terminal and run the following commands:
  
    A. javac -cp ".:postgresql-42.5.0.jar" *.java
    
    B. java -cp ".:postgresql-42.5.0.jar" ServiceModule

5. Open another terminal in the same directory and run the following command:

    java -cp ".:postgresql-42.5.0.jar" client

6.
