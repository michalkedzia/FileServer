package sample;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;

import java.io.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class ClientCommunicationHandler extends Thread {
    private ServerSocket serverSocket;
    int port;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private static String path = "UserDirectories"; // <-------- Nalezy stworzyc katalog i padac sciezke
    ArrayList<String> clientList;
    private Controller controller;
    String userName;
    private TreeItem<String> root;

    private ServerSocket serverSocketFile;
    DataOutputStream dos = null;
    DataInputStream dis = null;
    private Socket clientSocketFile;


    ExecutorService executor = Executors.newFixedThreadPool(5);
    BlockingQueue<CommunicationMessage> queue = new ArrayBlockingQueue<CommunicationMessage>(10);

    public ClientCommunicationHandler(int port, ArrayList<String> clientList, Controller controller, String userName) {
        this.port = port;
        this.clientList = clientList;
        this.controller = controller;
        this.userName = userName;

        try {
            serverSocket = new ServerSocket(this.port);
            serverSocketFile = new ServerSocket(this.port + 1);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void initObservableListFiles() {

        String[] temp;
        File f = new File(path + "\\" + userName);
        temp = f.list();

        Platform.runLater(() -> {
            for (String pathname : temp) {
                TreeItem<String> s = new TreeItem<String>(pathname);
                root.getChildren().add(s);
            }
        });


        executor.execute(this::observer);

//        Thread thread = new Thread(this::observer);
//        thread.start();

    }


    synchronized void sendFile(String path) throws IOException {

        FileInputStream fis = new FileInputStream(path);
        byte[] buffer = new byte[4096];
        while (fis.read(buffer) > 0) {
            dos.write(buffer);
        }
        fis.close();
    }

    public synchronized long checkFileSize(String path) {
        File f = new File(path);
        return f.length();
    }


    synchronized void reciveFile(String path, long fileSize) throws IOException {

        FileOutputStream fos = new FileOutputStream(path);
        byte[] buffer = new byte[4096];
        long bufferSize = 4096;
        int packets = 0;
        boolean f = false;

        if ((fileSize % bufferSize) > 0) {
            packets = ((int) (fileSize / bufferSize)) + 1;
            f = true;
        } else {
            packets = ((int) (fileSize / bufferSize));
        }

        int read = 0;
        int totalRead = 0;
        int remaining = (int) fileSize;
        for (int i = 0; i < packets; i++) {

            if ((packets - 1) == i && f == true) {
                read = dis.read(buffer, 0, buffer.length);
                fos.write(buffer, 0, (int) (fileSize % bufferSize));
            } else {
                read = dis.read(buffer, 0, buffer.length);
                fos.write(buffer, 0, read);
            }
        }

        fos.close();

    }


    private void observer() {
        try {
            WatchService watchService
                    = FileSystems.getDefault().newWatchService();

            Path pathDirectory = Paths.get(path + "\\" + userName);

            pathDirectory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);


            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {

                    if (event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE) == true) {
                        Platform.runLater(() -> {
                            TreeItem<String> s = new TreeItem<String>(event.context().toString());
                            root.getChildren().add(s);
                        });

                        try {
                            CommunicationMessage m = new CommunicationMessage();
                            m.setMessageID(CommunicationMessage.MessageType.CHECK_FILE);
                            m.setFileName(event.context().toString());
                            queue.put(m);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }

                    if (event.kind().equals(StandardWatchEventKinds.ENTRY_DELETE) == true) {
                        Platform.runLater(() -> {
                            int i = 0;
                            for (TreeItem<String> child : root.getChildren()) {
                                if (child.getValue().equals(event.context().toString())) {
                                    root.getChildren().remove(child);
                                    break;
                                }
                            }
                        });
                    }
                }
                key.reset();
            }

        } catch (Exception e) {
        }
    }


    public void registerUser() {
        String[] directories;
        File ff = new File(path);
        directories = ff.list();
        boolean exist = false;

        for (String dir : directories) {

            if (dir.equals(userName) == true) {
                exist = true;
            }
        }
        if (exist = true) {
            File newDir = new File(path + "\\" + userName);
            newDir.mkdir();
        }
    }


    void userOperationHandler() {

        CommunicationMessage m = null;

        try {
            while (true) {
                m = (CommunicationMessage) in.readObject();


                if (m.getMessageID().equals(CommunicationMessage.MessageType.FILE_TO_OTHER_USER)) {
                    reciveFile(path + "\\" + m.getUserNameFileSend() + "\\" + m.getFileName(), m.getFileSize());

                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.FILE)) {

                    System.out.println(userName + "   " + m.getFileSize());
                    reciveFile(path + "\\" + userName + "\\" + m.getFileName(), m.getFileSize());


                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.CHECK_FILE)) {

                    String[] temp;
                    File f = new File(path + "\\" + userName);
                    temp = f.list();
                    boolean flag = false;

                    for (String file : temp) {
                        if (file.equals(m.getFileName())) {
                            flag = true;
                            break;
                        }
                    }

                    if (flag == false) {
                        m.setMessageID(CommunicationMessage.MessageType.FILE_NOT_ON_SERVER);
                        queue.put(m);

                    }

                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.FILE_NOT_ON_USER)) {
                    m.setMessageID(CommunicationMessage.MessageType.FILE);
                    queue.put(m);


                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.MODIFY_FILE)) {


                    File fileToDelete = new File(path + "\\" + userName + "\\" + m.getFileName());
                    fileToDelete.delete();

                    reciveFile(path + "\\" + userName + "\\" + m.getFileName(), m.getFileSize());


                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.DELETE_FILE)) {

                    File fileToDelete = new File(path + "\\" + userName + "\\" + m.getFileName());
                    fileToDelete.delete();

                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.LOGOUT)) {
                    m.setMessageID(CommunicationMessage.MessageType.LOGOUT);
                    queue.put(m);


                } else {
                    System.out.println("Blad ");
                    System.out.println(m.getMessageID());
                }


            }

        } catch (EOFException e){
            System.out.println(e.toString());
        }catch (Exception e) {
            e.printStackTrace();
        }


    }


    void serverOperationHandler() {


        CommunicationMessage m = null;

        try {

            while (true) {
                m = queue.take();


                if (m.getMessageID().equals(CommunicationMessage.MessageType.FILE_NOT_ON_SERVER)) {
                    out.writeObject(m);
                    out.flush();

                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.CHECK_FILE)) {
                    out.writeObject(m);
                    out.flush();

                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.FILE)) {


                    m.setFileSize(checkFileSize(path + "\\" + userName + "\\" + m.getFileName()));
                    out.writeObject(m);
                    out.flush();


                    try {
                        sendFile(path + "\\" + userName + "\\" + m.getFileName());
                    } catch (Exception e) {
                        Thread.sleep(1000);
                        try {
                            sendFile(path + "\\" + userName + "\\" + m.getFileName());
                        } catch (Exception e2) {
                            Thread.sleep(1000);
                            sendFile(path + "\\" + userName + "\\" + m.getFileName());
                        }
                    }


                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.UPDATE_CLIENT_LIST)) {
                    out.writeObject(m);
                    out.flush();
                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.LOGOUT)) {

                    System.out.println("wylogowanie " + userName);

                    clientList.remove(userName);
                    Platform.runLater(() -> {
                        controller.getTreeRoot().getChildren().remove(root);
                    });



                    break;

                }


            }

        } catch (Exception e) {
            System.out.println(e.toString());
        }


    }


    void checkChangesClietList() {

        addNewUserToList();
        while (true) {
            try {
                Thread.sleep(2000);

                CommunicationMessage m = new CommunicationMessage();
                m.setMessageID(CommunicationMessage.MessageType.UPDATE_CLIENT_LIST);

                ArrayList<String> cpy = new ArrayList<String>(clientList);

                m.setClientList(cpy);
                queue.put(m);


            } catch (InterruptedException e){
                System.out.println(e.toString());
            }catch (Exception e) {
                e.printStackTrace();
            }
        }


    }


    @Override
    public void run() {

        try {

            boolean f = true;
            clientSocket = serverSocket.accept();

            //
            clientSocketFile = serverSocketFile.accept();
            dos = new DataOutputStream(clientSocketFile.getOutputStream());
            dis = new DataInputStream(clientSocketFile.getInputStream());
            //

            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());


            registerUser();


            Platform.runLater(() -> {
                root = new TreeItem<String>(userName);
                controller.getTreeRoot().getChildren().add(root);
            });

            initServerClinetFiles();
            initObservableListFiles();



            executor.execute(new Thread(this::checkChangesClietList));
            executor.execute(new Thread(this::serverOperationHandler));

//            Thread threaduserOperationHandler = new Thread(this::checkChangesClietList);
//            threaduserOperationHandler.start();


//            Thread threadSendNewFileToUser = new Thread(this::sendNewFileToUser);
//            threadSendNewFileToUser.start();

            userOperationHandler();




            executor.shutdown();
            try {
                if (!executor.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }



            //
            dos.close();
            dis.close();
            clientSocketFile.close();
            serverSocketFile.close();
            //

            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();

            System.out.println("koniec usera "  + userName );
        } catch (EOFException eof) {
            //  moze, rzucac  EOF przed odczytaniem okreslonej dlugosci bajtow
            eof.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized public void addNewUserToList() {
        clientList.add(userName);
    }

    public void initServerClinetFiles() {
        try {
            CommunicationMessage m;

            m = (CommunicationMessage) in.readObject();


            if (m.getMessageID() == CommunicationMessage.MessageType.NUMBER_OF_FILE_TO_CHCEK) {

                String[] temp;
                File ff = new File(path + "\\" + userName);
                temp = ff.list();
                int counter = m.getNumberOfFileToCheck();
                boolean flag = false;

                m.setMessageID(CommunicationMessage.MessageType.READY);


                out.writeObject(m);
                out.flush();

                for (int i = 0; i < counter; i++) {

                    m = (CommunicationMessage) in.readObject();

                    for (String fileName : temp) {
                        if (fileName.equals(m.getFileName()) == true) {

                            flag = true;
                        }
                    }

                    if (flag == false) {

                        m.setMessageID(CommunicationMessage.MessageType.FILE_NOT_ON_SERVER);
                        out.writeObject(m);
                        out.flush();

                        m = (CommunicationMessage) in.readObject();

                        reciveFile(path + "\\" + userName + "\\" + m.getFileName(), m.getFileSize());


                    } else {
                        m.setMessageID(CommunicationMessage.MessageType.FILE_ON_SERVER);
                        out.writeObject(m);
                        out.flush();
                    }

                    flag = false;

                }


            } else {
                System.out.println("Blad");
                System.out.println(m.getMessageID());
            }

            //////////////////////////////////////////////////////

            m = (CommunicationMessage) in.readObject();

            String[] temp;
            File ff = new File(path + "\\" + userName);
            temp = ff.list();

            m.setMessageID(CommunicationMessage.MessageType.NUMBER_OF_FILE_TO_CHCEK);
            m.setNumberOfFileToCheck(temp.length);
            out.writeObject(m);
            out.flush();

            m = (CommunicationMessage) in.readObject();
            for (String fileName : temp) {

                m.setFileName(fileName);
                m.setMessageID(CommunicationMessage.MessageType.CHECK_FILE);
                out.writeObject(m);
                out.flush();

                m = (CommunicationMessage) in.readObject();

                if (m.getMessageID() == CommunicationMessage.MessageType.FILE_NOT_ON_USER) {


                    m.setFileSize(checkFileSize(path + "\\" + userName + "\\" + fileName));
                    m.setFileName(fileName);
                    m.setMessageID(CommunicationMessage.MessageType.FILE);
                    out.writeObject(m);
                    out.flush();

                    sendFile(path + "\\" + userName + "\\" + fileName);

                    m = (CommunicationMessage) in.readObject();

                    if (m.getMessageID().equals(CommunicationMessage.MessageType.READY)) {

                    } else {
                        System.out.println("Blad przy potwierdzaniu");
                        System.out.println(m.getMessageID());
                    }


                } else if (m.getMessageID() == CommunicationMessage.MessageType.FILE_ON_USER) {

                }
            }

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
