package intelligentBoxClient.ss.dao;

import java.sql.*;

/**
 * Created by yaohx on 3/22/2016.
 */
public class SqliteContext {
    private Connection c = null;

    public void connect(String dbFile) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        c = DriverManager.getConnection("jdbc:sqlite:" + dbFile);

        System.out.println("Opened database successfully");
    }

    public void createDirectoryTable()
    {
        String sql = "create tabel Directory "
                + "...";
    }
    public void disconnect() {

        try {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public void query() throws SQLException {
        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM tbl1;");
        while (rs.next()) {
            String one = rs.getString("one");
            int two = rs.getInt("two");
            System.out.println("one = " + one);
            System.out.println("two = " + two);
            System.out.println();
        }
        rs.close();
        stmt.close();
    }

    public void tx_insert() throws SQLException {
        c.setAutoCommit(false);
        //c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        Statement stmt = c.createStatement();
        stmt.execute(
                "Insert into tbl1 (one, two) values " +
                        "(\"world\", 30);");
        c.rollback();
        stmt.close();
        c.setAutoCommit(true);
    }

    public void insert() throws SQLException {
        Statement stmt = c.createStatement();
        stmt.execute(
                "Insert into tbl1 (one, two) values " +
                        "(\"world\", 40);");

        stmt.close();
    }
}
