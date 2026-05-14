import java.sql.ResultSet; // Keep import for now, might be needed for casting Map values
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Date; // Keep import for casting Map values
import java.nio.charset.StandardCharsets; // Add missing import

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import java.io.*;
import java.math.BigDecimal; // Import BigDecimal for Oracle NUMBER type
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Import Map

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class Client {
    // Datetime
    private static Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
    private static LocalDate currentDate = LocalDate.now();
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yy");
    private static String formattedDate = currentDate.format(formatter);

    // Hash
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 128; // in bits

    // TOTP
    private static String TOTP_secret = "COMP3334";
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int GCM_IV_LENGTH = 12; // 96 bits

    public static void main(String[] args)
            throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
        // System Value
        Console console = System.console();
        String acc_name;
        String pwd;
        int cmd = -1;
        boolean sysLoop = true;
        String rsStr="";
        boolean rsBool=false;

        // DB connection related variables removed from client main logic
        String sql = "";
        // ResultSet rset; // Replace ResultSet with List<Map<String, Object>>
        List<Map<String, Object>> resultList; // Use List<Map>

        // account value
        Account acc = new Account();

        // Remove direct Server instance creation
        // Server ser = new Server();

        // Testing TOTP
        // TOTPGenerator totp = new TOTPGenerator(); // Use static methods

        clearScreen();

        // ---Start of the loing and register---
        while (!(cmd >= 0 && cmd <= 2)) {
            try {

                clearScreen();
                topView();
                System.out.println("=================================================================");
                System.out.println("** Command List **");
                System.out.print("[0]:Exit    \n[1]:Login Comp3334_CSS    \n[2]:Register Account \n\nEnter CMD:");
                cmd = Integer.parseInt(console.readLine());
                // 0 = exit
                if (cmd == 0) {
                    sysLoop = false;
                    // 1 = login
                } else if (cmd == 1) {
                    System.out.print("\nEnter your account:");
                    acc_name = console.readLine();
                    while (!isValidString(acc_name)) {
                        System.out.print(
                                "\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter your account:");
                        acc_name = console.readLine();
                    }

                    System.out.print("Enter your password:");
                    pwd = console.readLine();
                    while (!isValidString(pwd)) {
                        System.out.print(
                                "\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter your password:");
                        pwd = console.readLine();
                    }


                    if (acc_name.contains("@")) {
                        sql = "SELECT password FROM Comp3334_CSSAccount WHERE email = ?";
                        rsStr="";
                        // Replace direct server call with ClientService
                        rsStr = ClientService.getHashPWD(sql, acc_name);
                    } else {
                        sql = "SELECT password FROM Comp3334_CSSAccount WHERE loginName = ? ";
                        rsStr="";
                        // Replace direct server call with ClientService
                        rsStr = ClientService.getHashPWD(sql, acc_name);
                    }
                    boolean cc = false;
                    try {
                        if (!rsStr.isEmpty()) {
                            String storedhash = rsStr;
                            cc = verifyPassword(pwd, storedhash);
                            // if verify, set pwd = hash and store TOTP_secret = "COMP3334" + Upper Cased &
                            // validChars of hash
                            if (cc) {
                                pwd = storedhash;

                                String[] parts = storedhash.split(":");
                                // System.out.println("\nT:"+TOTP_secret+":PK"); //COMP3334
                                TOTP_secret += toTOTPKEY(parts[1]); // client TOTP key = individual SK
                                // System.out.println("\nT:"+TOTP_secret+":SK"); //COMP3334 + hash
                            }
                        } else {
                            System.out.println("acccount_Name/email is incorrect... ");
                            System.out.println("Please press <enter> to continue\n\n");
                            console.readLine();
                            clearScreen();
                            cmd = -1;
                            continue; // Go back to the start of the loop
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Reset resultList before use
                    resultList = new ArrayList<>();
                    if (cc) { // Only proceed if password verification was successful
                        if (acc_name.contains("@")) {
                            sql = "SELECT accNo,loginName,email,phoneNo,userStatus,userRight FROM Comp3334_CSSAccount WHERE email = ? AND password = ? ";
                            // Replace direct server call with ClientService
                            resultList = ClientService.getAcc(sql, acc_name, pwd); // Get List<Map>

                        } else {
                            sql = "SELECT accNo,loginName,email,phoneNo,userStatus,userRight FROM Comp3334_CSSAccount WHERE loginName = ? AND password = ? ";
                            // Replace direct server call with ClientService
                            resultList = ClientService.getAcc(sql, acc_name, pwd); // Get List<Map>
                        }
                    }
                    // System.out.println(acc_name+":"+pwd);

                    // Check if the list is not empty and contains exactly one account
                    if (resultList != null && resultList.size() == 1) {
                        Map<String, Object> accountData = resultList.get(0); // Get the first (and only) map

                        // Extract data using keys (assuming uppercase from Oracle) and casting
                        // Use getIntFromMap helper for numeric types
                        int accNo = getIntFromMap(accountData, "ACCNO");
                        String loginName = (String) accountData.get("LOGINNAME");
                        String email = (String) accountData.get("EMAIL");
                        int phoneNo = getIntFromMap(accountData, "PHONENO");
                        int userStatus = getIntFromMap(accountData, "USERSTATUS");
                        int userRight = getIntFromMap(accountData, "USERRIGHT");


                        String totpString = TOTPGenerator.generateTOTP(TOTP_secret); // Fix: Access static method directly
                        storeTOTP(totpString, "phone.txt");
                        System.out.print("\nEnter your TOTP:");
                        totpString = console.readLine();
                        while (totpString.isEmpty()) {
                            System.out.print("\nEnter your TOTP:");
                            totpString = console.readLine();
                        }
                        // if(totp.verifyTOTP(TOTP_secret, totpString, 1)){
                        if (TOTPGenerator.verifyTOTP(TOTP_secret, totpString, 1)) { // Fix: Access static method
                                                                                    // directly
                            System.out.flush();
                            acc.setAccNo(accNo);
                            acc.setLoginName(loginName);
                            acc.setEmail(email);
                            acc.setPhoneNo(phoneNo);
                            acc.setUserStatus(userStatus);
                            acc.setUserRight(userRight);
                            System.out.println("login successful");

                            // Save log into db
                            sql = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , 'LOGIN - User logged in' , ? )";

                            // No ResultSet to close here
                            ClientService.setLogVoid(sql, acc.getAccNo(), formattedDate);

                            System.out.println("Please press <enter> to continue\n\n");
                            console.readLine();
                            clearScreen();

                            // Inside the login section of Client.java
                            try (Socket socket = new Socket("localhost", 12345);
                                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {

                                output.writeObject("LOGIN"); // Send command
                                output.writeObject(currentTimestamp + "");
                                output.writeObject(acc.getAccNo());
                                output.writeObject(acc.getLoginName());
                                output.writeObject(acc.getEmail());
                                output.writeObject(pwd); // Send the hashed password used for login
                                output.writeObject(acc.getPhoneNo());
                                output.writeObject(acc.getUserStatus());
                                output.writeObject(acc.getUserRight());

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // Login - Fail with TOTP but pass the hash
                            // Save log into db
                            if (userStatus == 0) { // Use userStatus extracted from map
                                sql = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , 'LOGIN - Fail with TOTP (Curr:Activate Account)' , ? )";
                            } else {
                                sql = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , 'LOGIN - Fail with TOTP (Curr:Locked Account)' , ? )";
                            }

                            ClientService.setLogVoid(sql, accNo, formattedDate); // Use accNo extracted from map

                            // Inside the LOGIN_Fail section of Client.java
                            try (Socket socket = new Socket("localhost", 12345);
                                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {

                                output.writeObject("LOGIN_Fail"); // Send command
                                output.writeObject(currentTimestamp + "");
                                output.writeObject(accNo); // Use extracted data
                                output.writeObject(loginName);
                                output.writeObject(email);
                                output.writeObject(pwd); // Send the hashed password used for login attempt
                                output.writeObject(phoneNo);
                                output.writeObject(userStatus);
                                output.writeObject(userRight);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            System.out.println("\nTOTP is expired / invaild \n");
                            System.out.println("Please press <enter> to continue\n\n");
                            console.readLine();
                            clearScreen();
                            cmd = -1;
                        }

                    } else {
                        // This handles cases where:
                        // 1. Password verification failed (cc is false, resultList is empty)
                        // 2. Password was correct, but getAcc returned no rows (resultList is empty)
                        // 3. Password was correct, but getAcc returned multiple rows (resultList.size() > 1 - data inconsistency)
                        System.out.println("acccount_Name/email or password is incorrect... ");
                        System.out.println("Please press <enter> to continue\n\n");
                        console.readLine();
                        clearScreen();
                        cmd = -1;
                    }

                    // 2 = register
                } else if (cmd == 2) {
                    // Account info
                    // -> Check input format is it valid
                    // -> check is it registered
                    // -> if all valid, hash pwd & regisiter account
                    
                    // accName, loginName
                    System.out.print("Enter your account_Name:");
                    String nc_name = console.readLine();

                    while (!isValidString(nc_name) || (nc_name == null || nc_name.isEmpty())
                            || ((nc_name.length() > 20 || nc_name.length() < 1)
                                    || Character.isDigit(nc_name.charAt(0)))) {
                        System.out.print(
                                "\nUser_name can't start with digit & lenght must be 1 to 20\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter your account_Name:");
                        nc_name = console.readLine();
                    }

                    // accPws, password
                    System.out.print("Enter your password:");
                    String nc_pwd = console.readLine();

                    while (!isValidString(nc_pwd) || (nc_name == null || nc_name.isEmpty())
                            || (nc_pwd.length() > 15 || nc_pwd.length() < 1)) {
                        System.out.print(
                                "\nPassword length must be 1-15\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter your password:");
                        nc_pwd = console.readLine();
                    }
                    // pwd = hash pwd, which including 2 part, 1:salt 2:hashed pwd
                    nc_pwd = hashPassword(nc_pwd);
                    // System.out.print("\nhash:"+nc_pwd+"\n");

                    // accEmail, email
                    System.out.print("Enter your email:");
                    String nc_email = console.readLine();

                    while (!isValidString(nc_email) || (nc_name == null || nc_name.isEmpty())
                            || !(nc_email.contains("@"))) {
                        System.out.print(
                                "\nEmail must be contains '@' \nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter your email:");
                        nc_email = console.readLine();
                    }

                    // accPhone, phoneNo
                    System.out.print("Enter your phoneNo:");
                    String str_phoneNo = console.readLine();

                    while (!isValidNumber(str_phoneNo) || (nc_name == null || nc_name.isEmpty())
                            || !(str_phoneNo.length() == 8)) {
                        System.out.print(
                                "\nPhoneNo must be 8 number, e.g, 28879643\nOnly Numner <0-9> \nEnter your phoneNo:");
                        str_phoneNo = console.readLine();
                    }

                    int nc_phoneNo = Integer.parseInt(str_phoneNo);

                    // checking email is it registered
                    sql = "SELECT accNo FROM Comp3334_CSSAccount WHERE email = ?";
                    rsBool = false;
                    // Replace direct server calls with ClientService
                    rsBool = ClientService.registeredEmail(sql, nc_email); // This doesn't need change

                    boolean cc = true;
                    if (rsBool) {
                        System.out.print("\nThis email has been registered");
                        cc = false;
                    }

                    // checking loginName is it registered
                    // Note: ClientService doesn't have a direct method for checking loginName,
                    // but the server method registeredLoginName exists. We'll use registeredEmail logic.
                    // If a dedicated check is needed, ClientService and Server need updates.
                    sql = "SELECT accNo FROM Comp3334_CSSAccount WHERE loginName = ?";
                    rsBool = false;
                    // Using registeredEmail logic for loginName check (assumes similar check needed)
                    rsBool = ClientService.registeredEmail(sql, nc_name); // Check login name

                    if (rsBool) {
                        System.out.print("\nThis account_Name has been registered");
                        cc = false;
                    }

                    // if valid, INSERT DATA
                    // -> if all valid, hash pwd & regisiter account
                    if (cc) {
                        sql = "INSERT INTO Comp3334_CSSAccount VALUES (null, ?, ?, ?, ?,0,0) ";
                        ClientService.regisiterAccount(sql, nc_name, nc_pwd, nc_email, nc_phoneNo);

                        System.out.println("\nRegister Account Successfully");
                        // Inside the REGISTER section of Client.java
                        try {
                            boolean success = ClientService.regUser(
                                currentTimestamp + "",
                                nc_name,
                                nc_pwd,
                                nc_email,
                                nc_phoneNo
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                                


                    } else {
                        System.out.println("\nRegister Account Unsuccessfully");
                    }

                    System.out.println("Please Enter <enter> to next...");
                    console.readLine();
                    clearScreen();
                    cmd = -1;
                }
            } catch (NumberFormatException e) { // Catch specific exception
                 System.out.println("Invalid input! Please enter a valid number for CMD.");
                 cmd = -1; // Reset cmd to retry
                 System.out.println("Please press <enter> to continue...");
                 console.readLine(); // Pause
            } catch (Exception e) {
                e.printStackTrace();
                System.out.print("System: An unexpected error occurred. Please try again.");
                cmd = -1; // Reset cmd to retry
                System.out.println("Please press <enter> to continue...");
                console.readLine(); // Pause
            }
        }

        // --- End of the loing and register ---

        // --- Start of the main application loop ---
        // If UserStatus == 1 , account locked
        if (acc.getUserStatus() == 1) {
            topView();
            displayAccData(acc);
            System.out.print(
                    "\nOpss! you account has be locked! \nPlease contact with 'COMP3334@connect.polyu.hk' or call '56635210' to get more details, Bye! \n");
            System.out.println("Please press <enter> to continue\n\n");
            console.readLine();
            sysLoop = false; // Ensure loop terminates if account is locked
        }

        // If UserStatus == 0, login
        while (acc.getUserStatus() != 1 && sysLoop) {
            // UserRight == 0 , normal user, UserRight == 1, admin
            if (acc.getUserRight() == 0 || acc.getUserRight() == 1) {
                clearScreen();
                cssView(acc);

                int icount = 1;
                String icount_str = "";
                // store the fileno in list --> (index)list
                List<Integer> fileno = new ArrayList<>();
                List<String> filenames = new ArrayList<>(); // Store filenames for display/download

                // --Start of displaying the owner files--
                sql = "SELECT a.fileNo, a.fileName, owner.loginName AS ownerName, " +
                        "LISTAGG(shared.loginName, ', ') WITHIN GROUP (ORDER BY shared.loginName) AS shareName " +
                        "FROM Comp3334_AccFile a " +
                        "JOIN Comp3334_CSSAccount owner ON a.accNo = owner.accNo " +
                        "LEFT JOIN Comp3334_ShareFile s ON a.fileNo = s.fileNo " +
                        "LEFT JOIN Comp3334_CSSAccount shared ON s.accNo = shared.accNo " +
                        "WHERE a.accNo = ? " +
                        "GROUP BY a.fileNo, a.fileName, owner.loginName";

                resultList = ClientService.selectFile(sql, acc.getAccNo()); // Get List<Map>

                System.out.println("\n\n** Files List: **");
                String testmsg = "-Empty list-";
                icount = 1;

                if(resultList != null && !resultList.isEmpty()){
                    testmsg = ""; // Reset testmsg if list is not empty
                    for (Map<String, Object> row : resultList) {
                        // Use helper method for safe integer retrieval
                        int fileNo = getIntFromMap(row, "FILENO");
                        String fileName = (String) row.get("FILENAME");
                        String ownerName = (String) row.get("OWNERNAME");
                        String shareName = (String) row.get("SHARENAME"); // LISTAGG returns String

                        fileno.add(fileNo);
                        filenames.add(fileName); // Add filename

                        // Handle null shareName (no shared accounts)
                        if (shareName == null) {
                            shareName = "";
                        }

                        System.out.printf("\n[Index:%s] [Owner:%s] [File:%s] [ShareTo:%s]",
                                (icount <= 9 ? "0" + icount++ : "" + icount++),
                                ownerName,
                                fileName,
                                shareName);
                    }
                }


                // --end of displaying the owner files--

                // --start of displaying the share file by other user--
                sql = "SELECT a.fileNo, c.loginName, c.email, a.fileName " +
                        "FROM Comp3334_AccFile a " +
                        "JOIN Comp3334_ShareFile s ON a.fileNo = s.fileNo " +
                        "JOIN Comp3334_CSSAccount c ON a.accNo = c.accNo " +
                        "WHERE s.accNo = ?";

                // rset.close(); // No ResultSet to close
                resultList = ClientService.selectWithSharedFile(sql, acc.getAccNo()); // Get List<Map>

                if(resultList != null && !resultList.isEmpty()){
                     if (testmsg.contains("-Empty list-")) testmsg = ""; // Reset testmsg if list was previously empty
                     for (Map<String, Object> row : resultList) {
                        fileno.add(getIntFromMap(row, "FILENO"));
                        filenames.add((String) row.get("FILENAME")); // Add filename

                        // Format the index string before using it
                        String indexStr = (icount <= 9) ? "0" + icount : "" + icount;
                        icount++; // Increment after using
                        
                        System.out.printf("\n[Index:%s] [Owner:%s] [File:%s]",
                                indexStr,
                                (String) row.get("LOGINNAME"), // Owner's login name
                                (String) row.get("FILENAME")); // File name
                    }
                }
                // No ResultSet to close

                // // display the fileList data, debuging
                // if (!fileno.isEmpty()) {
                // testmsg += "fileList:" + fileno;
                // testmsg += ", fileList[0]:" + fileno.get(0);
                // }

                // Load the encryption key
                String keyFilePath = "encryption_key.key";
                SecretKey secretKey = loadOrGenerateKey(keyFilePath);
                byte[] encryptedFileData;

                // Get the current working directory
                String currentPath = System.getProperty("user.dir");

                // Define paths for upload and download folders
                Path uploadPath = Paths.get(currentPath, "uploads");
                Path downloadPath = Paths.get(currentPath, "downloads");

                // Create the folders if they don't exist
                try {
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                        System.out.println("Created uploads folder: " + uploadPath);
                    }
                    if (!Files.exists(downloadPath)) {
                        Files.createDirectories(downloadPath);
                        System.out.println("Created downloads folder: " + downloadPath);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Failed to create upload/download folders.");
                }

                System.out.print("\n" + testmsg);
                boolean validInput = false;
                cmd = -1; // Reset cmd before asking for input

                // ensure cmd is number not string
                while (!validInput) {
                    System.out.print("\n\nEnter CMD:");
                    String input = console.readLine();

                    try {
                        cmd = Integer.parseInt(input); // Try to parse the input as an integer
                        validInput = true; // If successful, exit the loop
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input! Please enter a valid number.");
                    }
                }

                switch (cmd) {
                    case 0:
                        // Save log into db
                        sql = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , 'LOGOUT - User logged out' , ? )";

                        ClientService.setLogVoid(sql, acc.getAccNo(), formattedDate);

                        // Inside the LOGOUT section of Client.java
                        try (Socket socket = new Socket("localhost", 12345);
                                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {

                            output.writeObject("LOGOUT"); // Send command
                            output.writeObject(currentTimestamp + "");
                            output.writeObject(acc.getAccNo());
                            output.writeObject(acc.getLoginName());
                            output.writeObject(acc.getEmail());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        sysLoop = false;
                        break;

                    // end of case 0, logout
                    case 1:
                        System.out.print(
                                "*Please ensure the file is store in 'uploads' folder*\n*E.g: abcd.txt*\nEnter the file name to upload to DB : ");
                        String filenameUpload = console.readLine(); // Use different variable name
                        while (!isValidString(filenameUpload)) {
                            System.out.print(
                                    "\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter the file name to upload to DB :");
                            filenameUpload = console.readLine();
                        }

                        Path filePathUpload = uploadPath.resolve(filenameUpload);
                        File fileUpload = filePathUpload.toFile();

                        if (!fileUpload.exists()) {
                            System.out.println("File does not exist in the uploads folder: " + filePathUpload);
                            System.out.println("Please press <enter> to EXIT\n\n");
                            console.readLine();
                            break;
                        }

                        // Encrypt the file
                        encryptedFileData = encryptFile(fileUpload, secretKey);

                        if (encryptedFileData == null) {
                            System.out.println("File encryption failed.");
                            System.out.println("Please press <enter> to EXIT\n\n");
                            console.readLine();
                            break;
                        }

                        // Insert the encrypted file into the database
                        try {
                            sql = "INSERT INTO Comp3334_AccFile (accNo, fileName, fileData) VALUES (?, ?, ?)";
                            rsBool = ClientService.encryptedfile(sql, acc.getAccNo(), fileUpload.getName(),
                                    encryptedFileData);

                            // Inside the UPLOADFILE section of Client.java
                            try {
                                boolean success = ClientService.uploadFile(
                                    currentTimestamp + "",
                                    acc.getAccNo(),
                                    acc.getLoginName(),
                                    acc.getEmail(),
                                    fileUpload.getName(),
                                    encryptedFileData
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            
                            if (rsBool) {
                                System.out.println("File uploaded successfully.");
                                // Save log into db
                                String action = "UPLOAD - Uploaded file: " + fileUpload.getName();
                                String logSQL = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , '" + action + "' , ? )";

                                ClientService.setLogVoid(logSQL, acc.getAccNo(), formattedDate);

                            } else {
                                System.out.println("File upload failed.");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Database error during file upload.");
                        }
                        System.out.println("Please press <enter> to EXIT\n\n");
                        console.readLine();
                        break;

                    // end of case 1, upload
                    case 2: // Download File
                        if (fileno.isEmpty()) {
                            System.out.println("No files available to download.");
                            System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }

                        int fileIndex = -1;
                        validInput = false;

                        // ensure cmd is number not string and within range
                        while (!validInput) {
                            System.out.print("Enter the index of the file to download (1-" + fileno.size() + "): ");
                            String input = console.readLine();

                            try {
                                // Try to parse the input as an integer
                                fileIndex = Integer.parseInt(input) - 1; // Convert to 0-based index
                                if (fileIndex >= 0 && fileIndex < fileno.size()) {
                                    validInput = true; // If successful and within range, exit the loop
                                } else {
                                     System.out.println("Invalid index. Please enter a number between 1 and " + fileno.size() + ".");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input! Please enter a valid number.");
                            }
                        }

                        // No need for this check as the loop ensures validity
                        // if (fileIndex < 0 || fileIndex >= fileno.size()) {
                        //     System.out.println("Invalid file index.");
                        //     break;
                        // }

                        int selectedFileNo = fileno.get(fileIndex);
                        String selectedFileName = filenames.get(fileIndex); // Get filename from list
                        
                        System.out.println("Downloading file: " + selectedFileName + " (ID: " + selectedFileNo + ")");

                        // Make sure downloads folder exists and is accessible
                        try {
                            if (!Files.exists(downloadPath)) {
                                Files.createDirectories(downloadPath);
                                System.out.println("Created downloads folder: " + downloadPath.toAbsolutePath());
                            }
                            // Test folder is writable
                            if (!Files.isWritable(downloadPath)) {
                                System.out.println("Error: Downloads folder is not writable: " + downloadPath.toAbsolutePath());
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }
                        } catch (IOException e) {
                            System.out.println("Error accessing downloads folder: " + e.getMessage());
                            System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }

                        // Retrieve the encrypted file from the database
                        try {
                            System.out.println("Retrieving file from database...");
                            sql = "SELECT fileName, fileData FROM Comp3334_AccFile WHERE fileNo = ? AND (accNo = ? OR fileNo IN (SELECT fileNo FROM Comp3334_ShareFile WHERE accNo = ?))";
                            resultList = ClientService.decFile(sql, selectedFileNo, acc.getAccNo()); // Get List<Map>

                            if (resultList == null || resultList.isEmpty()) {
                                System.out.println("Error: File not found or access denied.");
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }

                            Map<String, Object> fileDataMap = resultList.get(0); // Get the first map
                            
                            // Retrieve fileData as byte[] - Oracle stores BLOB as byte[]
                            byte[] fileEncryptedData = (byte[]) fileDataMap.get("FILEDATA");
                            
                            if (fileEncryptedData == null || fileEncryptedData.length == 0) {
                                System.out.println("Error: File data is empty or corrupted.");
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }
                            
                            System.out.println("File data retrieved. Size: " + fileEncryptedData.length + " bytes");
                            
                            // Check if encryption key is available
                            if (secretKey == null) {
                                System.out.println("Error: Failed to load encryption key.");
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }

                            // Decrypt the file
                            System.out.println("Decrypting file...");
                            System.out.println("IV length: " + GCM_IV_LENGTH + ", Tag length: " + GCM_TAG_LENGTH);
                            byte[] decryptedFileData = decryptFile(fileEncryptedData, secretKey);

                            if (decryptedFileData == null || decryptedFileData.length == 0) {
                                System.out.println("Error: File decryption failed.");
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }
                            
                            System.out.println("File decrypted successfully. Size: " + decryptedFileData.length + " bytes");

                            // Save the decrypted file to the downloads folder
                            Path outputFilePath = downloadPath.resolve(selectedFileName);
                            File outputFile = outputFilePath.toFile();
                            
                            System.out.println("Writing file to: " + outputFilePath.toAbsolutePath());

                            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                                fos.write(decryptedFileData);
                                fos.flush(); // Ensure all data is written
                                
                                // Double-check the file exists and has content
                                if (outputFile.exists() && outputFile.length() > 0) {
                                    System.out.println("[ok] File downloaded and saved successfully!");
                                    System.out.println("  Location: " + outputFilePath.toAbsolutePath());

                                    // Inside the DOWNLOADFILE section of Client.java
                                    try {
                                        boolean success = ClientService.downloadFile(
                                            currentTimestamp + "",
                                            acc.getAccNo(),
                                            acc.getLoginName(),
                                            acc.getEmail(),
                                            selectedFileName,
                                            fileEncryptedData
                                        );
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    
                                    // Log the download operation
                                    String action = "DOWNLOAD - Downloaded file: " + selectedFileName;
                                    String logSQL = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , '" + action + "' , ? )";

                                    ClientService.setLogVoid(logSQL, acc.getAccNo(), formattedDate);
                                } else {
                                    System.out.println("Error: File appears to have been saved but can't be verified.");
                                    System.out.println("Expected at: " + outputFilePath.toAbsolutePath());
                                    System.out.println("Please check folder permissions and disk space.");
                                }
                            } catch (IOException e) {
                                System.out.println("Error saving file: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            System.out.println("Error during download process: " + e.getMessage());
                            e.printStackTrace();
                        }
                        
                        System.out.println("Please press <enter> to continue...");
                        console.readLine();
                        break;

                    // end of case 2, download
                    case 3: // Edit (Re-upload)
                        if (fileno.isEmpty()) {
                            System.out.println("No files available to edit.");
                            System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }

                        int editFileIndex = -1;
                        validInput = false;
                        while (!validInput) {
                            System.out.print("Enter the index of the file to edit (1-" + fileno.size() + "): ");
                            String input = console.readLine();
                            try {
                                editFileIndex = Integer.parseInt(input) - 1; // Convert to 0-based index
                                if (editFileIndex >= 0 && editFileIndex < fileno.size()) {
                                    validInput = true; // If successful and within range, exit the loop
                                } else {
                                    System.out.println("Invalid index. Please enter a number between 1 and " + fileno.size() + ".");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input! Please enter a valid number.");
                            }
                        }

                        int editFileNo = fileno.get(editFileIndex);
                        String editFileName = filenames.get(editFileIndex); // Get the filename from list

                        System.out.println("Selected file: " + editFileName + " (ID: " + editFileNo + ")");

                        // Check if the file belongs to the current user
                        sql = "SELECT accNo FROM Comp3334_AccFile WHERE fileNo = ? AND accNo = ?";
                        resultList = ClientService.checkFileOwner(sql, editFileNo, acc.getAccNo());

                        if (resultList == null || resultList.isEmpty()) {
                            System.out.println("You can only edit your own files, or file not found.");
                            System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }

                        // Download the file first
                        try {
                            // Get the file data
                            System.out.println("Retrieving file from database...");
                            sql = "SELECT fileName, fileData FROM Comp3334_AccFile WHERE fileNo = ? AND accNo = ?";
                            resultList = ClientService.selectEditFile(sql, editFileNo, acc.getAccNo());

                            if (resultList == null || resultList.isEmpty()) {
                                System.out.println("Error: File not found or access denied.");
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }

                            Map<String, Object> fileDataMap = resultList.get(0);
                            byte[] fileEncryptedData = (byte[]) fileDataMap.get("FILEDATA");
                            
                            if (fileEncryptedData == null || fileEncryptedData.length == 0) {
                                System.out.println("Error: File data is empty or corrupted.");
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }
                            
                            System.out.println("File data retrieved. Size: " + fileEncryptedData.length + " bytes");

                            if (secretKey == null) {
                                System.out.println("Error: Failed to load encryption key.");
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }

                            // Decrypt the file
                            System.out.println("Decrypting file...");
                            System.out.println("IV length: " + GCM_IV_LENGTH + ", Tag length: " + GCM_TAG_LENGTH);
                            byte[] decryptedFileData = decryptFile(fileEncryptedData, secretKey);
                            
                            if (decryptedFileData == null || decryptedFileData.length == 0) {
                                System.out.println("Error: File decryption failed.");
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }
                            
                            System.out.println("File decrypted successfully. Size: " + decryptedFileData.length + " bytes");

                            // Save the file to the downloads folder
                            Path downloadFilePath = downloadPath.resolve(editFileName);
                            File downloadFile = downloadFilePath.toFile();
                            
                            System.out.println("Writing file to: " + downloadFilePath.toAbsolutePath());
                            
                            try (FileOutputStream fos = new FileOutputStream(downloadFile)) {
                                fos.write(decryptedFileData);
                                fos.flush(); // Ensure all data is written
                                
                                // Double-check the file exists and has content
                                if (downloadFile.exists() && downloadFile.length() > 0) {
                                    System.out.println("[ok] File downloaded and saved successfully!");
                                    System.out.println("  Location: " + downloadFilePath.toAbsolutePath());
                                    System.out.println("\nPlease edit this file with your system editor.");
                                    System.out.println("After editing, place the file in the 'uploads' folder.");
                                    System.out.println("Press <enter> when you're ready to upload the edited file.");
                                    console.readLine();
                                } else {
                                    System.out.println("Error: File appears to have been saved but can't be verified.");
                                    System.out.println("Expected at: " + downloadFilePath.toAbsolutePath());
                                    System.out.println("Please check folder permissions and disk space.");
                                    System.out.println("Please press <enter> to continue...");
                                    console.readLine();
                                    break;
                                }
                            }

                            // Now handle the re-upload process
                            System.out.print(
                                    "*Please ensure the edited file is stored in the 'uploads' folder*\n*E.g: " + editFileName + "*\nEnter the file name to re-upload: ");
                            String reuploadFileName = console.readLine();
                            while (!isValidString(reuploadFileName)) {
                                System.out.print(
                                        "\nCan't include any invalid symbols like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter the file name to re-upload: ");
                                reuploadFileName = console.readLine();
                            }

                            // Check if file exists in uploads folder
                            Path reuploadFilePath = uploadPath.resolve(reuploadFileName);
                            File reuploadFile = reuploadFilePath.toFile();

                            if (!reuploadFile.exists()) {
                                System.out.println("File does not exist in the uploads folder: " + reuploadFilePath);
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }
                            
                            System.out.println("Found file to upload. Size: " + reuploadFile.length() + " bytes");

                            // Encrypt the edited file
                            System.out.println("Encrypting file...");
                            byte[] newEncryptedFileData = encryptFile(reuploadFile, secretKey);

                            if (newEncryptedFileData == null) {
                                System.out.println("Error: File encryption failed.");
                                System.out.println("Please press <enter> to continue...");
                                console.readLine();
                                break;
                            }
                            
                            System.out.println("File encrypted successfully. Size: " + newEncryptedFileData.length + " bytes");

                            // Update the file in the database
                            System.out.println("Updating file in database...");
                            sql = "UPDATE Comp3334_AccFile SET fileData = ?, updatedAt = CURRENT_TIMESTAMP WHERE fileNo = ?";
                            int rowsUpdated = ClientService.updateEditedFile(sql, newEncryptedFileData, editFileNo);

                            if (rowsUpdated > 0) {
                                System.out.println("[ok] File updated successfully in database!");

                                // Record in log
                                String action = "EDIT - Re-uploaded file (fileNo): " + editFileNo + " with content from " + reuploadFileName;
                                String logSQL = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , '" + action + "' , ? )";
                                ClientService.setLogVoid(logSQL, acc.getAccNo(), formattedDate);

                                // Notify server via socket
                                try {
                                    boolean success = ClientService.editFile(
                                        currentTimestamp + "",
                                        acc.getAccNo(),
                                        acc.getLoginName(),
                                        acc.getEmail(),
                                        editFileNo,
                                        reuploadFileName,
                                        newEncryptedFileData
                                    );
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                System.out.println("Failed to update the file in the database.");
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing file edit request: " + e.getMessage());
                            e.printStackTrace();
                        }
                        
                        System.out.println("Please press <enter> to continue...");
                        console.readLine();
                        break;

                    // end of case 3, edit
                    case 4: // Share File
                         if (fileno.isEmpty()) {
                            System.out.println("No files available to share.");
                            System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }
                        validInput = false;
                        int shareFileIndex = -1;
                        while (!validInput) {
                             System.out.print("Enter the index of the file to share (1-" + fileno.size() + "): ");
                            String input = console.readLine();
                            try {
                                shareFileIndex = Integer.parseInt(input) - 1; // Convert to 0-based index
                                 if (shareFileIndex >= 0 && shareFileIndex < fileno.size()) {
                                    validInput = true; // If successful and within range, exit the loop
                                } else {
                                     System.out.println("Invalid index. Please enter a number between 1 and " + fileno.size() + ".");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input! Please enter a valid number.");
                            }
                        }

                        int shareFileNo = fileno.get(shareFileIndex);
                        String shareFileName = filenames.get(shareFileIndex); // Get the filename from list

                        System.out.print("Enter the username of the user to share the file with: ");
                        String shareToUser = console.readLine();
                         while (!isValidString(shareToUser) || shareToUser.isEmpty()) { // Also check for empty
                            System.out.print(
                                    "\nInvalid username format or empty.\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter the username to share with:");
                            shareToUser = console.readLine();
                        }

                        // Check if the file belongs to the current user
                        sql = "SELECT accNo FROM Comp3334_AccFile WHERE fileNo = ? AND accNo = ?";

                        // No ResultSet to close
                        resultList = ClientService.checkFileOwner(sql, shareFileNo, acc.getAccNo());

                        if (resultList == null || resultList.isEmpty()) { // Check if list is empty
                            System.out.println("You can only share your own files, or file not found.");
                             System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }

                        // Check if the file is already shared with the target user
                        sql = "SELECT sf.accNo FROM Comp3334_ShareFile sf JOIN Comp3334_CSSAccount ca ON sf.accNo = ca.accNo WHERE sf.fileNo = ? AND ca.loginName = ? ";

                        // No ResultSet to close
                        resultList = ClientService.checkAlreadySharedFile(sql, shareFileNo, shareToUser);

                        if (resultList != null && !resultList.isEmpty()) { // Check if list is NOT empty
                            System.out.println("File is already shared with user '" + shareToUser + "'.");
                            System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }

                        // Share the file
                        sql = "INSERT INTO Comp3334_ShareFile (fileNo, accNo) SELECT ?, accNo FROM Comp3334_CSSAccount WHERE loginName = ?";

                        int rowsShared = ClientService.sharedFile(sql, shareFileNo, shareToUser);

                        if (rowsShared > 0) {
                            System.out.println("File shared successfully with " + shareToUser + ".");

                            // Notify the server

                            try {
                                boolean success = ClientService.shareFile(
                                    currentTimestamp + "",
                                    acc.getAccNo(),
                                    acc.getLoginName(),
                                    acc.getEmail(),
                                    shareFileNo,
                                    shareToUser
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // Save log into db
                            String action = "SHARE - Shared file (fileNo): " + shareFileNo + " with User: "
                                    + shareToUser;

                            String logSQL = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , '" + action
                                    + "' , ? )";

                            ClientService.setLogVoid(logSQL, acc.getAccNo(), formattedDate);

                        } else {
                            System.out.println("Failed to share the file. Ensure the username '" + shareToUser + "' is correct and exists.");
                        }
                         System.out.println("Please press <enter> to continue...");
                        console.readLine();
                        break;

                    // end of case 4, share
                    case 5: // Unshare File
                         if (fileno.isEmpty()) {
                            System.out.println("No files available to unshare.");
                            System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }
                        validInput = false;
                        int unshareFileIndex = -1;
                        while (!validInput) {
                            System.out.print("Enter the index of the file to unshare (1-" + fileno.size() + "): ");
                            String input = console.readLine();
                            try {
                                unshareFileIndex = Integer.parseInt(input) - 1; // Convert to 0-based index
                                 if (unshareFileIndex >= 0 && unshareFileIndex < fileno.size()) {
                                    validInput = true; // If successful and within range, exit the loop
                                } else {
                                     System.out.println("Invalid index. Please enter a number between 1 and " + fileno.size() + ".");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input! Please enter a valid number.");
                            }
                        }

                        int unshareFileNo = fileno.get(unshareFileIndex);

                        // First, check if the current user owns the file. Only owners can unshare.
                        sql = "SELECT accNo FROM Comp3334_AccFile WHERE fileNo = ? AND accNo = ?";
                        resultList = ClientService.checkFileOwner(sql, unshareFileNo, acc.getAccNo());

                        if (resultList == null || resultList.isEmpty()) {
                            System.out.println("You can only unshare your own files, or file not found.");
                            System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }

                        // Now, ask who to unshare from
                        System.out.print("Enter the username of the user to unshare the file from: ");
                        String unshareFromUser = console.readLine();
                         while (!isValidString(unshareFromUser) || unshareFromUser.isEmpty()) {
                            System.out.print(
                                    "\nInvalid username format or empty.\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter the username to unshare from:");
                            unshareFromUser = console.readLine();
                        }


                        // Unshare the file by deleting the specific share record
                        sql = "DELETE FROM Comp3334_ShareFile WHERE fileNo = ? AND accNo = (SELECT accNo FROM Comp3334_CSSAccount WHERE loginName = ?)";

                        int rowsUnshared = ClientService.unshareFile(sql, unshareFileNo, unshareFromUser);

                        if (rowsUnshared > 0) {
                            System.out.println("File unshared successfully from " + unshareFromUser + ".");

                            // Notify the server
                            try {
                                boolean success = ClientService.unshareFile(
                                    currentTimestamp + "",
                                    acc.getAccNo(),
                                    acc.getLoginName(),
                                    acc.getEmail(),
                                    unshareFileNo,
                                    unshareFromUser
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // Save log into db
                            String action = "UNSHARE - Unshared file (fileNo): " + unshareFileNo + " from User: " // Corrected log message
                                    + unshareFromUser;

                            String logSQL = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , '" + action
                                    + "' , ? )";

                            ClientService.setLogVoid(logSQL, acc.getAccNo(), formattedDate);

                        } else {
                            System.out.println("Failed to unshare the file. Ensure the username '" + unshareFromUser + "' is correct and the file was actually shared with them.");
                        }
                         System.out.println("Please press <enter> to continue...");
                        console.readLine();
                        break;

                    // end of case 5, unshare
                    case 6: // Delete File
                         if (fileno.isEmpty()) {
                            System.out.println("No files available to delete.");
                            System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }
                        validInput = false;
                        int deleteFileIndex = -1;
                        while (!validInput) {
                            System.out.print("Enter the index of the file to delete (1-" + fileno.size() + "): ");
                            String input = console.readLine();
                            try {
                                deleteFileIndex = Integer.parseInt(input) - 1; // Convert to 0-based index
                                 if (deleteFileIndex >= 0 && deleteFileIndex < fileno.size()) {
                                    validInput = true; // If successful and within range, exit the loop
                                } else {
                                     System.out.println("Invalid index. Please enter a number between 1 and " + fileno.size() + ".");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input! Please enter a valid number.");
                            }
                        }

                        int deleteFileNo = fileno.get(deleteFileIndex);

                        // Check if the file belongs to the current user
                        sql = "SELECT accNo FROM Comp3334_AccFile WHERE fileNo = ? AND accNo = ?";

                        // No ResultSet to close
                        resultList = ClientService.checkFileOwner(sql, deleteFileNo, acc.getAccNo());

                        if (resultList == null || resultList.isEmpty()) { // Check if list is empty
                            System.out.println("You can only delete your own files, or file not found.");
                            System.out.println("Please press <enter> to continue...");
                            console.readLine();
                            break;
                        }

                        // Delete the file from Comp3334_ShareFile first (clean up shares)
                        sql = "DELETE FROM Comp3334_ShareFile WHERE fileNo = ?";
                        ClientService.deleteFromShareFile(sql, deleteFileNo); // No return value needed

                        // Delete the file from Comp3334_AccFile
                        sql = "DELETE FROM Comp3334_AccFile WHERE fileNo = ?";
                        int rowsDeleted = ClientService.deleteFromAccFile(sql, deleteFileNo);

                        if (rowsDeleted > 0) {
                            System.out.println("File deleted successfully.");
                            // Remove from local lists as well
                            fileno.remove(deleteFileIndex);
                            filenames.remove(deleteFileIndex);


                            // Notify the server
                            try {
                                boolean success = ClientService.deleteFile(
                                    currentTimestamp + "",
                                    acc.getAccNo(),
                                    acc.getLoginName(),
                                    acc.getEmail(),
                                    deleteFileNo
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // Save log into db
                            String action = "DELETE - Deleted file (fileNo): " + deleteFileNo;
                            String logSQL = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , '" + action
                                    + "' , ? )";

                            ClientService.setLogVoid(logSQL, acc.getAccNo(), formattedDate);

                        } else {
                            System.out.println("Failed to delete the file from the main table (it might have already been deleted or an error occurred).");
                        }
                         System.out.println("Please press <enter> to continue...");
                        console.readLine();
                        break;

                    // end of case 6, delete
                    case 7: // Reset Password
                        System.out.print("Enter your current password:");
                        String old_pwd = console.readLine();
                        while (!isValidString(old_pwd) || (old_pwd == null || old_pwd.isEmpty())
                                || (old_pwd.length() > 15 || old_pwd.length() < 1)) {
                            System.out.print(
                                    "\nPassword length must be 1-15\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter your password:");
                            old_pwd = console.readLine();
                        }

                        System.out.print("Enter your new password:");
                        String new_pwd = console.readLine();
                        while (!isValidString(new_pwd) || (new_pwd == null || new_pwd.isEmpty())
                                || (new_pwd.length() > 15 || new_pwd.length() < 1)) {
                            System.out.print(
                                    "\nPassword length must be 1-15\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter your password:");
                            new_pwd = console.readLine();
                        }

                        System.out.print("Confirm your new password:");
                        String confirm_new_pwd = console.readLine();
                        while (!isValidString(confirm_new_pwd) || (confirm_new_pwd == null || confirm_new_pwd.isEmpty())
                                || (confirm_new_pwd.length() > 15 || confirm_new_pwd.length() < 1)) {
                            System.out.print(
                                    "\nPassword length must be 1-15\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter your password:");
                            confirm_new_pwd = console.readLine();
                        }


                        if (!new_pwd.equals(confirm_new_pwd)) {
                            System.out.println("New passwords don't match!");
                            System.out.println("Please press <enter> to EXIT\n\n");
                            console.readLine();
                            break;
                        }
                        // get password from database
                        sql = "SELECT password FROM Comp3334_CSSAccount WHERE loginName = ?";

                        // No ResultSet to close
                        resultList = ClientService.getPWD(sql, acc.getLoginName()); // Get List<Map>

                        String storedhash = null;
                        String new_hashed_pwd = null;
                        if (resultList != null && !resultList.isEmpty()) { // Check if list is not empty
                            Map<String, Object> pwdData = resultList.get(0);
                            storedhash = (String) pwdData.get("PASSWORD"); // Key is likely PASSWORD

                            if (storedhash == null || !verifyPassword(old_pwd, storedhash)) {
                                System.out.println("Current password is not correct.");
                                System.out.println("Please press <enter> to EXIT\n\n");
                                console.readLine();
                                break;
                            }

                            new_hashed_pwd = hashPassword(new_pwd);

                            String sqlUpdate = "UPDATE Comp3334_CSSAccount SET password = ? WHERE loginName = ?";

                            // Properly declare the rowsUpdated variable
                            int rowsUpdated = ClientService.updateHashPWD(sqlUpdate, new_hashed_pwd, acc.getLoginName());

                            if (rowsUpdated > 0) {
                                System.out.println("Password reset successfully.");

                                // Save log into db
                                sql = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , 'RESET_PASSWORD - User reset password' , ? )";

                                ClientService.setLogVoid(sql, acc.getAccNo(), formattedDate);

                                // Inside the reset password section of Client.java
                                try {
                                    boolean success = ClientService.resetPWD(
                                        currentTimestamp + "",
                                        acc.getAccNo(),
                                        acc.getLoginName(),
                                        acc.getEmail(),
                                        new_hashed_pwd,
                                        acc.getPhoneNo(),
                                        acc.getUserStatus(),
                                        acc.getUserRight()
                                    );
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            } else {
                                System.out.println("Failed to reset password.");
                            }
                        } else {
                             System.out.println("Failed to retrieve current password information.");
                        }
                        System.out.println("Please press <enter> to EXIT\n\n");
                        console.readLine();
                        break;

                    // end of case 7, reset pwd
                    case 8: // View Logs (Admin)

                        if (acc.getUserRight() != 1) {
                             System.out.println("Access denied. Admin rights required.");
                             System.out.println("Please press <enter> to continue...");
                             console.readLine();
                            break;
                        }
                        String selectSQL = "SELECT logNo, accNo, action, logDate FROM Comp3334_LogCSS ORDER BY logNo FETCH FIRST 100 ROWS ONLY"; // Select specific 100 columns
                        String nameSQL = "SELECT loginName FROM Comp3334_CSSAccount where accNo = ?"; // SQL to get name

                        System.out.println("\n=== Audit Logs ===");
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Include time

                        try {
                            List<Map<String, Object>> logList = ClientService.selectAllLog(selectSQL); // Get logs

                            if (logList == null || logList.isEmpty()) {
                                System.out.println("No logs found.");
                            } else {
                                for (Map<String, Object> logEntry : logList) {
                                    int logAccNo = getIntFromMap(logEntry, "ACCNO");
                                    String action = (String) logEntry.get("ACTION");
                                    // Oracle DATE might be returned as Timestamp
                                    Timestamp logTimestamp = (Timestamp) logEntry.get("LOGDATE");
                                    String formattedlogDate = (logTimestamp != null) ? dateFormat.format(new Date(logTimestamp.getTime())) : "N/A";

                                    // Get login name for the accNo
                                    String loginName = "Unknown (accNo:" + logAccNo + ")"; // Default
                                    List<Map<String, Object>> nameList = ClientService.displayLog(nameSQL, logAccNo);
                                    if (nameList != null && !nameList.isEmpty()) {
                                        loginName = (String) nameList.get(0).get("LOGINNAME");
                                    }

                                    // Print each record
                                    System.out.printf("[%s] %s: %s%n", formattedlogDate, loginName, action);
                                }
                            }

                        } catch (Exception e) { // Catch broader exceptions
                            e.printStackTrace();
                            System.out.print("System: Error retrieving logs.");
                        }
                        System.out.println("\nPlease press <enter> to EXIT\n");
                        console.readLine();
                        break;

                    // end of case 8, admin log
                    case 9: // Lock/Unlock Account (Admin)
                        if (acc.getUserRight() != 1) {
                             System.out.println("Access denied. Admin rights required.");
                             System.out.println("Please press <enter> to continue...");
                             console.readLine();
                            break;
                        }

                        System.out.print("Enter the UserName to lock/unlock the Account (-1 to Exit):");
                        String userName = console.readLine();

                        if ("-1".equals(userName)) {
                            break; // Exit if user enters -1
                        }

                        while (!isValidString(userName) || userName.isEmpty()) {
                            System.out.print(
                                    "\nInvalid username format or empty.\nCan't including any invalid symbol like \"',!%-+`~><$/\\|&*()[]{};:?#^\nEnter the username:");
                            userName = console.readLine();
                             if ("-1".equals(userName)) break; // Allow exit from loop
                        }
                         if ("-1".equals(userName)) break; // Exit if user entered -1 in loop

                        sql = "Select userStatus, accNo From Comp3334_CSSAccount WHERE loginName = ?";

                        // No ResultSet to close
                        resultList = ClientService.getUserStatus(sql, userName); // Get List<Map>

                        if (resultList != null && !resultList.isEmpty()) {
                            Map<String, Object> userData = resultList.get(0);
                            int userAccNo = getIntFromMap(userData, "ACCNO");
                            int currentUserStatus = getIntFromMap(userData, "USERSTATUS");

                            // admin can't lock himself account
                            if (userAccNo != acc.getAccNo()) {
                                int updateUserStatus = (currentUserStatus == 0) ? 1 : 0; // Toggle status

                                sql = "UPDATE Comp3334_CSSAccount SET userStatus = ? WHERE loginName = ?";

                                int rows = ClientService.updateUserStatus(sql, updateUserStatus, userName);

                                if (rows > 0) {
                                    String statusAction = (updateUserStatus == 1) ? "locked" : "unlocked";
                                    System.out.println("User '" + userName + "' account has been " + statusAction + ".");

                                    // Save log into db
                                    String logActionDetail = "Account_Status - User:" + userName + " account " + statusAction + " by " + acc.getLoginName();
                                    sql = "INSERT INTO Comp3334_LogCSS VALUES (null, ? , '" + logActionDetail + "' , ? )";

                                    // Log against the admin's account number (acc.getAccNo()) or the user's (userAccNo)?
                                    // Logging against admin seems more appropriate for tracking admin actions.
                                    ClientService.setLogVoid(sql, acc.getAccNo(), formattedDate);


                                    // Inside the Account_Lock section notification
                                    try (Socket socket = new Socket("localhost", 12345);
                                            ObjectOutputStream output = new ObjectOutputStream(
                                                    socket.getOutputStream())) {

                                        output.writeObject("Account_Lock"); // Send command
                                        output.writeObject(currentTimestamp + "");
                                        output.writeObject(acc.getAccNo()); // admin No
                                        output.writeObject(acc.getLoginName()); // admin Name
                                        output.writeObject(userAccNo); // User No
                                        output.writeObject(userName); // User Name
                                        output.writeObject(updateUserStatus); // updated UserStatus

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    System.out.println("\nFailed to update user status for '" + userName + "'.");
                                }
                            } else {
                                System.out.println("Admin cannot lock/unlock their own account.");
                            }
                        } else {
                             System.out.println("User '" + userName + "' not found.");
                        }
                        System.out.println("Please press <enter> to continue\n\n");
                        console.readLine();
                        // clearScreen(); // Don't clear screen immediately, let user see the message
                        break;

                    // end of case 9, admin lock/unlock account
                    default:
                        System.out.println("Invalid command. Please try again.");
                        System.out.println("Please press <enter> to continue...");
                        console.readLine();
                        break; // Added default case
                } // End Switch
            } // End If UserRight Check
            else {
                 System.out.println("Invalid user rights configuration. Exiting.");
                 sysLoop = false; // Exit loop if rights are invalid
            }
        } // End While Loop

        // Exit the program
        endView();
        System.out.println("Please press <enter> to EXIT\n\n");
        console.readLine();
    }

    // --- functional method, not main logic ---

    // Helper method to safely get an integer from Map (handles BigDecimal from Oracle NUMBER)
    private static int getIntFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).intValue();
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                System.err.println("Warning: Could not parse string '" + value + "' as int for key '" + key + "'");
                return 0; // Or throw an exception, depending on desired behavior
            }
        }
        System.err.println("Warning: Unexpected type for key '" + key + "': " + (value != null ? value.getClass().getName() : "null"));
        return 0; // Default or error value
    }


    // clearScreen function -- clear screen
    public static void clearScreen() throws IOException, InterruptedException {
        if (System.getProperty("os.name").contains("Windows"))
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        else
            System.out.print("\033[H\033[2J");
    }

    public static void topView() {
        System.out.println("\n=================================================================");
        System.out.println("COMPUTER SYSTEMS SECURITY (Comp3334_CSS)");
        System.out.println("Date: " + currentTimestamp);
    }

    public static void cssView(Account acc) {
        topView();
        displayAccData(acc);
        System.out.println("\n** Command List **"); // Added newline for better spacing
        // upload logout download sharing editing
        System.out.print(
                "[0]:Logout & Exit          [1]:Upload File      [2]:Download File"
                        + "\n[3]:ReUpload File(Edit)    [4]:File ShareTo     [5]:File UnshareFrom " // Corrected label
                        + "\n[6]:Delete File            [7]:Reset PWD          ");
        if (acc.getUserRight() == 1) {
            System.out.print("\n** Admin Func **"
                    + "    \n[8]:View Audit Logs        [9]:Account Lock/Unlock"); // Corrected label
        }
    }

    public static int endView() {
        System.out.println("\n\nExit...");
        System.out.println("\n                         *Thinks for using*                      ");
        System.out.println("=================================================================");
        return 0;
    }

    public static void displayAccData(Account acc) {
        System.out.printf("\nAccNo:%s | Name:%s | Email:%s | PhoneNo:%s", acc.getAccNo() + "",
                acc.getLoginName() + "", acc.getEmail() + "", acc.getPhoneNo() + "");
        System.out.println("\n=================================================================");
    }

    public static boolean isValidString(String input) {

        // Check if input is null or consists only of spaces
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        // Define the set of invalid characters
        String invalidCharacters = "\"',!%-+`~><$/\\|&*()[]{};:?#^";

        // Iterate through each character in the input string
        for (char c : input.toCharArray()) {
            // Check if the character is in the invalid characters set
            if (invalidCharacters.indexOf(c) != -1) {
                return false; // Invalid character found
            }
        }

        return true; // No invalid characters found
    }

    public static boolean isValidNumber(String input) {
        // Check if the input is null or empty
        if (input == null || input.isEmpty()) {
            return false;
        }

        // Iterate through each character in the input string
        for (char c : input.toCharArray()) {
            // Check if the character is not a digit (0-9)
            if (!Character.isDigit(c)) {
                return false; // Invalid character found
            }
        }

        return true; // All characters are valid digits
    }

    public static boolean verifyPassword(String inputPassword, String storedHash) {
        // Define hashpwd to 2 part, first part = salt, sec part = hashed pwd
        String[] parts = storedHash.split(":");
        String salt = parts[0];
        String hash = parts[1];
        // hash the inputed pwd
        String inputHash = hashPasswordWithPBKDF2(inputPassword, salt);
        // if h(hash_inputed_pwd) == h(stored_hash), = ture = verify = login
        return inputHash.equals(hash);
    }

    public static String hashPassword(String password) {
        // gen salt and use salt to hash function
        String salt = generateSalt();
        String hash = hashPasswordWithPBKDF2(password, salt);
        return salt + ":" + hash; // Store salt and hash together
    }

    public static String hashPasswordWithPBKDF2(String password, String salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16]; // 128 bits
        random.nextBytes(salt);
        // Convert to Base64 for better compatibility if needed, but current String(byte[]) might work if encoding is consistent
        // return Base64.getEncoder().encodeToString(salt); // Example using Base64
         return new String(salt, StandardCharsets.ISO_8859_1); // Use a consistent charset
    }

    public static void storeTOTP(String totp, String fileName) {
        File file = new File(fileName); // Create a file object for the current directory

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(totp); // Write the TOTP to the file
            System.out.println("TOTP successfully stored in " + fileName + " (Please Use it within 30s)");
        } catch (IOException e) {
            System.err.println("Error writing TOTP to file: " + e.getMessage());
        }
    }

    public static String toTOTPKEY(String input) {
        String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        // Filter the hash to only include valid characters
        StringBuilder filteredinput = new StringBuilder();
        for (char c : input.toUpperCase().toCharArray()) {
            if (validChars.indexOf(c) != -1) { // Check if the character is valid
                filteredinput.append(c);
            }
        }

        String result = filteredinput.toString();
        // System.out.println(result);
        return result;
    }

    private static SecretKey loadOrGenerateKey(String keyFilePath) {
        File keyFile = new File(keyFilePath);
        SecretKey secretKey = null;

        if (keyFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(keyFile))) {
                secretKey = (SecretKey) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256); // Use 256-bit AES
                secretKey = keyGen.generateKey();

                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(keyFile))) {
                    oos.writeObject(secretKey);
                }
            } catch (NoSuchAlgorithmException | IOException e) {
                e.printStackTrace();
            }
        }

        return secretKey;
    }

    private static byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private static byte[] encryptFile(File file, SecretKey secretKey) {
        byte[] iv = generateIV(); // Generate a random IV
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] fileData = Files.readAllBytes(file.toPath());
            byte[] encryptedData = cipher.doFinal(fileData);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);

            return byteBuffer.array();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] decryptFile(byte[] encryptedFileData, SecretKey secretKey) {
        // Remove strict length validation, just check for null
        if (encryptedFileData == null) {
            System.err.println("Encrypted data is null.");
            return null;
        }
        try {
            // Add more detailed logging instead of validation
            System.out.println("Decrypting data of length: " + encryptedFileData.length + " bytes");
            
            // Safety check to prevent ByteBuffer underflow
            if (encryptedFileData.length < GCM_IV_LENGTH) {
                System.err.println("Warning: Data too short to extract IV (" + GCM_IV_LENGTH + 
                                  " bytes needed, got " + encryptedFileData.length + " bytes)");
                return null;
            }
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedFileData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] encryptedData = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedData);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            System.err.println("Decryption failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}