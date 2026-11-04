import java.nio.file.Path;
import java.security.*;
import java.sql.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Account storage for a local coursework application, not a remote identity service. */
public final class Accounts {
    private static final int ITERATIONS = 600_000;
    private final Path database;
    public Accounts(Path database) { this.database = database; }

    private static String username(String input) {
        if (input == null || !input.trim().matches("[A-Za-z0-9_.-]{3,40}"))
            throw new IllegalArgumentException("Username: 3–40 letters, digits, underscores, dots or hyphens.");
        return input.trim().toLowerCase(Locale.ROOT);
    }
    private static byte[] derive(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
    private static void password(char[] value) {
        if (value == null || value.length < 12 || value.length > 128)
            throw new IllegalArgumentException("Use a password between 12 and 128 characters.");
    }
    public void register(String user, String name, char[] secret) throws SQLException, GeneralSecurityException {
        user = username(user);
        password(secret);
        if (name == null || name.isBlank() || name.trim().length() > 100 || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Enter a display name of 1–100 characters.");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String hash = "pbkdf2-sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derive(secret, salt));
        try (Connection connection = JavaConnect.open(database);
             PreparedStatement query = connection.prepareStatement("INSERT INTO accounts_v2(username,display_name,password_hash) VALUES(?,?,?)")) {
            query.setString(1, user); query.setString(2, name.trim()); query.setString(3, hash);
            try { query.executeUpdate(); }
            catch (SQLException error) {
                if (error.getErrorCode() == 19) throw new IllegalArgumentException("That username is already registered.");
                throw error;
            }
        }
    }
    public Optional<String> login(String user, char[] secret) throws SQLException, GeneralSecurityException {
        user = username(user);
        password(secret);
        String name = null, encoded = null;
        try (Connection connection = JavaConnect.open(database);
             PreparedStatement query = connection.prepareStatement("SELECT display_name,password_hash FROM accounts_v2 WHERE username=?")) {
            query.setString(1,user);
            try (ResultSet rows = query.executeQuery()) {
                if (rows.next()) { name=rows.getString(1); encoded=rows.getString(2); }
            }
        }
        // Missing accounts still do one derivation. Only the versioned hash format is accepted.
        if (encoded == null) { derive(secret,new byte[16]); return Optional.empty(); }
        String[] pieces = encoded.split("\\$",-1);
        if (pieces.length != 4 || !pieces[0].equals("pbkdf2-sha256") || !pieces[1].equals(Integer.toString(ITERATIONS)))
            throw new SQLException("Unsupported account password format");
        try {
            byte[] salt=Base64.getDecoder().decode(pieces[2]), expected=Base64.getDecoder().decode(pieces[3]);
            if (salt.length!=16 || expected.length!=32) throw new IllegalArgumentException();
            return MessageDigest.isEqual(expected,derive(secret,salt)) ? Optional.of(name) : Optional.empty();
        } catch (IllegalArgumentException error) { throw new SQLException("Invalid account password format", error); }
    }
}
