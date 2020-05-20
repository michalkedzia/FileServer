package sample;

import java.io.*;
import java.net.*;

/**
 * Klasa obslugujace operacje na strumieniach danych pomiedzy serweren i klientem.
 */
public class ConnectionClass {

    public Socket clientSocket;
    public ObjectOutputStream out;
    public ObjectInputStream in;

    public DataOutputStream dos;
    public DataInputStream dis;
    public Socket clientSocketFile;

    /**
     * Otwiera polaczenie na danym porcie oraz strumienie do komunikacji pomiedzy serwerem.
     *
     * @param ip   ip
     * @param port Numer portu
     * @throws ConnectException
     */
    private void openConnection(String ip, int port) throws ConnectException {
        try {
            this.clientSocket = new Socket(ip, port);
        } catch (IOException exception) {
            try {
                System.out.println("Blad. Proba ponownego polaczenia z serwerem");
                this.clientSocket = new Socket(ip, port);
            } catch (IOException e) {
                throw new ConnectException("Nie mozna ustanowic polaczenia z serwerem.  " + e.toString());
            }

        } catch (SecurityException exception) {
            throw new ConnectException(
                    "Security manager nie pozwala na polaczenie sie z serwerem  " + exception.toString());
        } catch (IllegalArgumentException exception) {
            throw new ConnectException("Podany zly zakres portu 0-65535  " + exception.toString());
        }

        try {
            this.out = new ObjectOutputStream(clientSocket.getOutputStream());
            this.in = new ObjectInputStream(clientSocket.getInputStream());

        } catch (IOException exception) {
            throw new ConnectException("Blad przy tworzenius strumienii I/O  " + exception.toString());

        } catch (NullPointerException exception) {
            throw new ConnectException(exception.toString());
        }
    }

    /**
     * Zamyka port oraz struminie.
     */
    public void closeConnection() {
        try {
            this.in.close();
            this.out.close();
            this.clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Rejestruje/loguje uzytkowika do serwera.
     *
     * @param login Nazwa uzytkownika logujacego sie do serwera
     */
    public void init(String login) {

        try {
            openConnection("127.0.0.1", 5555);
        } catch (ConnectException e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {

            CommunicationMessage loginMessage = new CommunicationMessage();
            loginMessage.setUserName(login);
            this.out.writeObject(loginMessage);

            CommunicationMessage portMessage = null;
            portMessage = (CommunicationMessage) this.in.readObject();
            closeConnection();


            openConnectionFile("127.0.0.1", portMessage.getPort() + 1);
            openConnection("127.0.0.1", portMessage.getPort());
        } catch (IOException | ClassNotFoundException | ConnectException e) {

            e.printStackTrace();
        }

    }

    /**
     * Wysyła plik na serwer.
     *
     * @param path Sciezka do pliku.
     * @throws IOException
     */
    public synchronized void sendFile(String path) throws IOException {
        FileInputStream fis = new FileInputStream(path);
        byte[] buffer = new byte[4096];

        while (fis.read(buffer) > 0) {
            dos.write(buffer);
        }
        fis.close();
    }

    /**
     * Odbiera plik wysłany z serwera.
     *
     * @param fileSize Rozmiar pliku.
     * @param path     Sciezka do miejsca w ktorym ma zostac zapisany plik.
     * @throws IOException
     */
    public synchronized void reciveFile(String path, long fileSize) throws IOException {
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

    /**
     * Sprawdza i zwraca rozmiar pliku.
     *
     * @param path Sciezka do pliku.
     * @return Rozmiar pliku
     */
    public synchronized long checkFileSize(String path) {
        File f = new File(path);
        return f.length();
    }

    /**
     * Otwiera port i strumienie dla wysylania/odbierania plikow.
     *
     * @param port Numer portu.
     * @param ip   ip
     */
    public void openConnectionFile(String ip, int port) {
        try {
            this.clientSocketFile = new Socket(ip, port);
            this.dos = new DataOutputStream(clientSocketFile.getOutputStream());
            this.dis = new DataInputStream(clientSocketFile.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Zamyka port oraz strumienie dla wysylania/odbierania plikow z serwera.
     */
    public void closeConnectionFile() {
        try {
            this.dos.close();
            this.clientSocketFile.close();
            this.dis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Zwraca clientSocket
     *
     * @return Socket
     */
    public Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * Zwraca strumien wyjsciowy
     *
     * @return ObjectOutputStream
     */
    public ObjectOutputStream getOut() {
        return out;
    }

    /**
     * Zwraca strumien wejsciowy
     *
     * @return ObjectInputStream
     */
    public ObjectInputStream getIn() {
        return in;
    }
}
