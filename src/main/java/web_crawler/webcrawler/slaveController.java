package web_crawler.webcrawler;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.InetAddress;
import java.util.function.UnaryOperator;

public class slaveController {
    @FXML
    TextField portInput;
    @FXML
    Text ipAddressText;
    @FXML
    TextArea updates;

    @FXML
    private void runSlave() throws IOException {
        updates.setText("");
        updates.setVisible(true);
        String portNumber = portInput.getText();
        String IP = InetAddress.getLocalHost().getHostAddress();
        if(portNumber.isEmpty()){
            System.out.println("Port number is empty.");
        }
        else {
            new Slave(portNumber, IP, updates);
        }
    }

    @FXML
    private void initialize() throws IOException {
        ipAddressText.setText("IP address of Slave is: " + InetAddress.getLocalHost().getHostAddress());
        TextFormatter<String> portFormatter = new TextFormatter<>(filter);
        portInput.setTextFormatter(portFormatter);
        updates.setVisible(false);
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
        if (newCharacter.matches("[0-9]*")) {
            if (fullText.length() < 7) {
                if (!fullText.isEmpty()) {
                    if (Integer.parseInt(fullText) < 65536) {
                        return change;
                    }
                }
            }
        }
        return null;
    };



}
