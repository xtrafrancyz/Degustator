package net.xtrafrancyz.degustator.mysql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author xtrafrancyz
 */
public interface PreparedStatementFiller {
    void fill(PreparedStatement statement) throws SQLException;
}
