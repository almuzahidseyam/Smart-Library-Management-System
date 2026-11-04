import java.nio.file.*;
import java.sql.*;

public class AccountsTest {
    interface Action { void run() throws Exception; }
    static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
    static void rejects(Action action) throws Exception {
        try { action.run(); throw new AssertionError("Expected validation failure"); }
        catch (IllegalArgumentException expected) {}
    }
    public static void main(String[] args) throws Exception {
        Path directory=Files.createTempDirectory("library-tests-");
        Path database=directory.resolve("nested/library.db");
        Accounts accounts=new Accounts(database);
        char[] secret="testing-only long password".toCharArray();
        accounts.register("Reader_1","Reader One",secret);
        check(accounts.login("READER_1",secret).orElseThrow().equals("Reader One"),"Sign in after registration");
        check(new Accounts(database).login("reader_1",secret).isPresent(),"Persistence across service instances");
        check(accounts.login("reader_1","incorrect password".toCharArray()).isEmpty(),"Wrong password");
        check(accounts.login("unknown",secret).isEmpty(),"Unknown account");
        rejects(() -> accounts.register("READER_1","Other",secret));
        rejects(() -> accounts.register("x' OR 1=1--","Other",secret));
        rejects(() -> accounts.register("reader2","Other","short".toCharArray()));
        rejects(() -> accounts.register("reader2","",secret));
        accounts.register("reader2","O'Neil",secret);
        try(Connection connection=JavaConnect.open(database); Statement statement=connection.createStatement()) {
            try(ResultSet rows=statement.executeQuery("SELECT password_hash FROM accounts_v2 ORDER BY username")) {
                rows.next(); String first=rows.getString(1); rows.next(); String second=rows.getString(1);
                check(first.startsWith("pbkdf2-sha256$600000$"),"Versioned password hash");
                check(!first.contains(new String(secret)) && !first.equals(second),"Salted, no plaintext");
            }
            statement.execute("CREATE TABLE Account (Username TEXT, Password TEXT)");
            statement.execute("INSERT INTO Account VALUES('legacy','old plaintext')");
            check(accounts.login("legacy",secret).isEmpty(),"Legacy plaintext not accepted");
            try(ResultSet rows=statement.executeQuery("SELECT COUNT(*) FROM Account")) { rows.next(); check(rows.getInt(1)==1,"Legacy data preserved"); }
            statement.execute("UPDATE accounts_v2 SET password_hash='broken' WHERE username='reader2'");
        }
        try { accounts.login("reader2",secret); throw new AssertionError("Corrupt hash accepted"); } catch(SQLException expected) {}
        Path regularFile=directory.resolve("not-a-directory"); Files.writeString(regularFile,"fixture");
        try { JavaConnect.open(regularFile.resolve("bad.db")); throw new AssertionError("Invalid database path accepted"); } catch(SQLException expected) {}
        Files.delete(database); Files.delete(database.getParent()); Files.delete(regularFile); Files.delete(directory);
        System.out.println("14 account/database checks passed against real SQLite.");
    }
}
