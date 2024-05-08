package web_crawler.webcrawler;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class userTypeController {
    @FXML
    RadioButton isMaster;


    @FXML
    private void userTypeSelected() throws IOException {
        Parent root;
        if(!isMaster.isSelected())
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("slave.fxml")));
        else
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("master.fxml")));

        Stage stage = (Stage) isMaster.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}