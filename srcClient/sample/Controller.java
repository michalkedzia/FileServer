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

/**
 * Klasa obsugująca wszystkie operacje pomiedzy klientem oraz serwerem.
 */
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

    /**
     * Ustawia nazwe uzytkownika oraz sciezke do katalogu
     *
     * @param path
     * @param userName
     */
    void setUserNameAndPath(String userName, String path) {
        this.userName = userName;
        this.path = path;
    }

    /**
     * Ustawia aktualny status kliena.
     *
     * @param status Status do wyświetlania w GUI.
     */
    synchronized void setClientStatus(String status) {

        Platform.runLater(() -> {
            clientStatus.setText(status);
        });
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
    }

    /**
     * Odpytuje serwer o nowe pliki dostepne do klienta i wysyła pliki z katalogu klienta, ktorych nie ma na serwerze.
     */
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

    /**
     * Odswieza liste uzytkownikow dostepnych na serwerze.
     *
     * @param list Lista aktualnie zalogowanych uzytkownikow uzytkownikow
     */
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

    /**
     * Dodaje pliki do listy plikow w GUI.
     * Uruchamia watki obslugujace watchDirectory,serverOperationHandler, sendToServer.
     */
    void initObservableListFiles() {

        String[] temp;
        File f = new File(path);
        temp = f.list();

        for (String pathname : temp) {
            observableListFiles.add(pathname);
        }

        executor.execute(new Thread(this::watchDirectory));
        executor.execute(new Thread(this::sendToServer));
        executor.execute(new Thread(this::serverOperationHandler));
    }

    /**
     * Odczytuje operacje od serwera, dodaje je do kolejki zadan do wykonania lub od razu je wykonuje np.
     * wysyla plik dodany do folderu.
     */
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

    /**
     * Zdejmuje operacje z kolejki do wykonania i wykonuje je, np.  wysyla plik do serwera.
     */
    void sendToServer() {

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

    /**
     * Obserwuje lokalny katalog i reaguje na jego zmiany, aktualizujac liste plikow lub umieszczajac operacje w kolejce.
     */
    private void watchDirectory() {

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

    /**
     * Ustawia klase ConnectionClass.
     *
     * @param connectionClass Referancja do klasy ConnectionClass
     */
    public void setConnectionClass(ConnectionClass connectionClass) {
        this.connectionClass = connectionClass;
    }

    /**
     * Pobiera klase ConnectionClass
     */
    public ConnectionClass getConnectionClass() {
        return connectionClass;
    }

    /**
     * Dodaje do kolejki operacje wyslania pliku do innego uzytkownika. Parametry pobiera z GUI.
     */
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

    /**
     * Konczy działanie aplikacji po nacisnieciu na exit. Zabija waatki dzilając w aplikacji, wylogowuje sie z serwera.
     */
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

    /**
     * Inicializacja parametrow klasy, zaraz po utworzeniu obiektu danej klasy.
     *
     * @param url            url
     * @param resourceBundle resourceBundle
     */
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