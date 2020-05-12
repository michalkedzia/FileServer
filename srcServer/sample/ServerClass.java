package sample;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerClass {

    public ServerSocket serverSocket;
    public int serverSocketDefaultPort = 5555;
    public int serverSocketClientPort = serverSocketDefaultPort + 1;
    ArrayList<String> clientList;
    public Controller controller;

    //


    public void startServer(Controller controller) {
        clientList = new ArrayList<String>();
        this.controller = controller;
        Thread thread = new Thread(this::startServerHandle);
        thread.start();
    }

    private void startServerHandle() {
        try {
            serverSocket = new ServerSocket(this.serverSocketDefaultPort);
            while (true) {
                new ClientHandler(serverSocket.accept(), serverSocketClientPort, serverSocket, clientList, controller).start();
                serverSocketClientPort = serverSocketClientPort + 3;
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        } finally {
            stopServer();
        }
    }

    public void stopServer() {
        try {
            serverSocket.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private int newClientPort;

        Socket socket;
        ServerSocket serverSocket;
        ArrayList<String> clientList;
        Controller controller;


        public ClientHandler(Socket socket, int newClientPort, ServerSocket serverSocket, ArrayList<String> clientList, Controller controller) {
            this.clientSocket = socket;
            this.newClientPort = newClientPort;
            this.socket = socket;
            this.serverSocket = serverSocket;
            this.clientList = clientList;
            this.controller = controller;
        }


        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                CommunicationMessage login = null;
                login = (CommunicationMessage) in.readObject();


                new ClientCommunicationHandler(newClientPort, clientList, controller, login.getUserName()).start();

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
