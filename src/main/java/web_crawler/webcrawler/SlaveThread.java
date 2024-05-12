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
    private static final HashMap<String, ArrayList<String>> extractedURLs = new HashMap<>();
    private boolean duplicate;
    private int levels;
    private final boolean openConnection;
    private final TextArea updates;
    private static Set<String> update = new HashSet<>();
    private static Set<String> checkedUrls = new HashSet<>();


    public SlaveThread (String port, TextArea updates) {
        this.port = port;
        this.openConnection = true;
        this.updates = updates;
        update.clear();
        checkedUrls.clear();
    }

    public SlaveThread (String port, LinkedList<String> urlLinkedList, boolean duplicate, int levels, TextArea updates, Set<String> checkedUrls) {
        this.port = port;
        this.URLs = new ArrayList<>(urlLinkedList);
        this.urlLinkedList = urlLinkedList;
        this.duplicate = duplicate;
        this.levels = levels;
        this.openConnection = false;
        this.updates = updates;
        this.checkedUrls = checkedUrls;
    }

    @Override
    public void run() {
        URL runURL;
        Socket clientSocket = null;
        Thread secondThread = null;
        Pattern pattern = Pattern.compile("<a[^>]+href=\"(.*?)\"[^>]*>");

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
                            int start = URLs.size() / 2;
                            for (int i = URLs.size() - 1; i >= start; i--) {
                                secondThreadLinkedList.addFirst(URLs.get(i));
                                URLs.remove(i);
                            }
                            secondThread = new Thread(new SlaveThread(this.port, secondThreadLinkedList, this.duplicate,
                                    this.levels, updates, checkedUrls));
                            secondThread.setName("SecondThread");
                            secondThread.start();
                        }
                        String updateMessage = "Slave Received: [" + URLs.toString() + "\nEnableDuplicates: "
                                + duplicate + "\nMaxLevels: " + levels + "\nHOSTS VISITED\n----------\n";
                        updates.setText(updateMessage);
                    } catch (ClassNotFoundException ignored) {}
                } catch (IOException ignored) {}
            }

            if (URLs.isEmpty()){
                return;
            }
            for (String url : this.URLs) {
                if (URI.create(url).isAbsolute() && (checkedUrls.add(url) || duplicate)) {
                    String hostname = URI.create(url).getHost();
                    System.out.println("Current URL: " + url + " ----- Thread Name: " + Thread.currentThread().getName());
                    ArrayList<String> temporaryArrayList = new ArrayList<>();
                    runURL = URI.create(url).toURL();
                    InputStream inputStream = null;
                    try {
                        inputStream = new BufferedInputStream(runURL.openStream());
                    } catch (FileNotFoundException | ConnectException ignored) {
                        continue;
                    }
                    try {
                        String s = new String(inputStream.readAllBytes());
                        Matcher matcher = pattern.matcher(s);

                        while (matcher.find()) {
                            String group = matcher.group(1);
                            URI uri = null;
                            try {
                                uri = URI.create(group);
                            } catch (IllegalArgumentException ignored) {
                                continue;
                            }
                            if (uri.isAbsolute()) {
                                if (!temporaryArrayList.contains(group) || duplicate) {
                                    temporaryArrayList.add(group);
                                    if (uri.getHost() != null) {
                                        update.add(uri.getHost());
                                        if (uri.getHost().equals(hostname)) {
                                            urlLinkedList.add(group);
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        inputStream.close();  // Ensure inputStream is closed after use
                    }
                    extractedURLs.put(url, temporaryArrayList);
                }
            }

            if (levels > 0) {
                Thread t1 = new Thread(new SlaveThread(this.port, this.urlLinkedList, this.duplicate, this.levels - 1, updates, checkedUrls));
                t1.start();
                t1.join();
            }
            if(secondThread != null) {
                secondThread.join();
            }
            if(Thread.currentThread().getName().equals("FirstThread")) {
                sendExtractedURLs(clientSocket);
            }

        } catch (IOException | InterruptedException| IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
        }

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
        StringBuilder hostNames = new StringBuilder();
        for (String hostName: update){
            hostNames.append(hostName).append("\n");
        }
        updates.appendText(String.valueOf(hostNames));
        System.out.println("SIZE OF MAP: " + count);
        updates.appendText(" ----- \nSLAVE TERMINATED");
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(new Socket(IP, port).getOutputStream())){
            objectOutputStream.writeObject(extractedURLs);
        }catch (IOException ignored){}
        extractedURLs.clear();
    }
}
