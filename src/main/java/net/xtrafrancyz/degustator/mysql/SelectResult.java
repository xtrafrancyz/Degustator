package net.xtrafrancyz.degustator.mysql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xtrafrancyz
 */
public class SelectResult {
    List<Column> columns;
    List<Row> rows;
    private Map<String, Integer> columnIndexes = null;
    
    SelectResult() {
        
    }
    
    public int getColumnIndex(String name) {
        if (columnIndexes == null) {
            columnIndexes = new HashMap<>();
            for (int i = 0; i < columns.size(); i++)
                columnIndexes.put(columns.get(i).name, i);
        }
        Integer index = columnIndexes.get(name);
        if (index == null)
            throw new IllegalArgumentException("Field " + name + " not found");
        return index;
    }
    
    public boolean isEmpty() {
        return rows.isEmpty();
    }
    
    public int getRowCount() {
        return rows.size();
    }
    
    public List<Row> getRows() {
        return rows;
    }
    
    public List<Column> getColumns() {
        return columns;
    }
    
    public Row getFirst() {
        return rows.isEmpty() ? null : rows.get(0);
    }
}
