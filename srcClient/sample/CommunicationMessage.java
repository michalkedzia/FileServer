package sample;

import java.io.*;
import java.util.ArrayList;

public class CommunicationMessage implements Serializable {

    private MessageType messageID;
    private int port;
    private String userName;
    private ArrayList<String> clientList;
    private String fileName;
    private long fileSize;
    private int numberOfFileToCheck;
    private String userNameFileSend;
    public enum MessageType {
        CHECK_FILE,
        FILE_ON_SERVER,
        FILE_NOT_ON_SERVER,
        FILE_ON_USER,
        FILE_NOT_ON_USER ,
        NUMBER_OF_FILE_TO_CHCEK ,
        READY ,
        FILE,
        FILE_TO_USER,
        FILE_TO_OTHER_USER,
        MODIFY_FILE,
        DELETE_FILE,
        UPDATE_CLIENT_LIST,
        LOGOUT
    }

    public MessageType getMessageID() {
        return messageID;
    }

    public void setMessageID(MessageType messageID) {
        this.messageID = messageID;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public ArrayList<String> getClientList() {
        return clientList;
    }

    public void setClientList(ArrayList<String> clientList) {
        this.clientList = clientList;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getNumberOfFileToCheck() {
        return numberOfFileToCheck;
    }

    public void setNumberOfFileToCheck(int numberOfFileToCheck) {
        this.numberOfFileToCheck = numberOfFileToCheck;
    }

    public String getUserNameFileSend() {
        return userNameFileSend;
    }

    public void setUserNameFileSend(String userNameFileSend) {
        this.userNameFileSend = userNameFileSend;
    }
}
