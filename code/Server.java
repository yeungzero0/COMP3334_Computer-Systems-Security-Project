import java.io.*;
import java.net.*;
import java.nio.ByteBuffer; // Added import for ByteBuffer
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec; // Added import for GCMParameterSpec

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData; // Added import for ResultSetMetaData
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.jdbc.driver.OracleDriver;

public class Server {
    private static final int PORT = 12345; // Port number for the server
    private static final String DB_URL = "jdbc:oracle:thin:@studora.comp.polyu.edu.hk:1521:dbms";
    private static final String DB_USER = "\"22027226d\""; // Your Oracle Account (SID)
    private static final String DB_PASSWORD = ""; // Your Oracle PWD
    private transient Connection conn = null; // Mark connection as transient to prevent serialization

    Server() throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
        DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
        this.conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                // System.out.println("\nNew client connected");
                handleClient(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        ObjectInputStream input = null;
        ObjectOutputStream output = null;
        Server server = null;

        try {
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());

            // Create a server instance to handle database operations
            server = new Server();

            // Read command from the client
            String command = (String) input.readObject();
            // System.out.println("Received command: " + command);
            // Handle database related commands from ClientService
            if ("GET_HASH_PWD".equals(command)) {
                String sql = (String) input.readObject();
                String acc_name = (String) input.readObject();

                String result = server.getHashPWD(sql, acc_name);

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result);
            } else if ("GET_ACC".equals(command)) {
                String sql = (String) input.readObject();
                String acc_name = (String) input.readObject();
                String pwd = (String) input.readObject();

                List<Map<String, Object>> result = server.getAcc(sql, acc_name, pwd); // Changed return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("SET_LOG".equals(command)) {
                String sql = (String) input.readObject();
                int accNo = (Integer) input.readObject();
                String formattedDate = (String) input.readObject();

                server.setLogVoid(sql, accNo, formattedDate);

                // Send back response
                output.writeInt(0); // No response data
            } else if ("CHECK_EMAIL".equals(command)) {
                String sql = (String) input.readObject();
                String email = (String) input.readObject();

                boolean result = server.registeredEmail(sql, email);

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result);
            } else if ("REGISTER_ACCOUNT".equals(command)) {
                String sql = (String) input.readObject();
                String name = (String) input.readObject();
                String pwd = (String) input.readObject();
                String email = (String) input.readObject();
                int phoneNo = (Integer) input.readObject();

                server.regisiterAccount(sql, name, pwd, email, phoneNo);

                // Send back response
                output.writeInt(0); // No response data
            } else if ("SELECT_FILES".equals(command)) {
                String sql = (String) input.readObject();
                int accNo = (Integer) input.readObject();

                List<Map<String, Object>> result = server.selectFile(sql, accNo); // Changed return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("SELECT_SHARED_FILES".equals(command)) {
                String sql = (String) input.readObject();
                int accNo = (Integer) input.readObject();

                List<Map<String, Object>> result = server.selectWithSharedFile(sql, accNo); // Changed return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("UPLOAD_FILE".equals(command)) {
                String sql = (String) input.readObject();
                int accNo = (Integer) input.readObject();
                String fileName = (String) input.readObject();
                byte[] encryptedFileData = (byte[]) input.readObject();

                boolean result = server.encryptedfile(sql, accNo, fileName, encryptedFileData);

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result);
            } else if ("DOWNLOAD_FILE".equals(command)) {
                String sql = (String) input.readObject();
                int fileNo = (Integer) input.readObject();
                int accNo = (Integer) input.readObject();

                List<Map<String, Object>> result = server.decFile(sql, fileNo, accNo); // Changed return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("SELECT_EDIT_FILE".equals(command)) {
                String sql = (String) input.readObject();
                int fileNo = (Integer) input.readObject();
                int accNo = (Integer) input.readObject();

                List<Map<String, Object>> result = server.selectEditFile(sql, fileNo, accNo); // Changed return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("UPDATE_EDITED_FILE".equals(command)) {
                String sql = (String) input.readObject();
                byte[] encryptedFileData = (byte[]) input.readObject();
                int fileNo = (Integer) input.readObject();

                int result = server.updateEditedFile(sql, encryptedFileData, fileNo);

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result);
            } else if ("CHECK_FILE_OWNER".equals(command)) {
                String sql = (String) input.readObject();
                int fileNo = (Integer) input.readObject();
                int accNo = (Integer) input.readObject();

                List<Map<String, Object>> result = server.checkFileOwner(sql, fileNo, accNo); // Changed return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("CHECK_ALREADY_SHARED".equals(command)) {
                String sql = (String) input.readObject();
                int fileNo = (Integer) input.readObject();
                String userName = (String) input.readObject();

                List<Map<String, Object>> result = server.checkAlreadySharedFile(sql, fileNo, userName); // Changed
                                                                                                         // return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("SHARE_FILE".equals(command)) {
                String sql = (String) input.readObject();
                int fileNo = (Integer) input.readObject();
                String userName = (String) input.readObject();

                int result = server.sharedFile(sql, fileNo, userName);

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result);
            } else if ("UNSHARE_FILE".equals(command)) {
                String sql = (String) input.readObject();
                int fileNo = (Integer) input.readObject();
                String userName = (String) input.readObject();

                int result = server.unshareFile(sql, fileNo, userName);

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result);
            } else if ("DELETE_FROM_SHARE".equals(command)) {
                String sql = (String) input.readObject();
                int fileNo = (Integer) input.readObject();

                server.deleteFromShareFile(sql, fileNo);

                // Send back response
                output.writeInt(0); // No response data
            } else if ("DELETE_FILE".equals(command)) {
                String sql = (String) input.readObject();
                int fileNo = (Integer) input.readObject();

                int result = server.deleteFromAccFile(sql, fileNo);

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result);
            } else if ("GET_PWD".equals(command)) {
                String sql = (String) input.readObject();
                String loginName = (String) input.readObject();

                List<Map<String, Object>> result = server.getPWD(sql, loginName); // Changed return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("UPDATE_PWD".equals(command)) {
                String sql = (String) input.readObject();
                String newPwd = (String) input.readObject();
                String loginName = (String) input.readObject();

                int result = server.updateHashPWD(sql, newPwd, loginName);

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result);
            } else if ("SELECT_ALL_LOGS".equals(command)) {
                String sql = (String) input.readObject();

                List<Map<String, Object>> result = server.selectAllLog(sql); // Changed return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("DISPLAY_LOG".equals(command)) {
                String sql = (String) input.readObject();
                int accNo = (Integer) input.readObject();

                List<Map<String, Object>> result = server.displayLog(sql, accNo); // Changed return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("GET_USER_STATUS".equals(command)) {
                String sql = (String) input.readObject();
                String userName = (String) input.readObject();

                List<Map<String, Object>> result = server.getUserStatus(sql, userName); // Changed return type

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result); // Send List<Map>
            } else if ("UPDATE_USER_STATUS".equals(command)) {
                String sql = (String) input.readObject();
                int status = (Integer) input.readObject();
                String userName = (String) input.readObject();

                int result = server.updateUserStatus(sql, status, userName);

                // Send back response
                output.writeInt(1); // Response size
                output.writeObject(result);
            }
            // Original commands for notification to server
            else if ("LOGIN".equals(command)) {
                // Handle login
                String DateTime = (String) input.readObject();
                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                String accEmail = (String) input.readObject();
                String accHash = (String) input.readObject();
                Integer accPhoneNo = (Integer) input.readObject();
                Integer accStatus = (Integer) input.readObject();
                Integer accRight = (Integer) input.readObject();

                System.out.printf(
                        "\n\nLOGIN by(%s), accNo:%s, accName:%s, accEmail:%s, accHash:%s, accPhoneNo:%s, accStatus:%s accRight:%s",
                        DateTime, accNo + "", accName, accEmail, accHash, accPhoneNo + "", accStatus + "",
                        accRight + "");
            } else if ("LOGIN_Fail".equals(command)) {
                // Handle LOGIN_Fail
                String DateTime = (String) input.readObject();
                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                String accEmail = (String) input.readObject();
                String accHash = (String) input.readObject();
                Integer accPhoneNo = (Integer) input.readObject();
                Integer accStatus = (Integer) input.readObject();
                Integer accRight = (Integer) input.readObject();

                System.out.printf(
                        "\n\nLOGIN_Fail by(%s), accNo:%s, accName:%s, accEmail:%s, accHash:%s, accPhoneNo:%s, accStatus:%s accRight:%s",
                        DateTime, accNo + "", accName, accEmail, accHash, accPhoneNo + "", accStatus + "",
                        accRight + "");
            } else if ("REGISTER".equals(command)) {
                // Handle register
                String DateTime = (String) input.readObject();
                String accName = (String) input.readObject();
                String accHash = (String) input.readObject();
                String accEmail = (String) input.readObject();
                Integer accPhone = (Integer) input.readObject();
                System.out.printf("\n\nREGISTER by(%s), accName:%s, accHash:%s, accEmail:%s, accPhone%s", DateTime,
                        accName, accHash, accEmail, accPhone);

            } else if ("LOGOUT".equals(command)) {
                // Handle logout
                String DateTime = (String) input.readObject();
                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                String accEmail = (String) input.readObject();
                System.out.printf("\n\nLOGOUT by(%s), accNo:%s, accName:%s, accEmail:%s", DateTime, accNo, accName,
                        accEmail);

            } else if ("UPLOADFILE".equals(command)) {
                // Handle file upload
                String DateTime = (String) input.readObject();
                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                String accEmail = (String) input.readObject();
                String fileName = (String) input.readObject();
                byte[] fileContent = (byte[]) input.readObject();

                System.out.printf(
                        "\n\nUPLOAD_FILE by(%s), accNo:%s, accName:%s, accEmail:%s fileName:%s, fileContent:%s",
                        DateTime, accNo, accName, accEmail, fileName, "" + fileContent);

                String keyFilePath = "encryption_key.key";
                SecretKey secretKey = loadOrGenerateKey(keyFilePath);
                byte[] encryptedFileData = fileContent;
                byte[] decryptedFileData = decryptFile(encryptedFileData, secretKey);

                System.out.printf("\n  ->SYSTEM Decrypt file(%s): %s -> %s", DateTime, "(ENC," + fileContent + ")",
                        "(DEC," + decryptedFileData + ")");

            } else if ("DOWNLOADFILE".equals(command)) {
                // Handle file download
                String DateTime = (String) input.readObject();
                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                String accEmail = (String) input.readObject();
                String fileName = (String) input.readObject();
                byte[] fileContent = (byte[]) input.readObject();

                System.out.printf(
                        "\n\nDOWNLOAD_FILE by(%s), accNo:%s, accName:%s, accEmail:%s fileName:%s, fileContent:%s",
                        DateTime, accNo, accName, accEmail, fileName, "" + fileContent);

                String keyFilePath = "encryption_key.key";
                SecretKey secretKey = loadOrGenerateKey(keyFilePath);
                byte[] encryptedFileData = fileContent;
                byte[] decryptedFileData = decryptFile(encryptedFileData, secretKey);

                System.out.printf("\n  ->SYSTEM Decrypt file(%s): %s -> %s", DateTime, "(ENC," + fileContent + ")",
                        "(DEC," + decryptedFileData + "):");

            } else if ("EDITFILE".equals(command)) {
                String DateTime = (String) input.readObject();
                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                String accEmail = (String) input.readObject();
                Integer fileNo = (Integer) input.readObject();
                String fileName = (String) input.readObject();
                byte[] fileContent = (byte[]) input.readObject();

                System.out.printf(
                        "\n\nEDIT_FILE by(%s), accNo:%s, accName:%s, accEmail:%s, fileNo:%s, fileName:%s, fileContent:%s",
                        DateTime, accNo, accName, accEmail, fileNo, fileName, "" + fileContent);

                String keyFilePath = "encryption_key.key";
                SecretKey secretKey = loadOrGenerateKey(keyFilePath);
                byte[] decryptedFileData = decryptFile(fileContent, secretKey);

                System.out.printf("\n  ->SYSTEM Decrypt file(%s): %s -> %s", DateTime, "(ENC," + fileContent + ")",
                        "(DEC," + decryptedFileData + ")");

            } else if ("DELETEFILE".equals(command)) {
                String DateTime = (String) input.readObject();
                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                String accEmail = (String) input.readObject();
                Integer deletedfileNo = (Integer) input.readObject();

                System.out.printf(
                        "\n\nDELETE_FILE by(%s), accNo:%s, accName:%s, accEmail:%s, fileNo:%s",
                        DateTime, accNo, accName, accEmail, deletedfileNo);

            } else if ("SHAREFILE".equals(command)) {
                String DateTime = (String) input.readObject();
                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                String accEmail = (String) input.readObject();
                Integer shareFileNo = (Integer) input.readObject();
                String shareToUser = (String) input.readObject();

                System.out.printf(
                        "\n\nSHARE_FILE by(%s), accNo:%s, accName:%s, accEmail:%s, shareFileNo:%s, shareToUser:%s",
                        DateTime, accNo, accName, accEmail, shareFileNo, shareToUser);

            } else if ("UNSHAREFILE".equals(command)) {
                String DateTime = (String) input.readObject();
                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                String accEmail = (String) input.readObject();
                Integer unshareFileNo = (Integer) input.readObject();
                String unshareFromUser = (String) input.readObject();

                System.out.printf(
                        "\n\nUNSHARE_FILE by(%s), accNo:%s, accName:%s, accEmail:%s, unshareFileNo:%s, unshareFromUser:%s",
                        DateTime, accNo, accName, accEmail, unshareFileNo, unshareFromUser);

            } else if ("RESET_PASSWORD".equals(command)) {

                String DateTime = (String) input.readObject();
                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                String accEmail = (String) input.readObject();
                String accHash = (String) input.readObject();
                Integer accPhoneNo = (Integer) input.readObject();
                Integer accStatus = (Integer) input.readObject();
                Integer accRight = (Integer) input.readObject();

                System.out.printf(
                        "\n\nRESET_PASSWORD by(%s), accNo:%s, accName:%s, accEmail:%s, accHash:%s, accPhoneNo:%s, accStatus:%s accRight:%s",
                        DateTime, accNo + "", accName, accEmail, accHash, accPhoneNo + "", accStatus + "",
                        accRight + "");

            } else if ("Account_Lock".equals(command)) {

                String DateTime = (String) input.readObject();
                Integer adimnAccNo = (Integer) input.readObject();
                String adimnAccName = (String) input.readObject();

                Integer accNo = (Integer) input.readObject();
                String accName = (String) input.readObject();
                Integer accStatus = (Integer) input.readObject();

                System.out.printf(
                        "\n\nAccount_Lock (%s), adimnAccNo:%s, adimnAccName:%s, accNo:%s, accName:%s, accStatus:%s",
                        DateTime, adimnAccNo + "", adimnAccName, accNo + "", accName,
                        (accStatus == 0) ? accStatus + "(unlocked)" : accStatus + "(locked)");

            } else {
                // Fix: ObjectOutputStream doesn't have println method
                System.out.println("Unknown command received: " + command); // Log unknown command
                // Optionally send an error response back
                // output.writeInt(1);
                // output.writeObject("ERROR: Unknown command");
            }

        } catch (IOException | ClassNotFoundException | SQLException | NoSuchAlgorithmException
                | InterruptedException e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // Close resources in reverse order of creation
                if (server != null && server.conn != null && !server.conn.isClosed()) {
                    server.conn.close();
                }
                if (output != null) {
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
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

    // Helper method to convert ResultSet to List<Map<String, Object>>
    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        if (rs == null) {
            return list;
        }

        ResultSetMetaData md = null;
        int columns = 0;

        try {
            md = rs.getMetaData();
            columns = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>(columns);
                for (int i = 1; i <= columns; ++i) {
                    // Use column label which is usually case-insensitive or uppercase in Oracle
                    String columnName = md.getColumnLabel(i).toUpperCase();
                    Object value = rs.getObject(i);

                    // Special handling for non-serializable objects
                    if (value != null) {
                        if (value instanceof java.sql.Blob) {
                            java.sql.Blob blob = (java.sql.Blob) value;
                            value = blob.getBytes(1, (int) blob.length());
                        } else if (value instanceof java.sql.Clob) {
                            java.sql.Clob clob = (java.sql.Clob) value;
                            value = clob.getSubString(1, (int) clob.length());
                        } else if (!(value instanceof Serializable)) {
                            // Convert other non-serializable types to String
                            value = value.toString();
                        }
                    }

                    row.put(columnName, value);
                }
                list.add(row);
            }
        } finally {
            // Make sure to close the ResultSet to release any database resources
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing ResultSet: " + e.getMessage());
            }
        }
        return list;
    }

    // Helper method to check if an object is serializable
    private boolean isSerializable(Object obj) {
        return obj == null || obj instanceof Serializable;
    }

    // Update the decFile method to ensure proper handling of ResultSet and
    // Connection
    public List<Map<String, Object>> decFile(String sql, int selectedFileNo, int accNo) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        PreparedStatement pstmt = null;
        ResultSet rset = null;

        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, selectedFileNo);
            pstmt.setInt(2, accNo);
            pstmt.setInt(3, accNo);
            rset = pstmt.executeQuery();

            resultList = resultSetToList(rset); // This will close the ResultSet
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Close resources to prevent memory leaks and connection leaks
            try {
                if (pstmt != null)
                    pstmt.close();
                // ResultSet already closed in resultSetToList
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return resultList;
    }

    // Also update selectFile method for consistency
    public List<Map<String, Object>> selectFile(String sql, int accNo) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        PreparedStatement pstmt = null;
        ResultSet rset = null;

        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, accNo);
            rset = pstmt.executeQuery();

            resultList = resultSetToList(rset); // This will close the ResultSet
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Close resources
            try {
                if (pstmt != null)
                    pstmt.close();
                // ResultSet already closed in resultSetToList
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return resultList;
    }

    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int GCM_IV_LENGTH = 12; // 96 bits

    private static byte[] decryptFile(byte[] encryptedFileData, SecretKey secretKey) {
        if (encryptedFileData == null || encryptedFileData.length < GCM_IV_LENGTH) {
            System.err.println("Encrypted data is too short or null.");
            return null;
        }
        try {
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

    public String getHashPWD(String sql, String acc_name) {
        String rs = "";
        // Use try-with-resources for PreparedStatement and ResultSet
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, acc_name);
            try (ResultSet rset = pstmt.executeQuery()) {
                if (rset.next()) {
                    rs = rset.getString(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rs;
    }

    // Changed return type to List<Map<String, Object>>
    public List<Map<String, Object>> getAcc(String sql, String acc_name, String pwd) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, acc_name);
            pstmt.setString(2, pwd);
            try (ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset); // Convert ResultSet to List
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    // setLog doesn't return ResultSet, keep as void or return boolean/int for
    // success
    public void setLogVoid(String sql, int accNo, String formattedDate) {
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accNo);
            pstmt.setString(2, formattedDate);
            pstmt.executeUpdate(); // Use executeUpdate for INSERT
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean registeredEmail(String sql, String nc_email) {
        boolean rs = false;
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nc_email);
            try (ResultSet rset = pstmt.executeQuery()) {
                if (rset.next()) {
                    rs = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rs;
    }

    // registeredLoginName is not used by ClientService, but if needed, change
    // return type
    public List<Map<String, Object>> registeredLoginName(String sql, String nc_name) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nc_name);
            try (ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public void regisiterAccount(String sql, String nc_name, String nc_pwd, String nc_email, int nc_phoneNo) {
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nc_name);
            pstmt.setString(2, nc_pwd);
            pstmt.setString(3, nc_email);
            pstmt.setInt(4, nc_phoneNo);
            pstmt.executeUpdate(); // Use executeUpdate for INSERT
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Changed return type
    public List<Map<String, Object>> selectWithSharedFile(String sql, int accNo) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accNo);
            try (ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public boolean encryptedfile(String sql, int accNo, String fileName, byte[] encryptedFileData) {
        int rows = 0;
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accNo);
            pstmt.setString(2, fileName);
            pstmt.setBytes(3, encryptedFileData);
            rows = pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows > 0;
    }

    // Changed return type
    public List<Map<String, Object>> selectEditFile(String sql, int editFileNo, int accNo) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, editFileNo);
            pstmt.setInt(2, accNo);
            try (ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset);
            }

            // If no results found, log the parameters for debugging
            if (resultList.isEmpty()) {
                System.out.println("DEBUG: No file found with fileNo=" + editFileNo + " and accNo=" + accNo);
            }
        } catch (Exception e) {
            System.err.println("Error in selectEditFile: " + e.getMessage());
            e.printStackTrace();
        }
        return resultList;
    }

    public int updateEditedFile(String sql, byte[] encryptedEditedFileData, int editFileNo) {
        int rows = 0;
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBytes(1, encryptedEditedFileData);
            pstmt.setInt(2, editFileNo);
            rows = pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    // This method seems unused by ClientService, but changed return type if needed
    public List<Map<String, Object>> shareFile(String sql, int shareFileNo, int accNo) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, shareFileNo);
            pstmt.setInt(2, accNo);
            try (ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    // Changed return type
    public List<Map<String, Object>> checkAlreadySharedFile(String sql, int shareFileNo, String shareToUser) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, shareFileNo);
            pstmt.setString(2, shareToUser);
            try (ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public int sharedFile(String sql, int shareFileNo, String shareToUser) {
        int rows = 0;
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, shareFileNo);
            pstmt.setString(2, shareToUser);
            rows = pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    public int unshareFile(String sql, int unshareFileNo, String unshareFromUser) {
        int rows = 0;
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, unshareFileNo);
            pstmt.setString(2, unshareFromUser);
            rows = pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    // Changed return type
    public List<Map<String, Object>> checkFileOwner(String sql, int fileNo, int accNo) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fileNo);
            pstmt.setInt(2, accNo);
            try (ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public void deleteFromShareFile(String sql, int deleteFileNo) {
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, deleteFileNo);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int deleteFromAccFile(String sql, int deleteFileNo) {
        int rows = 0;
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, deleteFileNo);
            rows = pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    // Changed return type
    public List<Map<String, Object>> getPWD(String sql, String getLoginName) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, getLoginName);
            try (ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public int updateHashPWD(String sql, String new_hashed_pwd, String getLoginName) {
        int rows = 0;
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, new_hashed_pwd);
            pstmt.setString(2, getLoginName);
            rows = pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    // Changed return type
    public List<Map<String, Object>> selectAllLog(String sql) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources, assuming conn is managed appropriately elsewhere or
        // reopened if closed
        try {
            if (conn == null || conn.isClosed()) {
                // Re-establish connection if closed - adjust credentials as needed
                conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                    ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset);
            }
        } catch (Exception e) { // Removed redundant SQLException
            e.printStackTrace();
        }
        return resultList;
    }

    // Changed return type
    public List<Map<String, Object>> displayLog(String sql, int accNo) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accNo);
            try (ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    // Changed return type
    public List<Map<String, Object>> getUserStatus(String sql, String userName) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            try (ResultSet rset = pstmt.executeQuery()) {
                resultList = resultSetToList(rset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public int updateUserStatus(String sql, int updateUserStatus, String userName) {
        int rows = 0;
        // Use try-with-resources
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, updateUserStatus);
            pstmt.setString(2, userName);
            rows = pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }
}