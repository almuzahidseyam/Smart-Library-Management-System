# Smart Library · account prototype

A small Java Swing coursework application with working local account creation, sign-in and sign-out. Book catalog, borrowing records and password recovery are **not implemented**.

## Run

Install JDK 17+ and Python 3.10+. From the repository root:

```sh
python build.py          # compile and verify against a temporary SQLite database
python build.py --run    # open the desktop app
```

The build downloads [Xerial SQLite JDBC 3.54.1.0](https://github.com/xerial/sqlite-jdbc/releases/tag/3.54.1.0) from Maven Central and verifies its pinned SHA-256. No old bundled JDBC binary is used. Network access is needed only for the first build; account use stays local. NetBeans users should run the build once to populate `.deps/`, then open this project. The Swing UI is maintained in Java source; obsolete GUI-builder forms were removed to avoid regenerating the broken handlers.

## Accounts and storage

- The default database is `~/.smart-library/library.db`; set `LIBRARY_DB_PATH` to an alternate file before launching. Parent directories and the versioned `accounts_v2` table are created automatically.
- Usernames are case-insensitive, 3–40 characters, using letters, digits, dots, underscores or hyphens. Passwords contain 12–128 characters; spaces are preserved.
- Passwords use individual random salts and PBKDF2-HMAC-SHA256 with 600,000 iterations, following the [OWASP password-storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). Database writes use prepared statements. Password work runs outside Swing's event thread.
- This is a **local account demonstration**. Anyone with filesystem access to the database can alter its records; it is not a remote authentication service or an operating-system security boundary.
- Historical `Account` tables are preserved but not accepted for authentication because they contain plaintext passwords and unsafe recovery answers. Create a new account. No password-display or security-question recovery is provided.
- The old tracked sample database and obsolete driver were removed from the current tree; their history remains. Existing user database files are not deleted or modified by the build.

## Verification

Windows and Linux CI compile all sources and run real SQLite checks for account creation, case-insensitive login, persistence, wrong credentials, duplicates, invalid input, unique salts, legacy-record preservation, corrupt hashes and database failures. UI-flow verification covers registration, login and sign-out.

[MIT license](LICENSE).
