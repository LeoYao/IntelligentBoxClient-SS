package intelligentBoxClient.ss.dao;

import intelligentBoxClient.ss.bootstrapper.IConfiguration;
import intelligentBoxClient.ss.dao.pojo.LruEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Leo on 4/7/16.
 */
public class LruDbContext extends SqliteContext {

    public static final String HEADER = ".header";
    public static final String TAIL = ".tail";

    private PreparedStatement _selectStatement;
    private PreparedStatement _updateStatement;
    private PreparedStatement _insertStatement;
    private PreparedStatement _deleteStatement;

    public LruDbContext(IConfiguration configuration) {
        super(configuration);
    }

    public LruEntity query(String curr) throws SQLException {
        _selectStatement.setString(1, curr);

        ResultSet rs = null;
        LruEntity result = null;
        try {
            rs = _selectStatement.executeQuery();
            if (rs.next()){
                result = new LruEntity();
                result.setCurr(rs.getString("curr"));
                result.setPrev(rs.getString("prev"));
                result.setNext(rs.getString("next"));
            }
        }
        finally {
            if (rs != null && !rs.isClosed()){
                rs.close();
            }
        }

        return result;
    }

    public int insert(LruEntity entity) throws SQLException {
        int affectedRowCnt = 0;

        _insertStatement.setString(1, entity.getCurr());
        _insertStatement.setString(2, entity.getPrev());
        _insertStatement.setString(3, entity.getNext());

        affectedRowCnt = _insertStatement.executeUpdate();

        return affectedRowCnt;
    }

    public int update(LruEntity entity) throws SQLException {
        int affectedRowCnt = 0;

        _updateStatement.setString(1, entity.getPrev());
        _updateStatement.setString(2, entity.getNext());
        _updateStatement.setString(3, entity.getCurr());

        affectedRowCnt = _updateStatement.executeUpdate();

        return affectedRowCnt;
    }

    public int delete(String curr) throws SQLException {
        int affectedRowCnt = 0;

        _deleteStatement.setString(1, curr);

        affectedRowCnt = _deleteStatement.executeUpdate();

        return affectedRowCnt;
    }

    @Override
    protected boolean prepareStatements() {
        try {
            _selectStatement =
                    _connection.prepareStatement("SELECT   curr\n" +
                            "       , prev\n" +
                            "       , next\n" +
                            "  FROM lru_queue\n" +
                            " WHERE curr = ?;");

            _insertStatement = _connection.prepareStatement
                    ("INSERT INTO lru_queue\n" +
                            "( curr\n" +
                            ", prev\n" +
                            ", next)\n" +
                            "VALUES (?,?,?);");

            _updateStatement = _connection.prepareStatement
                    ("UPDATE lru_queue\n" +
                            "SET prev = ?\n" +
                            ", next = ?\n" +
                            "WHERE curr = ?;");

            _deleteStatement = _connection.prepareStatement("DELETE FROM lru_queue WHERE curr = ?;");
        }
        catch (SQLException e)
        {
            logger.error("Failed to prepare statements.", e);
            return false;
        }

        return true;
    }

    public LruEntity pop() throws SQLException {
        LruEntity result = null;
        boolean inTransaction = false;
        boolean inError = true;
        try {
            beginTransaction();

            LruEntity header = query(HEADER);
            if (header == null || header.getNext() == TAIL) {
                return null;
            }

            result = query(header.getNext());
            header.setNext(result.getNext());
            LruEntity next = query(result.getNext());
            next.setPrev(HEADER);

            update(header);
            update(next);
            delete(result.getCurr());
            inError = false;
        }
        finally {
            if (inTransaction){
                if(inError){
                    rollbackTransaction();
                } else {
                    commitTransaction();
                }
            }
        }

        return result;
    }


    @Override
    protected boolean disposeStatements() {
        try {
            if (_selectStatement != null && !_selectStatement.isClosed()) {
                _selectStatement.close();
            }
            if (_insertStatement != null && !_insertStatement.isClosed()) {
                _insertStatement.close();
            }
            if (_updateStatement != null && !_updateStatement.isClosed()) {
                _updateStatement.close();
            }
            if (_deleteStatement != null && !_deleteStatement.isClosed()) {
                _deleteStatement.close();
            }
        }
        catch (SQLException e)
        {
            logger.error("Failed to close statements.", e);
            return false;
        }

        return true;
    }

    @Override
    protected boolean initDb() {
        try {
            createLruTable();
        }
        catch (SQLException e) {
            logger.error("Failed to initialized DB.", e);
            return false;
        }
        return true;
    }

    private void createLruTable() throws SQLException {

        String sql = "create table if not exists Lru \n" +
                "(curr varchar(4000) PRIMARY KEY,\n" +
                " prev varchar(4000), \n" +
                " next varchar(4000));";

        executeSql(sql);

    }
}
