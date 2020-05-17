package sample;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.*;

public class Controller implements Initializable {

    private String userName;
    private String path;
    private ConnectionClass connectionClass = null;

    @FXML
    private Label clientStatus;

    @FXML
    private ListView fileList;

    @FXML
    private ListView availableUsers;

    @FXML
    private Button send;

    @FXML
    public ComboBox<String> choseClient;

    @FXML
    private ComboBox<String> choseFile;

    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private ObservableList<String> observableListFiles = FXCollections.observableArrayList();
    private ObservableList<String> observableListClients = FXCollections.observableArrayList();
    private BlockingQueue<CommunicationMessage> queue = new ArrayBlockingQueue<CommunicationMessage>(10);

    private BlockingQueue<String> clientStatusQueue = new ArrayBlockingQueue<String>(10);

    void setUserNameAndPath(String userName, String path) {
        this.userName = userName;
        this.path = path;
    }

    synchronized void setClientStatus(String status) {

        Platform.runLater(() -> {
            clientStatus.setText(status);
        });
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
        }
    }


    void initUserFileAndServerFile() {

        try {

            CommunicationMessage m = new CommunicationMessage();
            String[] temp;
            File f = new File(path);
            temp = f.list();

            m.setMessageID(CommunicationMessage.MessageType.NUMBER_OF_FILE_TO_CHCEK);
            m.setNumberOfFileToCheck(temp.length);
            connectionClass.out.writeObject(m);
            connectionClass.out.flush();

            m = (CommunicationMessage) connectionClass.in.readObject();

            for (String fileName : temp) {

                m.setFileName(fileName);
                m.setMessageID(CommunicationMessage.MessageType.CHECK_FILE);
                connectionClass.out.writeObject(m);
                connectionClass.out.flush();

                m = (CommunicationMessage) connectionClass.in.readObject();

                if (m.getMessageID() == CommunicationMessage.MessageType.FILE_NOT_ON_SERVER) {

                    m.setFileSize(connectionClass.checkFileSize(path + "\\" + fileName));
                    m.setFileName(fileName);
                    m.setMessageID(CommunicationMessage.MessageType.FILE);
                    connectionClass.out.writeObject(m);
                    connectionClass.out.flush();
                    connectionClass.sendFile(path + "\\" + fileName);
                } else if (m.getMessageID() == CommunicationMessage.MessageType.FILE_ON_SERVER) {

                }
            }

            m.setMessageID(CommunicationMessage.MessageType.READY);
            connectionClass.out.writeObject(m);
            connectionClass.out.flush();

            m = (CommunicationMessage) connectionClass.in.readObject();

            if (m.getMessageID() == CommunicationMessage.MessageType.NUMBER_OF_FILE_TO_CHCEK) {

                File ff = new File(path);
                temp = ff.list();
                int counter = m.getNumberOfFileToCheck();
                boolean flag = false;

                m.setMessageID(CommunicationMessage.MessageType.READY);
                connectionClass.out.writeObject(m);
                connectionClass.out.flush();

                for (int i = 0; i < counter; i++) {
                    m = (CommunicationMessage) connectionClass.in.readObject();

                    for (String fileName : temp) {

                        if (fileName.equals(m.getFileName()) == true) {
                            flag = true;
                        }
                    }

                    if (flag == false) {

                        m.setMessageID(CommunicationMessage.MessageType.FILE_NOT_ON_USER);
                        connectionClass.out.writeObject(m);
                        connectionClass.out.flush();

                        m = (CommunicationMessage) connectionClass.in.readObject();
                        connectionClass.reciveFile(path + "\\" + m.getFileName(), m.getFileSize());

                        m.setMessageID(CommunicationMessage.MessageType.READY);
                        connectionClass.out.writeObject(m);
                        connectionClass.out.flush();
                    } else {

                        m.setMessageID(CommunicationMessage.MessageType.FILE_ON_USER);
                        connectionClass.out.writeObject(m);
                        connectionClass.out.flush();
                    }
                    flag = false;
                }
            } else {

                System.out.println("Blad");
                System.out.println(m.getMessageID());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void updateUsersList(ArrayList<String> list) {

        Platform.runLater(() -> {
            try {
                for (String usr : list) {
                    if (observableListClients.contains(usr) == false) {
                        observableListClients.add(usr);
                    }
                }

                for (String usr : observableListClients) {
                    if (list.contains(usr) == false) {
                        observableListClients.remove(usr);
                    }
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        });
    }

    void initObservableListFiles() {

        String[] temp;
        File f = new File(path);
        temp = f.list();

        for (String pathname : temp) {
            observableListFiles.add(pathname);
        }

        executor.execute(new Thread(this::handleThread));
        executor.execute(new Thread(this::sendFileToServer));
        executor.execute(new Thread(this::serverOperationHandler));
    }


    void serverOperationHandler() {

        CommunicationMessage m = null;

        try {

            while (true) {

                m = (CommunicationMessage) connectionClass.in.readObject();

                if (m.getMessageID().equals(CommunicationMessage.MessageType.FILE_NOT_ON_SERVER)) {
                    m.setMessageID(CommunicationMessage.MessageType.FILE);
                    queue.put(m);

                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.CHECK_FILE)) {

                    String[] temp;
                    File f = new File(path);
                    temp = f.list();
                    boolean flag = false;

                    for (String file : temp) {
                        if (file.equals(m.getFileName())) {
                            flag = true;
                            break;
                        }
                    }

                    if (flag == false) {

                        m.setMessageID(CommunicationMessage.MessageType.FILE_NOT_ON_USER);
                        queue.put(m);
                    } else {

                    }

                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.FILE)) {

                    setClientStatus("Pobieram...");
                    connectionClass.reciveFile(path + "\\" + m.getFileName(), m.getFileSize());
                    setClientStatus("Sprawdzam...");
                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.UPDATE_CLIENT_LIST)) {

                    updateUsersList(m.getClientList());
                } else {

                    System.out.println("Blad");
                }
            }
        } catch (SocketException e) {
            System.out.println(e.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendFileToServer() {

        CommunicationMessage m = new CommunicationMessage();

        try {
            while (true) {
                m = queue.take();
                System.out.println(m.getMessageID());

                if (m.getMessageID().equals(CommunicationMessage.MessageType.CHECK_FILE)) {

                    connectionClass.out.writeObject(m);
                    connectionClass.out.flush();
                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.FILE)) {

                    setClientStatus("Wysylam...");
                    m.setFileSize(connectionClass.checkFileSize(path + "\\" + m.getFileName()));
                    connectionClass.out.writeObject(m);
                    connectionClass.out.flush();

                    try {
                        connectionClass.sendFile(path + "\\" + m.getFileName());
                    } catch (Exception e) {
                        Thread.sleep(1000);
                        try {
                            connectionClass.sendFile(path + "\\" + m.getFileName());
                        } catch (Exception e2) {
                            Thread.sleep(1000);
                            connectionClass.sendFile(path + "\\" + m.getFileName());
                        }
                    }

                    setClientStatus("Sprawdzam...");

                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.FILE_TO_OTHER_USER)) {

                    m.setFileSize(connectionClass.checkFileSize(path + "\\" + choseFile.getValue()));
                    setClientStatus("Wysylam...");
                    connectionClass.out.writeObject(m);
                    connectionClass.out.flush();
                    connectionClass.sendFile(path + "\\" + m.getFileName());
                    setClientStatus("Sprawdzam...");
                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.FILE_NOT_ON_USER)) {

                    connectionClass.out.writeObject(m);
                    connectionClass.out.flush();
                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.MODIFY_FILE)) {

                    setClientStatus("Wysylam...");
                    m.setFileSize(connectionClass.checkFileSize(path + "\\" + m.getFileName()));
                    connectionClass.out.writeObject(m);
                    connectionClass.out.flush();

                    try {
                        connectionClass.sendFile(path + "\\" + m.getFileName());
                    } catch (Exception e) {
                        Thread.sleep(1000);
                        try {
                            connectionClass.sendFile(path + "\\" + m.getFileName());
                        } catch (Exception e2) {
                            Thread.sleep(1000);
                            connectionClass.sendFile(path + "\\" + m.getFileName());
                        }
                    }
                    setClientStatus("Sprawdzam...");

                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.DELETE_FILE)) {

                    connectionClass.out.writeObject(m);
                    connectionClass.out.flush();
                } else if (m.getMessageID().equals(CommunicationMessage.MessageType.LOGOUT)) {

                    connectionClass.out.writeObject(m);
                    connectionClass.out.flush();
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleThread() {

        try {
            WatchService watchService
                    = FileSystems.getDefault().newWatchService();

            Path pathDirectory = Paths.get(path);

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
                            observableListFiles.addAll(event.context().toString());
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
                            observableListFiles.removeAll(event.context().toString());
                        });

                        try {
                            CommunicationMessage m = new CommunicationMessage();
                            m.setMessageID(CommunicationMessage.MessageType.DELETE_FILE);
                            m.setFileName(event.context().toString());
                            queue.put(m);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (event.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY) == true) {

                        try {
                            CommunicationMessage m = new CommunicationMessage();
                            m.setMessageID(CommunicationMessage.MessageType.MODIFY_FILE);
                            m.setFileName(event.context().toString());
                            queue.put(m);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setConnectionClass(ConnectionClass connectionClass) {
        this.connectionClass = connectionClass;
    }

    public ConnectionClass getConnectionClass() {
        return connectionClass;
    }

    @FXML
    void sendFileToUser() {

        CommunicationMessage m = new CommunicationMessage();
        m.setMessageID(CommunicationMessage.MessageType.FILE_TO_OTHER_USER);
        m.setFileName(choseFile.getValue());
        m.setUserNameFileSend(choseClient.getValue());

        try {
            queue.put(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onShutdownApp() {

        try {
            CommunicationMessage m = new CommunicationMessage();
            m.setMessageID(CommunicationMessage.MessageType.LOGOUT);
            queue.put(m);
        } catch (Exception e) {
            e.printStackTrace();
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        connectionClass.closeConnectionFile();
        connectionClass.closeConnection();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        choseFile.setPromptText("Choose a file");
        choseClient.setPromptText("Select the user");
        choseFile.setItems(observableListFiles);
        choseClient.setItems(observableListClients);
        fileList.setItems(observableListFiles);
        availableUsers.setItems(observableListClients);
        clientStatus.setText("Ready");
    }
}