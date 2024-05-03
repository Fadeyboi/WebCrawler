module web_crawler.webcrawler {
    requires javafx.controls;
    requires javafx.fxml;


    opens web_crawler.webcrawler to javafx.fxml;
    exports web_crawler.webcrawler;
}