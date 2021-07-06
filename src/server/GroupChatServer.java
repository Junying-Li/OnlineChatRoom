package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class GroupChatServer {
    private static ServerSocket serverSocket;
    private static Socket socket;
    public static List<GroupChatServerThread> threadList = new ArrayList<GroupChatServerThread>();

    private static void runServer() {
        try {
            serverSocket = new ServerSocket(6209);
            System.out.println("Server is running!");
            System.out.println("Waiting for connection...");
            while (true) {
                socket = serverSocket.accept();
                GroupChatServerThread thread = new GroupChatServerThread(socket);
                thread.start();
                threadList.add(thread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        runServer();
    }
}