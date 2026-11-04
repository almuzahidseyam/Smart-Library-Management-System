import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/** Local database boundary. Failures propagate; callers never receive null. */
public final class JavaConnect {
    public static Path databasePath() {
        String override = System.getenv("LIBRARY_DB_PATH");
        return (override == null || override.isBlank()
            ? Path.of(System.getProperty("user.home"), ".smart-library", "library.db")
            : Path.of(override)).toAbsolutePath().normalize();
    }

    public static Connection open(Path path) throws SQLException {
        try {
            Path absolute = path.toAbsolutePath().normalize();
            Files.createDirectories(absolute.getParent());
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + absolute);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA busy_timeout=3000");
                statement.execute("CREATE TABLE IF NOT EXISTS accounts_v2 (username TEXT PRIMARY KEY, display_name TEXT NOT NULL, password_hash TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            } catch (SQLException error) {
                connection.close();
                throw error;
            }
            return connection;
        } catch (java.io.IOException error) {
            throw new SQLException("Cannot create the database directory", error);
        }
    }

    public static Connection ConnecrDb() throws SQLException { return open(databasePath()); }
}
