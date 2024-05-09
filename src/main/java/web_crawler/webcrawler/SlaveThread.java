package web_crawler.webcrawler;

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
    private boolean openConnection;


    public SlaveThread (String port) {
        this.port = port;
        this.openConnection = true;
    }

    public SlaveThread (String port, LinkedList<String> urlLinkedList, boolean duplicate, int levels) {
        this.port = port;
        this.URLs = new ArrayList<>(urlLinkedList);
        this.urlLinkedList = urlLinkedList;
        this.duplicate = duplicate;
        this.levels = levels;
        this.openConnection = false;
    }

    @Override
    public void run() {
        URL runURL;
        Scanner sc = null;
        try {
            if (openConnection) {
                try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port))) {
                    Socket clientSocket = serverSocket.accept();
                    try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {
                        this.URLs = (ArrayList<String>) ois.readObject();
                        this.duplicate = (boolean) ois.readObject();
                        this.levels = (Integer) ois.readObject();
                    } catch (ClassNotFoundException e) {
                        System.out.println(e.getMessage());
                        ;
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
                    sc = new Scanner(runURL.openStream());
                    StringBuffer sb = new StringBuffer();
                    while (sc.hasNextLine()) {
                        sb.append(sc.nextLine());
                    }
                    Pattern p = Pattern.compile("href=\"([^\"]*)\"");
                    Matcher matcher = p.matcher(sb);
                    while (matcher.find()) {
                        String group = matcher.group(1);
                        if (duplicate) {
                            temporaryArrayList.add(group);
                            urlLinkedList.add(group);
                        } else {
                            if (!temporaryArrayList.contains(group)) {
                                temporaryArrayList.add(group);
                                urlLinkedList.add(group);
                            }
                        }
                    }
                    extractedURLs.put(url, temporaryArrayList);
                }
            }
            for (String urls : urlLinkedList) {
                System.out.println(urls);
            }

            if (levels > 0){
                System.out.println("HI REACHED HERE");
                SlaveThread slaveThread = new SlaveThread(this.port, this.urlLinkedList, this.duplicate, this.levels-1);
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
