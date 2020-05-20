package sample;

import javafx.scene.layout.Pane;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Klasa obsulugujaca serwer, polaczenia od klientow
 */
public class ServerClass {

    public ServerSocket serverSocket;
    public int serverSocketDefaultPort = 5555;
    public int serverSocketClientPort = serverSocketDefaultPort + 1;
    public ArrayList<String> clientList;
    public Controller controller;
    ExecutorService executor = Executors.newFixedThreadPool(30);

    /**
     * Zamyka serwer, porty i otwarte strumienie, zabija watki
     */
    void onClose() {

        executor.shutdown();
        try {
            if (!executor.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        stopServer();

    }

    /**
     * Startuje serwer i inicjalizuje liste klientow
     *
     * @param controller controller do aplikacji serwera
     */
    public void startServer(Controller controller) {
        clientList = new ArrayList<String>(100);
        this.controller = controller;
        executor.execute(new Thread(this::startServerHandle));

        this.controller.getPane().getScene().getWindow().setOnCloseRequest(e -> {
            onClose();
        });
    }

    /**
     * Akceptuje polaczenia przychodzace od klientow, tworzy nowy watek ktory odpytuje klienta i przyznaje mu pule portow
     */
    private void startServerHandle() {
        try {
            serverSocket = new ServerSocket(this.serverSocketDefaultPort);
            while (true) {
                executor.execute(new ClientHandler(serverSocket.accept(), serverSocketClientPort, this));
                serverSocketClientPort = serverSocketClientPort + 3;
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    /**
     * Zamyka glowny strumien serwera.
     */
    public void stopServer() {
        try {
            serverSocket.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }


    /**
     * Prywatna klasa ktora, odpytuje uzytkownika o nazwe  i uruchamia watek obsugujacy komunikacje pomiedzy nimi
     */
    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private int newClientPort;
        ServerClass server = null;

        /**
         * Konstruktor
         *
         * @param newClientPort port przyznany przez serwer dla klienta
         * @param server        referencja do klasy ServerClass
         * @param socket        glowny socket serwera
         */
        public ClientHandler(Socket socket, int newClientPort, ServerClass server) {
            this.clientSocket = socket;
            this.newClientPort = newClientPort;
            this.server = server;
        }

        /**
         * Watek obslugujacy pobranie nazwy uzytkownika, startuje watek obslugujacy komunikacje miedzy nimi na przyznanym porcie
         */
        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                CommunicationMessage login = null;
                login = (CommunicationMessage) in.readObject();

                server.executor.execute(new ClientCommunicationHandler(newClientPort, server.clientList, server.controller, login.getUserName()));

                CommunicationMessage message = new CommunicationMessage();
                message.setPort(this.newClientPort);
                out.writeObject(message);

                in.close();
                out.close();
                clientSocket.close();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }
}
