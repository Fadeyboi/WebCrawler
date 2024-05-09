package web_crawler.webcrawler;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.w3c.dom.ls.LSOutput;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    public static ArrayList<String[]> slaveIPs = new ArrayList<>();
    public static ArrayList<String> seedURLs = new ArrayList<>();
    public static ArrayList<ArrayList<String>> splitSeedURLs = new ArrayList<>();
    private boolean duplicate;
    private int levels = 0;

    @FXML
    private void initialize() throws IOException {
        TextFormatter<String> portFormatter = new TextFormatter<>(filter);
        maxLevels.setTextFormatter(portFormatter);
    }

    @FXML
    TextArea updates;
    @FXML
    private void startButtonPressed() throws IOException {
        updates.clear();
        //TEMP
        slaveIPs.clear();
        seedURLs.clear();
        splitSeedURLs.clear();
        //TEMP

        duplicate = duplicateCheck.isSelected();
        if(!maxLevels.getText().isEmpty()){
            levels = Integer.parseInt(maxLevels.getText());
        }
        String[] fullIP = ipField.getText().split("\n");
        for (String line : fullIP) {
            slaveIPs.add(line.split(":"));
        }
        seedURLs.addAll(List.of(urlField.getText().split("\n")));
        int numberOfSlaves = slaveIPs.size();
        System.out.println(numberOfSlaves);
        for (int i = 0; i < numberOfSlaves; i++) { //saves URL
            splitSeedURLs.add(new ArrayList<>());
        }
        for (int i = 0; i < seedURLs.size(); i++) { // splits the tasks
            splitSeedURLs.get(i % numberOfSlaves).add(seedURLs.get(i));
        }

        for (int i = 0; i < splitSeedURLs.size(); i++)
//            System.out.println("Slave " + (i + 1) + ": " + splitSeedURLs.get(i));
            updates.appendText("Slave number " +(i+1) + " tasked with " + splitSeedURLs.get(i) + "\n");


        int num = 0;
        for (String[] ip : slaveIPs) {
            try (Socket masterSocket = new Socket(ip[0], Integer.parseInt(ip[1]));
                 ObjectOutputStream oos = new ObjectOutputStream(masterSocket.getOutputStream())){
                    oos.writeObject(splitSeedURLs.get(num)); //sends the URL
                    oos.writeObject(duplicate); // sends a boolean flag if duplication is allowed
                    oos.writeObject(levels); // sends the level of recursion
                    num++;
            } catch (IOException e) {
                 System.out.println(e.getMessage());
            }
        }

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
