import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SerializedResultSet implements Serializable {
    // Make sure to add serialVersionUID for better serialization control
    private static final long serialVersionUID = 1L;
    
    private List<Map<String, Object>> rows;
    private List<String> columnNames;
    private int currentRow = -1;

    public SerializedResultSet(ResultSet rs) throws SQLException {
        rows = new ArrayList<>();
        columnNames = new ArrayList<>();
        
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // Store column names
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            
            // Store data
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    
                    // Handle non-serializable objects
                    if (value != null && !(value instanceof Serializable)) {
                        // Convert specific types as needed
                        if (value instanceof java.sql.Blob) {
                            java.sql.Blob blob = (java.sql.Blob) value;
                            value = blob.getBytes(1, (int) blob.length());
                        } else {
                            // Convert other non-serializable objects to String
                            value = value.toString();
                        }
                    }
                    
                    row.put(columnName, value);
                }
                rows.add(row);
            }
        } finally {
            // Don't close the ResultSet here as it's the caller's responsibility
            // The caller should close it after creating this SerializedResultSet
        }
    }
    
    public boolean next() {
        if (currentRow < rows.size() - 1) {
            currentRow++;
            return true;
        }
        return false;
    }
    
    public Object getObject(String columnName) {
        if (currentRow >= 0 && currentRow < rows.size()) {
            return rows.get(currentRow).get(columnName);
        }
        return null;
    }
    
    public Object getObject(int columnIndex) {
        if (currentRow >= 0 && currentRow < rows.size()) {
            return rows.get(currentRow).get(columnNames.get(columnIndex - 1));
        }
        return null;
    }
    
    public String getString(String columnName) {
        Object value = getObject(columnName);
        return value != null ? value.toString() : null;
    }
    
    public String getString(int columnIndex) {
        Object value = getObject(columnIndex);
        return value != null ? value.toString() : null;
    }
    
    public int getInt(String columnName) {
        Object value = getObject(columnName);
        return value != null ? ((Number) value).intValue() : 0;
    }
    
    public int getInt(int columnIndex) {
        Object value = getObject(columnIndex);
        return value != null ? ((Number) value).intValue() : 0;
    }
    
    public byte[] getBytes(String columnName) {
        return (byte[]) getObject(columnName);
    }
    
    public void close() {
        // Nothing to do
    }
}
