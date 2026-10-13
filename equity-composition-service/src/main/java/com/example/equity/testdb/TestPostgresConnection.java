package com.example.equity.testdb;

import java.sql.*;

public class TestPostgresConnection {

    public static void main(String[] args) throws Exception {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "5432");
        String db   = System.getenv().getOrDefault("DB_NAME", "postgres");
        String user = System.getenv().getOrDefault("DB_USERNAME", "postgres");
        String pass = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        System.out.println("Connecting to " + url + " as " + user + " ...");

        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            System.out.println("✅ Connected");

            DatabaseMetaData md = c.getMetaData();
            System.out.println("  Server       : " + md.getDatabaseProductName()
                    + " " + md.getDatabaseProductVersion());
            System.out.println("  Driver       : " + md.getDriverName()
                    + " " + md.getDriverVersion());
            System.out.println("  JDBC version : " + md.getJDBCMajorVersion()
                    + "." + md.getJDBCMinorVersion());
            System.out.println("  Auto-commit  : " + c.getAutoCommit());

            // 1. Confirm the schema exists
            try (Statement st = c.createStatement()) {
                System.out.println("\n--- Tables ---");
                try (ResultSet rs = st.executeQuery(
                        "SELECT table_name FROM information_schema.tables " +
                                "WHERE table_schema = 'public' ORDER BY table_name")) {
                    while (rs.next()) {
                        System.out.println("  " + rs.getString(1));
                    }
                }

                // 2. Row counts
                System.out.println("\n--- Row counts ---");
                for (String t : new String[]{"equity_composition", "equity_composition_chunk"}) {
                    try (ResultSet rs = st.executeQuery("SELECT count(*) FROM " + t)) {
                        rs.next();
                        System.out.println("  " + t + " = " + rs.getLong(1));
                    }
                }

                // 3. Sequence check
                System.out.println("\n--- Sequences ---");
                try (ResultSet rs = st.executeQuery(
                        "SELECT sequencename, last_value FROM pg_sequences " +
                                "WHERE schemaname = 'public'")) {
                    while (rs.next()) {
                        System.out.println("  " + rs.getString(1) + " = " + rs.getLong(2));
                    }
                }

                // 4. Transaction round-trip test
                System.out.println("\n--- Write/rollback test ---");
                c.setAutoCommit(false);
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO equity_composition " +
                                "(portfolio_id, as_of_date, status, total_chunks, total_bytes) " +
                                "VALUES (?, CURRENT_DATE, 'TEST', 0, 0)")) {
                    ps.setString(1, "CONN-TEST-" + System.currentTimeMillis());
                    int n = ps.executeUpdate();
                    System.out.println("  Inserted " + n + " row(s)");
                }
                c.rollback();
                System.out.println("  Rolled back (no data persisted)");
                c.setAutoCommit(true);
            }

            System.out.println("\n✅ All connectivity tests passed");
        } catch (SQLException e) {
            System.err.println("❌ Connection failed");
            System.err.println("  SQLState : " + e.getSQLState());
            System.err.println("  ErrorCode: " + e.getErrorCode());
            System.err.println("  Message  : " + e.getMessage());
            System.exit(1);
        }
    }
}
