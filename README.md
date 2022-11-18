# Railway Reservation Portal

Steps to Follow to run the program for large input using multithreading :

1. Download and extract the entire source code in your PC.
2. Load the entire init.sql file in the PSQL Terminal.
3. Change the username and password in the ServiceModule.java file to the credentials of your PSQL account.
4. Open the directory src\javahandle\src in the terminal and run the following commands:

   ```
   javac -cp ".:postgresql-42.5.0.jar" *.java
   java -cp ".:postgresql-42.5.0.jar" ServiceModule
   ```
5. Open another terminal in the same directory and run the following command:

   ```
   java -cp ".:postgresql-42.5.0.jar" client
   ```

---

Steps to Follow to run the program interactivel :

1. Download and extract the entire source code in your PC.
2. Load the entire init.sql file in the PSQL Terminal.
3. Change the username and password in the ServiceModule.java file to the credentials of your PSQL account.
4. Open the directory src\javahandle\src in the terminal and run the following commands:

   ```
   javac -cp ".:postgresql-42.5.0.jar" *.java
   java -cp ".:postgresql-42.5.0.jar" ServiceModule
   ```
5. Open another terminal in the same directory and run the following command:

   ```
   java -cp ".:postgresql-42.5.0.jar" clientGUI
   ```

---

The Railway Reservation Portal has been designed to handle a lage number of requests for ticket bookings for the available trains simultaneously using Multithreading.
