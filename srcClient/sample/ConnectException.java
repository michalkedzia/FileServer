package sample;

/**
 * Wyjatek rzucany podczas proby połaczenia z serwerem.
 */
class ConnectException extends Exception {
    String er;

    /**
     * Konstruktor
     *
     * @param er Opis wyjatku
     */
    public ConnectException(String er) {
        this.er = er;
    }

    /**
     * toString
     *
     * @return error
     */
    public String toString() {
        return "Blad przy polaczeniu: " + er;
    }
}