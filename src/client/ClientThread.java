package client;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public class ClientThread extends Thread {
    private final Socket client;
    private final BufferedReader bufferedReader;
    private final BufferedWriter bufferedWriter;
    private final JTextArea messageBoard;
    private final DefaultListModel<String> userList;
    private final JComboBox<String> usernameBox;
    boolean isRunning = true;
    public ClientThread (Socket client, BufferedReader bufferedReader, BufferedWriter bufferedWriter, JTextArea messageBoard, DefaultListModel<String> userList, JComboBox<String> usernameBox) {
        this.client = client;
        this.bufferedReader = bufferedReader;
        this.bufferedWriter = bufferedWriter;
        this.messageBoard = messageBoard;
        this.userList = userList;
        this.usernameBox = usernameBox;
    }

    @Override
    public void run() {
        usernameBox.addItem("Everyone");
        while (isRunning) {
            String receiveMsg = readMsg();
            if (receiveMsg.startsWith("/add")) {
                String newUser = receiveMsg.substring(5);
//                messageBoard.append("[" + newUser + "] entered the room.\n");
                usernameBox.addItem(newUser);
                userList.addElement(newUser);
                continue;
            }
            if (receiveMsg.startsWith("/remove")) {
                String rmUser = receiveMsg.substring(8);
//                messageBoard.append("[" + rmUser + "] left the room.\n");
                usernameBox.removeItem(rmUser);
                userList.removeElement(rmUser);
                continue;
            }
            if (receiveMsg.startsWith("/userList")) {
                String[] list = receiveMsg.substring(10).split(",");
                for (String user : list) {
                    userList.addElement(user);
                }
                continue;
            }
            if (receiveMsg.startsWith("/Disconnect")) {
                break;
            }
            if (receiveMsg.startsWith("/isExistedUsername")) {
                JOptionPane.showMessageDialog(null, "This username is already taken, please re-entry!", "Warning", JOptionPane.WARNING_MESSAGE);
                break;
            }
            messageBoard.append(receiveMsg + "\n");
        }
        try {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (client != null) {
                client.close();
            }
            assert client != null;
            if (client.isClosed()) {
                isRunning = false;
                JOptionPane.showMessageDialog(null, "You are disconnected.", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readMsg () {
        String msg = "*** Connection is lost!(test-thread) ***\n";
        // 0703
        if (!isRunning) {
            closeAll(bufferedReader);
            return msg;
        }
        //
        try {
            msg = this.bufferedReader.readLine();
        } catch (IOException e) {
//            e.printStackTrace();
            isRunning = false;
            closeAll(bufferedReader);
        }
        return msg;
    }

    private static void closeAll(Closeable...io) {
        for (Closeable temp : io) {
            try {
                if (null!=temp) {
                    temp.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
