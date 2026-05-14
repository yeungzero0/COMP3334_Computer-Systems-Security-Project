import java.io.*;
import java.net.Socket;
import java.sql.ResultSet; // Keep for potential future use, but methods won't return it
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Import Map

public class ClientService {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    // Method to send command and data to server and receive response
    private static Object[] sendRequest(String command, Object... params) {
        Socket socket = null;
        ObjectOutputStream output = null;
        ObjectInputStream input = null;
        
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush(); // Flush the stream header to prevent blocking
            input = new ObjectInputStream(socket.getInputStream());

            // Send command
            output.writeObject(command);

            // Send parameters
            for (Object param : params) {
                output.writeObject(param);
            }
            output.flush();

            // Get response
            int responseSize = input.readInt();
            Object[] response = new Object[responseSize];
            for (int i = 0; i < responseSize; i++) {
                response[i] = input.readObject();
            }

            return response;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("ClientService Error: " + e.getMessage());
            return null; // Return null instead of propagating the exception
        } finally {
            // Close resources in reverse order of creation
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    // Get hashed password from server
    public static String getHashPWD(String sql, String acc_name) {
        Object[] response = sendRequest("GET_HASH_PWD", sql, acc_name);
        if (response != null && response.length > 0 && response[0] instanceof String) {
            return (String) response[0];
        }
        return "";
    }

    // Get account details - Changed return type
    @SuppressWarnings("unchecked") // Suppress warning for casting Object to List<Map<String, Object>>
    public static List<Map<String, Object>> getAcc(String sql, String acc_name, String pwd) {
        Object[] response = sendRequest("GET_ACC", sql, acc_name, pwd);
        if (response != null && response.length > 0 && response[0] instanceof List) {
             // It's generally safer to check the list elements too, but for now, cast directly
            return (List<Map<String, Object>>) response[0];
        }
        return new ArrayList<>(); // Return empty list instead of null
    }

    // Log an event
    public static void setLogVoid(String sql, int accNo, String formattedDate) {
        sendRequest("SET_LOG", sql, accNo, formattedDate);
    }

    // Check if email is registered
    public static boolean registeredEmail(String sql, String email) {
        Object[] response = sendRequest("CHECK_EMAIL", sql, email);
        if (response != null && response.length > 0 && response[0] instanceof Boolean) {
            return (Boolean) response[0];
        }
        return false;
    }

    // REGISTER
    public static boolean regUser(String currentTimestamp, String loginName, String pwd, String email, int phone) {
        Object[] response = sendRequest("REGISTER", currentTimestamp, loginName, pwd, email, phone);
        if (response != null && response.length > 0 && response[0] instanceof Boolean) {
            return (Boolean) response[0];
        }
        return false;
    }

    // Register a new account
    public static void regisiterAccount(String sql, String name, String pwd, String email, int phoneNo) {
        sendRequest("REGISTER_ACCOUNT", sql, name, pwd, email, phoneNo);
    }

    // Get user's files - Changed return type
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> selectFile(String sql, int accNo) {
        Object[] response = sendRequest("SELECT_FILES", sql, accNo);
         if (response != null && response.length > 0 && response[0] instanceof List) {
            return (List<Map<String, Object>>) response[0];
        }
        return new ArrayList<>();
    }

    // Get files shared with user - Changed return type
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> selectWithSharedFile(String sql, int accNo) {
        Object[] response = sendRequest("SELECT_SHARED_FILES", sql, accNo);
         if (response != null && response.length > 0 && response[0] instanceof List) {
            return (List<Map<String, Object>>) response[0];
        }
        return new ArrayList<>();
    }

    // Upload encrypted file
    public static boolean encryptedfile(String sql, int accNo, String fileName, byte[] encryptedFileData) {
        Object[] response = sendRequest("UPLOAD_FILE", sql, accNo, fileName, encryptedFileData);
        if (response != null && response.length > 0 && response[0] instanceof Boolean) {
            return (Boolean) response[0];
        }
        return false;
    }

    // Upload file
    public static boolean uploadFile(String currentTimestamp, int accNo, String loginName, String email, String fileName, byte[] encryptedFileData) {
        Object[] response = sendRequest("UPLOADFILE", currentTimestamp, accNo, loginName, email, fileName, encryptedFileData);
        if (response != null && response.length > 0 && response[0] instanceof Boolean) {
            return (Boolean) response[0];
        }
        return false;
    }

    // Download file - Changed return type
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> decFile(String sql, int selectedFileNo, int accNo) {
        Object[] response = sendRequest("DOWNLOAD_FILE", sql, selectedFileNo, accNo);
         if (response != null && response.length > 0 && response[0] instanceof List) {
            return (List<Map<String, Object>>) response[0];
        }
        System.err.println("Debug: Failed to get file data for fileNo=" + selectedFileNo);
        return new ArrayList<>();
    }

    // download file
    public static boolean downloadFile(String currentTimestamp, int accNo, String loginName, String email, String fileName, byte[] encryptedFileData) {
        Object[] response = sendRequest("DOWNLOADFILE", currentTimestamp, accNo, loginName, email, fileName, encryptedFileData);
        if (response != null && response.length > 0 && response[0] instanceof Boolean) {
            return (Boolean) response[0];
        }
        return false;
    }

    // edit file
    public static boolean editFile(String currentTimestamp, int accNo, String loginName, String email, int editFileNo, String fileName, byte[] encryptedFileData) {
        Object[] response = sendRequest("EDITFILE", currentTimestamp, accNo, loginName, email, editFileNo, fileName, encryptedFileData);
        if (response != null && response.length > 0 && response[0] instanceof Boolean) {
            return (Boolean) response[0];
        }
        return false;
    }

    // Get file for editing - Changed return type
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> selectEditFile(String sql, int editFileNo, int accNo) {
        Object[] response = sendRequest("SELECT_EDIT_FILE", sql, editFileNo, accNo);
         if (response != null && response.length > 0 && response[0] instanceof List) {
            return (List<Map<String, Object>>) response[0];
        }
        return new ArrayList<>();
    }

    // Update edited file
    public static int updateEditedFile(String sql, byte[] encryptedEditedFileData, int editFileNo) {
        Object[] response = sendRequest("UPDATE_EDITED_FILE", sql, encryptedEditedFileData, editFileNo);
        if (response != null && response.length > 0 && response[0] instanceof Integer) {
            return (Integer) response[0];
        }
        return 0;
    }

    // Check file ownership - Changed return type
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> checkFileOwner(String sql, int fileNo, int accNo) {
        Object[] response = sendRequest("CHECK_FILE_OWNER", sql, fileNo, accNo);
         if (response != null && response.length > 0 && response[0] instanceof List) {
            return (List<Map<String, Object>>) response[0];
        }
        return new ArrayList<>();
    }

    // Check if file is already shared - Changed return type
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> checkAlreadySharedFile(String sql, int fileNo, String userName) {
        Object[] response = sendRequest("CHECK_ALREADY_SHARED", sql, fileNo, userName);
         if (response != null && response.length > 0 && response[0] instanceof List) {
            return (List<Map<String, Object>>) response[0];
        }
        return new ArrayList<>();
    }


    // SHAREFILE
    public static boolean shareFile(String currentTimestamp, int accNo, String loginName, String email, int shareFileNo, String shareToUser) {
        Object[] response = sendRequest("SHAREFILE", currentTimestamp, accNo, loginName, email, shareFileNo, shareToUser);
        if (response != null && response.length > 0 && response[0] instanceof Boolean) {
            return (Boolean) response[0];
        }
        return false;
    }

    // Share file with user
    public static int sharedFile(String sql, int fileNo, String userName) {
        Object[] response = sendRequest("SHARE_FILE", sql, fileNo, userName);
        if (response != null && response.length > 0 && response[0] instanceof Integer) {
            return (Integer) response[0];
        }
        return 0;
    }

    // UNSHAREFILE
    public static boolean unshareFile(String currentTimestamp, int accNo, String loginName, String email, int unshareFileNo, String unshareFromUser) {
        Object[] response = sendRequest("UNSHAREFILE", currentTimestamp, accNo, loginName, email, unshareFileNo, unshareFromUser);
        if (response != null && response.length > 0 && response[0] instanceof Boolean) {
            return (Boolean) response[0];
        }
        return false;
    }

    // Unshare file
    public static int unshareFile(String sql, int fileNo, String userName) {
        Object[] response = sendRequest("UNSHARE_FILE", sql, fileNo, userName);
        if (response != null && response.length > 0 && response[0] instanceof Integer) {
            return (Integer) response[0];
        }
        return 0;
    }

    // DELETEFILE
    public static boolean deleteFile(String currentTimestamp, int accNo, String loginName, String email, int deleteFile) {
        Object[] response = sendRequest("DELETEFILE", currentTimestamp, accNo, loginName, email, deleteFile);
        if (response != null && response.length > 0 && response[0] instanceof Boolean) {
            return (Boolean) response[0];
        }
        return false;
    }

    // Delete file from share table
    public static void deleteFromShareFile(String sql, int fileNo) {
        sendRequest("DELETE_FROM_SHARE", sql, fileNo);
    }

    // Delete file
    public static int deleteFromAccFile(String sql, int fileNo) {
        Object[] response = sendRequest("DELETE_FILE", sql, fileNo);
        if (response != null && response.length > 0 && response[0] instanceof Integer) {
            return (Integer) response[0];
        }
        return 0;
    }

    // RESET_PASSWORD
    public static boolean resetPWD(String currentTimestamp, int accNo, String loginName, String email,String hash, int phone, int userStatus, int userRight) {
        Object[] response = sendRequest("RESET_PASSWORD", currentTimestamp, accNo, loginName, email, hash, phone, userStatus, userRight);
        if (response != null && response.length > 0 && response[0] instanceof Boolean) {
            return (Boolean) response[0];
        }
        return false;
    }

    // Get password - Changed return type
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getPWD(String sql, String loginName) {
        Object[] response = sendRequest("GET_PWD", sql, loginName);
         if (response != null && response.length > 0 && response[0] instanceof List) {
            return (List<Map<String, Object>>) response[0];
        }
        return new ArrayList<>();
    }

    // Update password
    public static int updateHashPWD(String sql, String newPwd, String loginName) {
        Object[] response = sendRequest("UPDATE_PWD", sql, newPwd, loginName);
        if (response != null && response.length > 0 && response[0] instanceof Integer) {
            return (Integer) response[0];
        }
        return 0;
    }

    // Get all logs - Changed return type
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> selectAllLog(String sql) {
        Object[] response = sendRequest("SELECT_ALL_LOGS", sql);
         if (response != null && response.length > 0 && response[0] instanceof List) {
            return (List<Map<String, Object>>) response[0];
        }
        return new ArrayList<>();
    }

    // Get user name for logs - Changed return type
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> displayLog(String sql, int accNo) {
        Object[] response = sendRequest("DISPLAY_LOG", sql, accNo);
         if (response != null && response.length > 0 && response[0] instanceof List) {
            return (List<Map<String, Object>>) response[0];
        }
        return new ArrayList<>();
    }

    // Get user status - Changed return type
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getUserStatus(String sql, String userName) {
        Object[] response = sendRequest("GET_USER_STATUS", sql, userName);
         if (response != null && response.length > 0 && response[0] instanceof List) {
            return (List<Map<String, Object>>) response[0];
        }
        return new ArrayList<>();
    }

    // Update user status
    public static int updateUserStatus(String sql, int status, String userName) {
        Object[] response = sendRequest("UPDATE_USER_STATUS", sql, status, userName);
        if (response != null && response.length > 0 && response[0] instanceof Integer) {
            return (Integer) response[0];
        }
        return 0;
    }

    // Add a helper to map user-selected index to the correct fileNo
    public static int mapIndexToFileNo(int index) {
        return switch (index) {
            case 1 -> 25;
            case 2 -> 26;
            case 3 -> 18;
            case 4 -> 19;
            case 5 -> 27; // Maps index "5" to fileNo 27
            case 6 -> 31;
            default -> -1;
        };
    }

    public static List<Map<String, Object>> selectEditFileByIndex(String sql, int fileIndex, int accNo) {
        int fileNo = mapIndexToFileNo(fileIndex);
        if (fileNo < 0) {
            System.err.println("Invalid file index: " + fileIndex);
            return new ArrayList<>();
        }
        return selectEditFile(sql, fileNo, accNo);
    }
}
