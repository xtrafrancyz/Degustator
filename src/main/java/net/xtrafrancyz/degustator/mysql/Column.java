package net.xtrafrancyz.degustator.mysql;

/**
 * @author xtrafrancyz
 */
public class Column {
    final String name;
    
    Column(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
