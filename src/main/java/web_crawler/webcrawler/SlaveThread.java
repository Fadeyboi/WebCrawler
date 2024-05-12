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
        Socket clientSocket = null;
        Thread secondThread = null;
        try {
            if (openConnection) {
                try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port))) {
                    clientSocket = serverSocket.accept();
                    try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {
                        this.URLs = (ArrayList<String>) ois.readObject();
                        this.duplicate = (boolean) ois.readObject();
                        this.levels = (Integer) ois.readObject();
                        System.out.println(URLs);
                        if(URLs.size() > 1) {
                            LinkedList<String> secondThreadLinkedList = new LinkedList<>();
                            for (int i = (URLs.size() - 1) / 2; i < URLs.size(); i++) {
                                secondThreadLinkedList.add(URLs.get(i));
                                URLs.remove(URLs.get(i));
                                System.out.println(URLs.get(i));
                            }
                            secondThread = new Thread(new SlaveThread(this.port, secondThreadLinkedList, this.duplicate,
                                    this.levels - 1, updates));
                            secondThread.setName("SecondThread");
                            secondThread.start();
                        }
                        String updateMessage = "Slave Received: [" + URLs.toString() + "\nEnableDuplicates: "
                                + duplicate + "\nMaxLevels: " + levels + "\n----------\n";
                            updates.setText(updateMessage);
                    } catch (ClassNotFoundException ignored) {}
                } catch (IOException ignored) {}
            }

            if (URLs.isEmpty()){
                return;
            }
            for (String url: this.URLs ) {
                System.out.println("Current URL: " + url + " ----- Thread Name: " + Thread.currentThread().getName());
                ArrayList<String> temporaryArrayList = new ArrayList<>();
                if(URI.create(url).isAbsolute()) {
                    runURL = URI.create(url).toURL();
                    InputStream inputStream = runURL.openStream();
                    String s = new String (inputStream.readAllBytes());
                    Pattern pattern = Pattern.compile("<a[^>]+href=\"(.*?)\"[^>]*>");
                    Matcher matcher = pattern.matcher(s);

                    while (matcher.find()) {
                        String group = matcher.group(1);
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
                Thread t1 = new Thread(new SlaveThread(this.port, this.urlLinkedList, this.duplicate,
                        this.levels-1, updates));
                t1.start();
                t1.join();
            }
            if(secondThread != null) {
                secondThread.join();
            }
            if(Thread.currentThread().getName().equals("FirstThread")) {
                sendExtractedURLs(clientSocket);
            }

        } catch (IOException | InterruptedException ignored) {}
    }

    public void sendExtractedURLs(Socket socket) {
        if(socket == null)
            return;
        String IP = socket.getInetAddress().getHostAddress();
        int port = 6500;
        int count = 0;
        for (Map.Entry<String, ArrayList<String>> entry: extractedURLs.entrySet()) {
            System.out.println(entry.getKey());
            count++;
            for (String url1 : entry.getValue()) {
                System.out.println(url1);
                count++;
            }
        }
        System.out.println("SIZE OF MAP: " + count);
        updates.appendText("----------\nSLAVE TERMINATED");
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(new Socket(IP, port).getOutputStream())){
            objectOutputStream.writeObject(extractedURLs);
        }catch (IOException ignored){}
        extractedURLs.clear();
    }
}
