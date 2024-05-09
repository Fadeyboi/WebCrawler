package web_crawler.webcrawler;

import javafx.scene.control.TextArea;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlaveThread implements Runnable {
    private String port;
    private LinkedList<String> urlLinkedList = new LinkedList<>();
    private ArrayList<String> URLs = new ArrayList<>();
    private static HashMap<String, ArrayList<String>> extractedURLs = new HashMap<>();
    private boolean duplicate;
    private int levels;
    private final boolean openConnection;
    private final TextArea updates;


    public SlaveThread (String port, TextArea updates) {
        this.port = port;
        this.openConnection = true;
        this.updates = updates;
    }

    public SlaveThread (String port, LinkedList<String> urlLinkedList, boolean duplicate, int levels, TextArea updates) {
        this.port = port;
        this.URLs = new ArrayList<>(urlLinkedList);
        this.urlLinkedList = urlLinkedList;
        this.duplicate = duplicate;
        this.levels = levels;
        this.openConnection = false;
        this.updates = updates;
    }

    @Override
    public void run() {
        URL runURL;
        Scanner htmlScanner;
        try {
            if (openConnection) {
                try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port))) {
                    Socket clientSocket = serverSocket.accept();
                    try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {
                        this.URLs = (ArrayList<String>) ois.readObject();
                        this.duplicate = (boolean) ois.readObject();
                        this.levels = (Integer) ois.readObject();
                        String updateMessage = "Slave Received: [" + URLs.toString() + "\nEnableDuplicates: "
                                + duplicate + "\nMaxLevels: " + levels + "\n----------\n";
                        updates.setText(updateMessage);
                    } catch (ClassNotFoundException e) {
                        System.out.println(e.getMessage());
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }

            System.out.println("All URLs: " + this.URLs);
            System.out.println("Duplicate: " + this.duplicate);
            System.out.println("Current level: " + this.levels);
            for (String url: this.URLs) {
                ArrayList<String> temporaryArrayList = new ArrayList<>();
                System.out.println("Current URL: " + url);
                if(URI.create(url).isAbsolute()) {
                    runURL = URI.create(url).toURL();
                    htmlScanner = new Scanner(runURL.openStream());
                    StringBuffer htmlContent = new StringBuffer();

                    while (htmlScanner.hasNextLine()) {
                        htmlContent.append(htmlScanner.nextLine());
                    }
                    String regex = "<a[^>]+href=\"(.*?)\"[^>]*>";
                    Pattern pattern = Pattern.compile(regex);

                    Matcher matcher = pattern.matcher(htmlContent);

                    while (matcher.find()) {
                        String group;
                        if(matcher.group(0) != null)
                            group = matcher.group(1);
                        else
                            break;
                        if(URI.create(group).isAbsolute()){
                            if (duplicate) {
                                temporaryArrayList.add(group);
                                urlLinkedList.add(group);
//                                updates.appendText("Second URL: " + group + "\n");

                            } else {
                                if (!temporaryArrayList.contains(group)) {
                                    temporaryArrayList.add(group);
                                    urlLinkedList.add(group);
//                                    updates.appendText("Second URL: " + group + "\n");

                                }
                            }
                        }



                    }
                    extractedURLs.put(url, temporaryArrayList);
                }

            }
            updates.appendText("--Thread ended--\n");
            for (String urls : urlLinkedList) {
                updates.appendText("\n");
                updates.appendText(urls);
            }

            if (levels > 0){
                updates.appendText("--Different Thread--\n");
                SlaveThread slaveThread = new SlaveThread(this.port, this.urlLinkedList, this.duplicate, this.levels-1, updates);
                Thread t1 = new Thread(slaveThread);
                t1.start();
                t1.join();
            }
        } catch (IOException e) {
            return;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(extractedURLs);
    }

    public HashMap<String, ArrayList<String>> getExtractedURLs() {
        return extractedURLs;
    }
}
