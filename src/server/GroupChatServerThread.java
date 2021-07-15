package server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("Duplicates")
public class GroupChatServerThread extends Thread {
    private final Socket socket;
    private InputStream input;
    private OutputStream output;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private boolean isConnected = true;

    public GroupChatServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            input = socket.getInputStream();
            output = socket.getOutputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(input));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(output));
            username = bufferedReader.readLine();

            if (checkUsernameExistence(username)) {
                sendInfo(bufferedWriter, "/isExistedUsername");
                try {
                    if (bufferedWriter != null) {
                        bufferedWriter.close();
                    }
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                    if (output != null) {
                        output.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                    GroupChatServer.threadList.remove(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                welcome();
                chat();
                close();
                goodBye();
            };
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkUsernameExistence (String username) {
        boolean isExistedUsername = false;
        for (GroupChatServerThread thread : GroupChatServer.threadList) {
            if (username.equals(thread.username) && thread != this) {
                isExistedUsername = true;
                break;
            }
        }
        return isExistedUsername;
    }

    private void welcome () {
        sendInfo(bufferedWriter, "\n*** Connected Successful. *** \n*** " + username + ", welcome to ChaToGo, a free online group chat room! :) *** \n");
        System.out.println("[" + username + "] connected." + getTime());
        for (GroupChatServerThread thread : GroupChatServer.threadList) {
            if (thread != this) {
                // Broadcast who's coming in
                sendInfo(thread.bufferedWriter, "\n [ Broadcast ] " + username + " entered this room.\n");
                addUser(thread.bufferedWriter, username);
                addUser(this.bufferedWriter, thread.username);
            }
        }
    }

    private void chat () {
        String msg = null;
        try {
            msg = bufferedReader.readLine();
            System.out.println(username + ": " + msg);
        } catch (IOException e) {
            isConnected = false;
        }
        while (!msg.equals("/Disconnect") && isConnected) {
            for (GroupChatServerThread thread : GroupChatServer.threadList) {
                String[] m = msg.split(" ");
                String receiver = m[0].substring(1);
                String message = msg.substring(m[0].length()+1);
                if (receiver.equals("Everyone")) {
                    if (!thread.username.equals(username)) {
                        sendInfo(thread.bufferedWriter, username + ": " + message + " " + getTime());
                    }
                } else {
                    if (thread.username.equals(receiver)) {
                        sendInfo(thread.bufferedWriter, username + " whispers to you: " + message + " " + getTime());
                    }
                }
            }
            try {
                msg = bufferedReader.readLine();
                System.out.println(username + ": " + msg);
            } catch (IOException e) {
                isConnected = false;
            }
        }
        sendInfo(bufferedWriter, "/Disconnect");
    }

    private void close() {
        closeAll();
        GroupChatServer.threadList.remove(this);
    }

    private void goodBye () {
        // Broadcast who left the room
        for (GroupChatServerThread thread : GroupChatServer.threadList) {
            if (thread != this) {
                sendInfo(thread.bufferedWriter, "\n [ Broadcast ] " + username + " left this room.\n");
                removeUser(thread.bufferedWriter, username);
            }
        }
    }

    private void sendInfo(BufferedWriter bWriter, String msg) {
        try {
            bWriter.write(msg);
            bWriter.newLine();
            bWriter.flush();
        } catch (IOException e) {
//            e.printStackTrace();
            isConnected = false;
        }
    }

    private void addUser (BufferedWriter bWriter, String username) {
        sendInfo(bWriter, "/add " + username);
    }

    private void removeUser (BufferedWriter bWriter, String username) {
        sendInfo(bWriter, "/remove " + username);
    }

    private String getTime () {
        @SuppressWarnings("SimpleDateFormat") SimpleDateFormat time = new SimpleDateFormat();
        time.applyPattern("HH:mm:ss yyyy/MM/dd");
        Date date = new Date();
        return "(" + time.format(date) + ")";
    }

    private void closeAll () {
        try {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (socket != null) {
                socket.close();
                System.out.println("[" + username + "] disconnected." + getTime());
            }
            GroupChatServer.threadList.remove(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
