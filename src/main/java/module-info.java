module web_crawler.webcrawler {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires java.sql;


    opens web_crawler.webcrawler to javafx.fxml;
    exports web_crawler.webcrawler;
}