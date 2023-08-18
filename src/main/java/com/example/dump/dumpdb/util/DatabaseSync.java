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
    private static final String PASSWORD = "password";
    private static final List<Long> addedPersonIds = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // 1. dump testdb su testdump
        String dumpFile = "intermediate_dump.sql";
        copyDatabase("testdb", "testdump", dumpFile);

        // 2. Aggiungi persone casuali a "testdump"
        addRandomPeopleToTestdump(10);

        // 3. dump di testdb su testdump2
        String dumpFile2 = "intermediate_dump2.sql";
        copyDatabase("testdb", "testdump2", dumpFile2);

        // 4. calcolare il delta tra testdump e testdump2 e genera un file chiamato delta_testdump.sql
        generateDelta("testdump", "testdump2", "delta_testdump.sql");

        // 5. applico a testdump delta_testdump.sql generando il file final_testdump.sql
        applyDelta("testdump", "delta_testdump.sql", "final_testdump.sql");

        // 6. controllare che testdump2 e final_testdump.sql siano uguali (si puo controllare con una diff vuota )
        dumpDatabase("testdump2", "testdump2_dump.sql");
        List<String> finalComparison = compareDumps("testdump2_dump.sql", "final_testdump.sql");
        if (finalComparison.isEmpty()) {
            System.out.println("Databases are synchronized!");
        } else {
            System.out.println("Databases are not synchronized. Further actions needed.");
        }


    }

    private static void applyDelta(String originalFile, String deltaFile, String finalFile) throws Exception {
        String command = "patch " + originalFile + " < " + deltaFile + " > " + finalFile;
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
    }


    private static void copyDatabase(String sourceDb, String targetDb, String dumpFile) throws Exception {

        dumpDatabase(sourceDb, dumpFile);

        String mysqlPath = "C:\\Programmi\\MySQL\\MySQLServer8.0\\bin\\mysql.exe";

        String[] restoreCmd = new String[]{"cmd.exe", "/c", mysqlPath + " -u " + USERNAME + " -p" + PASSWORD + " " + targetDb + " < " + dumpFile};
        Process process = Runtime.getRuntime().exec(restoreCmd);

        // Cattura e stampa l'output standard e l'output degli errori
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // Leggi l'output dal comando
        String s;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // Leggi eventuali errori dal comando
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

        process.waitFor();
    }

    private static void dumpDatabase(String dbName, String outputFile) throws Exception {
        String mysqldumpPath = "C:\\Programmi\\MySQL\\MySQLServer8.0\\bin\\mysqldump.exe";

        String[] command = {
                "cmd.exe", "/c",
                mysqldumpPath + " --no-create-info -u " + USERNAME + " -p" + PASSWORD + " " + dbName + " > " + outputFile
        };

        Process process = Runtime.getRuntime().exec(command);

        // Cattura e stampa l'output standard e l'output degli errori
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // Leggi l'output dal comando
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // Leggi eventuali errori dal comando
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

        process.waitFor();
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
            String sql = "INSERT INTO Persona (nome, cognome) VALUES (?, ?)";


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
        String command = "diff " + file1 + " " + file2 + " > " + deltaFile;
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
    }
}