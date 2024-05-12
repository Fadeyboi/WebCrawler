package web_crawler.webcrawler;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.function.UnaryOperator;

public class masterController {
    @FXML
    TextArea ipField;
    @FXML
    TextArea urlField;
    @FXML
    CheckBox duplicateCheck;
    @FXML
    TextField maxLevels;
    @FXML
    Button startButton;
    @FXML
    TextArea updates;
    public static ArrayList<String[]> slaveIPs = new ArrayList<>();
    public static ArrayList<String> seedURLs = new ArrayList<>();
    public static ArrayList<ArrayList<String>> splitSeedURLs = new ArrayList<>();
    private static HashMap<String, ArrayList<String>> extractedURLs = new HashMap<>();
    private int levels = 0;
    private int slaveCounter = 0;
    String USERNAME = "root";
    String PASSWORD = "";
    String URL = "jdbc:mysql://127.0.0.1:3306/mysql";
    Connection DBcon;
    @FXML
    private void initialize() {
        TextFormatter<String> portFormatter = new TextFormatter<>(filter);
        maxLevels.setTextFormatter(portFormatter);
        new Thread(() -> {
            System.out.println("Database manager running");
            try(ServerSocket serverSocket = new ServerSocket(6500)){
                while(!Thread.currentThread().isInterrupted()){
                    System.out.println("Waiting for connection");
                    Socket socket = serverSocket.accept();
                    new Thread(() -> {
                        System.out.println("NEW USER");
                        try(ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream())){
                            HashMap<String, ArrayList<String>> initialMap = (HashMap<String, ArrayList<String>>) objectInputStream.readObject();
                            extractedURLs.putAll(initialMap);
                            slaveCounter--;
                            if(slaveCounter == 0) {
                                addURLtoDatabase(extractedURLs);
                            }
                        }catch (IOException e){
                            System.out.println(e.getMessage());
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();

                }
            }catch (IOException e){
                System.out.println(e.getMessage());
            }
        }).start();
    }

    private boolean initializeConnection() {
        System.out.println("INITIALIZING CONNECTION");
        try{
            DBcon = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            return true;
        }catch (SQLException ignored){
            System.out.println("ERROR INITIALIZING CONNECTION");
            return false;
        }

    }

    private synchronized void addURLtoDatabase(HashMap<String, ArrayList<String>> temp)  {
        System.out.println("NEW ADDITION METHOD");
        if(DBcon == null)
            if(!initializeConnection())
                return;
        try {
            DatabaseMetaData dbm = DBcon.getMetaData();
            ResultSet tables = dbm.getTables(null, null, "urls", null);
            if (!tables.next()) {
                Statement newTable = DBcon.createStatement();
                String query = "CREATE TABLE urls(uid INT AUTO_INCREMENT PRIMARY KEY, date DATE, Page TEXT, ExtractedURL TEXT)";
                newTable.executeUpdate(query);
            }
        } catch (SQLException ignored) {}
        for(String page: temp.keySet()){
            System.out.println("PAGE: " + page);
            for (String extractedURL: temp.get(page)){
                try {
                    String queryStatement = "INSERT INTO urls (date, Page, ExtractedURL) VALUES (?, ?, ?)";
                    PreparedStatement newRow = DBcon.prepareStatement(queryStatement);
                    newRow.setDate(1, new java.sql.Date(new Date().getTime()));
                    newRow.setString(2, page);
                    newRow.setString(3, extractedURL);
                    newRow.executeUpdate();
                }catch (SQLException e){
                    System.out.println("Exception here");
                }

                System.out.println("EXTRACTED URL: " + extractedURL);
            }
        }
    }

    @FXML
    private void startButtonPressed() {
        updates.clear();
        slaveIPs.clear();
        seedURLs.clear();
        splitSeedURLs.clear();
        boolean duplicate = duplicateCheck.isSelected();
        if(!maxLevels.getText().isEmpty()){
            levels = Integer.parseInt(maxLevels.getText());
        }
        String[] fullIP = ipField.getText().split("\n");
        for (String line : fullIP) {
            slaveIPs.add(line.split(":"));
        }
        seedURLs.addAll(List.of(urlField.getText().split("\n")));
        int numberOfSlaves = slaveIPs.size();
        for (int i = 0; i < numberOfSlaves; i++) { //saves URL
            splitSeedURLs.add(new ArrayList<>());
        }
        for (int i = 0; i < seedURLs.size(); i++) { // splits the tasks
            splitSeedURLs.get(i % numberOfSlaves).add(seedURLs.get(i));
        }

        for (int i = 0; i < splitSeedURLs.size(); i++)
            updates.appendText("Slave number " +(i+1) + " tasked with " + splitSeedURLs.get(i) + "\n");


        new Thread(() -> {
            int num = 0;
            for (String[] ip : slaveIPs) {
                try (Socket masterSocket = new Socket(ip[0], Integer.parseInt(ip[1]));
                     ObjectOutputStream oos = new ObjectOutputStream(masterSocket.getOutputStream())) {
                    System.out.println("SOCKET: " + masterSocket.getInetAddress() + ":" + masterSocket.getPort());
                    oos.writeObject(splitSeedURLs.get(num)); //sends the URL
                    oos.writeObject(duplicate); // sends a boolean flag if duplication is allowed
                    oos.writeObject(levels); // sends the level of recursion
                    num++;
                    slaveCounter++;
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();

    }

    UnaryOperator<TextFormatter.Change> filter = change -> {
        String newCharacter = change.getText();
        if (!change.isContentChange()) {
            return change;
        }
        String fullText = change.getControlNewText();
        if (newCharacter.isEmpty()) {
            return change;
        }
        if (newCharacter.matches("[0-2]*")) {
            if (fullText.length() < 2) {
                if (!fullText.isEmpty()) {
                    if (Integer.parseInt(fullText) < 3) {
                        return change;
                    }
                }
            }
        }
        return null;
    };

}
