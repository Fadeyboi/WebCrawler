package web_crawler.webcrawler;

public class Slave {
    private String port;
    private String IP;

    public Slave(String port, String IP) throws InterruptedException {
        this.port = port;
        this.IP = IP;
        System.out.println("New slave made, IP is: " + IP + ":" + port);
        Thread t1 = new Thread(new SlaveThread(port));
        t1.start();
    }
}
