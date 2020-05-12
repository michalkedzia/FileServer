package sample;

class ConnectException extends Exception {
    String er;

    public ConnectException(String er) {
        this.er = er;
    }

    public String toString() {
        return "Blad przy polaczeniu: " + er;
    }
}