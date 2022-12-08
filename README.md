# Railway Reservation Portal

The Railway Reservation Portal has been designed to handle a lage number of requests for ticket bookings for the available trains simultaneously using Multithreading.

---

Steps to Follow to run the program for large input using multithreading:

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

Steps to Follow to run the program interactively:

1. Download and extract the entire source code in your PC.
2. Load the entire init.sql file in the PSQL Terminal.
3. Change the username and password in the ServiceModule.java file to the credentials of your PSQL account.
4. Open the directory src\javahandle\src in the terminal and run the following commands:

   ```
   javac -cp ".:postgresql-42.5.0.jar" *.java
   java -cp ".:postgresql-42.5.0.jar" ServiceModuleGUI
   ```
5. Open another terminal in the same directory and run the following command:

   ```
   java -cp ".:postgresql-42.5.0.jar" clientGUI
   ```
6. Now you will have 3 options:

   a. Add train (Enter 1)

   b. Find Ticket (Enter 2)

   c. Book Seat (Enter 3)

---
