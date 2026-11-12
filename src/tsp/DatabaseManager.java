package tsp;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages MySQL database connection, schema creation, and TSP result logging.
 */
public class DatabaseManager {

    // ─── CONFIGURE YOUR DATABASE HERE ───────────────────────────────────────
    private static final String DB_HOST   = "localhost";
    private static final String DB_PORT   = "3306";
    private static final String DB_NAME   = "tsp_db";
    private static final String DB_USER   = "root";
    private static final String DB_PASS   = "****"; // change this
    // ────────────────────────────────────────────────────────────────────────

    private static final String URL =
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private Connection connection;

    public DatabaseManager() throws TSPException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, DB_USER, DB_PASS);
            initializeSchema();
        } catch (ClassNotFoundException e) {
            throw new TSPException(TSPException.ErrorType.DATABASE_ERROR,
                "MySQL JDBC Driver not found. Add mysql-connector-java.jar to classpath.");
        } catch (SQLException e) {
             e.printStackTrace();
            throw new TSPException(TSPException.ErrorType.DATABASE_ERROR,
                "Cannot connect to MySQL: " + e.getMessage()
                + "\nVerify DB_HOST, DB_USER, DB_PASS and that MySQL is running.");
        }
    }

    /** Create tables if they don't exist */
    private void initializeSchema() throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS tsp_sessions (
                id            INT AUTO_INCREMENT PRIMARY KEY,
                session_time  DATETIME NOT NULL,
                num_nodes     INT NOT NULL,
                optimal_cost  INT NOT NULL,
                optimal_path  VARCHAR(500) NOT NULL,
                solve_time_ms LONG NOT NULL,
                status        VARCHAR(50) DEFAULT 'SUCCESS'
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS tsp_distance_matrix (
                id          INT AUTO_INCREMENT PRIMARY KEY,
                session_id  INT NOT NULL,
                from_node   INT NOT NULL,
                to_node     INT NOT NULL,
                distance    INT NOT NULL,
                FOREIGN KEY (session_id) REFERENCES tsp_sessions(id) ON DELETE CASCADE
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS tsp_error_logs (
                id           INT AUTO_INCREMENT PRIMARY KEY,
                log_time     DATETIME NOT NULL,
                error_type   VARCHAR(100),
                error_message VARCHAR(1000)
            )
        """);

        stmt.close();
    }

    /** Save a successful TSP result and its distance matrix */
    public int saveSession(int numNodes, int[][] matrix, String path, int cost, long solveTimeMs)
            throws TSPException {
        try {
            String sql = "INSERT INTO tsp_sessions (session_time, num_nodes, optimal_cost, " +
                         "optimal_path, solve_time_ms, status) VALUES (?, ?, ?, ?, ?, 'SUCCESS')";
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.setInt(2, numNodes);
            ps.setInt(3, cost);
            ps.setString(4, path);
            ps.setLong(5, solveTimeMs);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            int sessionId = -1;
            if (rs.next()) sessionId = rs.getInt(1);

            // Save distance matrix
            String matSql = "INSERT INTO tsp_distance_matrix (session_id, from_node, to_node, distance) " +
                            "VALUES (?, ?, ?, ?)";
            PreparedStatement matPs = connection.prepareStatement(matSql);
            for (int i = 0; i < numNodes; i++) {
                for (int j = 0; j < numNodes; j++) {
                    matPs.setInt(1, sessionId);
                    matPs.setInt(2, i);
                    matPs.setInt(3, j);
                    matPs.setInt(4, matrix[i][j]);
                    matPs.addBatch();
                }
            }
            matPs.executeBatch();
            matPs.close();
            ps.close();
            return sessionId;

        } catch (SQLException e) {
            throw new TSPException(TSPException.ErrorType.DATABASE_ERROR,
                "Failed to save session: " + e.getMessage());
        }
    }

    /** Log an error to the database */
    public void logError(TSPException ex) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO tsp_error_logs (log_time, error_type, error_message) VALUES (?, ?, ?)"
            );
            ps.setString(1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.setString(2, ex.getErrorType().getLabel());
            ps.setString(3, ex.getMessage());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ignored) {}
    }

    /** Fetch last N sessions for the history panel */
    public List<String[]> getRecentSessions(int limit) throws TSPException {
        List<String[]> rows = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT id, session_time, num_nodes, optimal_cost, optimal_path, solve_time_ms " +
                "FROM tsp_sessions ORDER BY id DESC LIMIT ?"
            );
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("session_time"),
                    String.valueOf(rs.getInt("num_nodes")),
                    String.valueOf(rs.getInt("optimal_cost")),
                    rs.getString("optimal_path"),
                    rs.getLong("solve_time_ms") + " ms"
                });
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            throw new TSPException(TSPException.ErrorType.DATABASE_ERROR,
                "Failed to load history: " + e.getMessage());
        }
        return rows;
    }

    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException ignored) {}
    }

    public boolean isConnected() {
        try { return connection != null && !connection.isClosed(); }
        catch (SQLException e) { return false; }
    }
}
