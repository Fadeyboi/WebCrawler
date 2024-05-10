package web_crawler.webcrawler;

import javafx.scene.control.TextArea;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlaveThread implements Runnable {
    private final String port;
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
        int maxLinks = 10;
        int linkCount = 0;
        Socket clientSocket = null;
        try {
            if (openConnection) {
                try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port))) {
                    clientSocket = serverSocket.accept();
                    try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {
                        this.URLs = (ArrayList<String>) ois.readObject();
                        this.duplicate = (boolean) ois.readObject();
                        this.levels = (Integer) ois.readObject();
                        String updateMessage = "Slave Received: [" + URLs.toString() + "\nEnableDuplicates: "
                                + duplicate + "\nMaxLevels: " + levels + "\n----------\n";
                            updates.setText(updateMessage);
                    } catch (ClassNotFoundException e) {
//                        System.out.println(e.getMessage());
                    }
                } catch (IOException e) {
//                    System.out.println(e.getMessage());
                }
            }

//            System.out.println("All URLs: " + this.URLs);
//            System.out.println("Duplicate: " + this.duplicate);
//            System.out.println("Current level: " + this.levels);
            for (String url: this.URLs ) {
//                System.out.println("IM STUCK IN URL FOR LOOP");
                ArrayList<String> temporaryArrayList = new ArrayList<>();
//                System.out.println("Current URL: " + url);
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

                    while (matcher.find() && linkCount < maxLinks) {
//                        System.out.println("IM STUCK IN MATCHER");
                        String group;
                        if(matcher.group(1) != null)
                            group = matcher.group(1);
                        else
                            break;
                        if(URI.create(group).isAbsolute()){
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

                        linkCount++;

                    }
                    extractedURLs.put(url, temporaryArrayList);
                }

            }
            StringBuilder update = new StringBuilder();
            for (String urls : urlLinkedList) {
                update.append(urls).append("\n");
            }

            updates.appendText(update.toString());

            if (levels > 0){
//                System.out.println("Different thread");
                SlaveThread slaveThread = new SlaveThread(this.port, this.urlLinkedList, this.duplicate,
                        this.levels-1, updates);
                Thread t1 = new Thread(slaveThread);
                t1.start();
                t1.join();
//                System.out.println("T1: " + t1.isAlive());
//                System.out.println("DONE, GOT ALL DATA");
                sendExtractedURLs(clientSocket);
            }else {
                sendExtractedURLs(clientSocket);

            }

        } catch (IOException e) {
            return;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

    public void sendExtractedURLs(Socket socket) {
        if(socket == null)
            return;
        String IP = socket.getInetAddress().getHostAddress();
        int port = 6500;
        System.out.println("SIZE OF MAP: " + extractedURLs.size());
        for (ArrayList<String> url : extractedURLs.values()) {
            for (String url1 : url) {
                System.out.println(url1);
            }
        }
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(new Socket(IP, port).getOutputStream())){
            objectOutputStream.writeObject(extractedURLs);

        }catch (IOException ignored){

        }
        extractedURLs.clear();
    }
}
