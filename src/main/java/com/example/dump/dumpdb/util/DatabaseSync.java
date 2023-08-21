package com.example.dump.dumpdb.util;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class DatabaseSync {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "Xio920@1";
    private static final List<Long> addedPersonIds = new ArrayList<>();

    // Determina il sistema operativo
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS.contains("win");

    private static final String SHELL_CMD = IS_WINDOWS ? "cmd.exe" : "/bin/sh";
    private static final String SHELL_OPTION = IS_WINDOWS ? "/c" : "-c";
    private static final String MYSQLDUMP_PATH = IS_WINDOWS ? "C:\\Programmi\\MySQL\\MySQLServer8.0\\bin\\mysqldump.exe" : "mysqldump";
    private static final String MYSQL_PATH = IS_WINDOWS ? "C:\\Programmi\\MySQL\\MySQLServer8.0\\bin\\mysql.exe" : "mysql";

    public static void main(String[] args) throws Exception {
        //clear tabelle
        clearTableIfNotEmpty("testdump", "persona");
        clearTableIfNotEmpty("testdump2", "persona");

        // 1. dump testdb su testdump
        String dumpFile = "intermediate_dump.sql";
        copyDatabase("testdb", "testdump", dumpFile);

        // 2. Aggiungi persone casuali a "testdump"
        addRandomPeopleToTestdump(10);

        // 3. dump di testdb su testdump2
        String dumpFile2 = "intermediate_dump2.sql";
        copyDatabase("testdb", "testdump2", dumpFile2);

        // 4. calcolare il delta tra testdump e testdump2 e genera un file chiamato delta_testdump.sql
        generateDelta(dumpFile, dumpFile2, "delta_testdump.sql");

        // 5. applico a testdump delta_testdump.sql generando il file final_testdump.sql
        applyDelta(dumpFile, "delta_testdump.sql", "final_testdump.sql");

        // 6. controllare che testdump2 e final_testdump.sql siano uguali (si puo controllare con una diff vuota )
        dumpDatabase("testdump2", "testdump2_dump.sql");
        List<String> finalComparison = compareDumps("testdump2_dump.sql", "final_testdump.sql");
        if (finalComparison.isEmpty()) {
            System.out.println("Databases are synchronized!");
        } else {
            System.out.println("Databases are not synchronized. Further actions needed.");
        }
    }


    private static void clearTableIfNotEmpty(String dbName, String tableName) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL + dbName, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM " + tableName);

            if (rs.next() && rs.getInt("total") > 0) {
                stmt.executeUpdate("DELETE FROM " + tableName);
                System.out.println("Cleared all records from " + tableName + " in " + dbName);
            }
        }
    }

    private static void applyDelta(String originalFile, String deltaFile, String finalFile) throws Exception {
        String command = "patch " + originalFile + " < " + deltaFile + " > " + finalFile;
        executeCommand(command);
    }

    private static void copyDatabase(String sourceDb, String targetDb, String dumpFile) throws Exception {
        dumpDatabase(sourceDb, dumpFile);

        String command = MYSQL_PATH + " -u " + USERNAME + " -p" + PASSWORD + " " + targetDb + " < " + dumpFile;
        executeCommand(command);
    }

    private static void dumpDatabase(String dbName, String outputFile) throws Exception {
        String command = MYSQLDUMP_PATH + " --no-create-info -u " + USERNAME + " -p" + PASSWORD + " " + dbName + " > " + outputFile;
        executeCommand(command);
    }

    private static List<String> compareDumps(String file1, String file2) throws Exception {
        List<String> lines1 = Files.readAllLines(Paths.get(file1));
        List<String> lines2 = Files.readAllLines(Paths.get(file2));

        Patch<String> patch = DiffUtils.diff(lines1, lines2);

        return patch.getDeltas().stream()
                .map(delta -> delta.toString())
                .collect(Collectors.toList());
    }

    private static void addRandomPeopleToTestdump(int numberOfPeople) throws Exception {
        Connection conn = DriverManager.getConnection(DB_URL + "testdump", USERNAME, PASSWORD);
        Random rand = new Random();

        for (int i = 0; i < numberOfPeople; i++) {
            String nome = "Nome" + rand.nextInt(1000);
            String cognome = "Cognome" + rand.nextInt(1000);
            String sql = "INSERT INTO persona (nome, cognome) VALUES (?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, nome);
            stmt.setString(2, cognome);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                addedPersonIds.add(rs.getLong(1));
            }
        }

        conn.close();
    }

    private static void generateDelta(String file1, String file2, String deltaFile) throws Exception {
        String command = "diff -u " + file1 + " " + file2 + " > " + deltaFile;
        executeCommand(command);
    }

    private static void executeCommand(String command) throws Exception {
        String[] cmdArray = {SHELL_CMD, SHELL_OPTION, command};
        Process process = Runtime.getRuntime().exec(cmdArray);

        try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String s;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            while ((s = stdError.readLine()) != null) {
                System.err.println(s);
            }
        }

        process.waitFor();
    }
}
