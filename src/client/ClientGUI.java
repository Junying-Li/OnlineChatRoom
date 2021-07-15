package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;

@SuppressWarnings("Duplicates")
public class ClientGUI extends ClientFrame {
    private static Socket client = null;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private ClientThread clientThread;
    private static boolean isConnected = false;
    private final DefaultListModel<String> userList;
    private final JList<String> userListBoard;

    private final JComboBox<String> usernameBox = new JComboBox<>();
    private final JButton sendButton = new JButton("Send");
    private final JButton connectButton = new JButton("Connect");
    private final JButton disconnectButton = new JButton("Disconnect");
    private final JTextField sendInputTF = new JTextField("Input message here.",40);
    private final JTextArea messageBoard = new JTextArea(20, 40);
    private final JLabel hostLabel = new JLabel("Host: ");
    private final JLabel portLabel = new JLabel("   Port: ");
    private final JLabel usernameLabel = new JLabel("   Username: ");
    private final JLabel sendTo = new JLabel("              Send To:");
    private final JLabel blank = new JLabel("     ");
    private final JTextField hostTF = new JTextField("127.0.0.1", 8);
    private final JTextField portTF = new JTextField("6209", 5);
    private final JTextField usernameTF = new JTextField(10);
    private final JPanel headPanel = new JPanel();
    private final JPanel sendPanel = new JPanel();
    private final JScrollPane messagePanel = new JScrollPane();
    private final JPanel userPanel = new JPanel();

    public ClientGUI () throws UnknownHostException {
        messageBoard.setEditable(false);
        userList = new DefaultListModel<>();
        userListBoard = new JList<>(userList);
        usernameBox.setPrototypeDisplayValue("XXXXXXXXXX");
        usernameBox.setEnabled(false);
        usernameBox.setEditable(false);
        disconnectButton.setEnabled(false);
        sendButton.setEnabled(false);
        sendInputTF.setEditable(false);

        sendButton.addActionListener(e -> {
            if (!isConnected) {
                JOptionPane.showMessageDialog(null, "Please connect to the server.", "Warning", JOptionPane.WARNING_MESSAGE);
            } else {
                String msg = sendInputTF.getText().trim();
                if (msg.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "You can't send a blank message.", "Warning", JOptionPane.WARNING_MESSAGE);
                } else {
                    try {
                        bufferedWriter.write("/" + usernameBox.getSelectedItem() + " " + msg);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (usernameBox.getSelectedItem().equals("Everyone")) {
                        messageBoard.append("You: " + msg + " " + getTime() + "\n");
                    } else {
                        messageBoard.append("You whisper to " + usernameBox.getSelectedItem() + ": " + msg + " " + getTime() + "\n");
                    }
                    sendInputTF.setText(null);
                }
            }
        });

        connectButton.addActionListener(e -> {
            if (!isConnected) {
                String username = usernameTF.getText();
                // Check username validation
                if (checkNameValidation(username)) {
                    connectToServer();
                    if (!isConnected) {
                        return;
                    }
                    try {
                        bufferedWriter.write(username);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    // Check username existed or not
                    String serverMsg = readMsg();
//                    messageBoard.append(serverMsg + "\n");
                    if (checkUserExistence(serverMsg)) {
                        JOptionPane.showMessageDialog(null, "This username has already been taken, please re-entry!", "Warning", JOptionPane.WARNING_MESSAGE);
                        closeAll(true);
                        setAfterDisconnect();
                    } else {
                        // Start thread
                        clientThread = new ClientThread(client, bufferedReader, bufferedWriter, messageBoard, userList, usernameBox);
                        clientThread.start();
                        userList.addElement(usernameTF.getText());
                        setAfterConnected();
                    }
                }
            }
        });

        disconnectButton.addActionListener(e -> {
            if (isConnected) {
                disconnectToServer();
                setAfterDisconnect();
            } else {
                JOptionPane.showMessageDialog(null, "Please connect to the server first.", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private boolean checkNameValidation (String username) {
        boolean validation = false;
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please entry a username.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        else if (username.contains(" ")) {
            JOptionPane.showMessageDialog(null, "Username can't contain space.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        else if (username.equals("Everyone")) {
            JOptionPane.showMessageDialog(null, "You can't use 'Everyone' as username.", "Warning", JOptionPane.WARNING_MESSAGE);
        } else {
            validation = true;
        }
        return validation;
    }

    private boolean checkUserExistence (String serverMsg) {
        return serverMsg.equals("/isExistedUsername");
    }

    private void connectToServer () {
        try {
            String host = hostTF.getText().trim();
            String stringPort = portTF.getText();
            if (host.isEmpty() || stringPort.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please make sure to input correct host and port address.", "Warning", JOptionPane.WARNING_MESSAGE);
            } else {
                int port = Integer.parseInt(stringPort);
                client = new Socket(host, port);
                try {
                    if (client != null) {
                        bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        bufferedWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                        isConnected = true;
                    }
                }catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Connection failed. Please check host and port address.", "Warning", JOptionPane.WARNING_MESSAGE);
                    isConnected = false;
                    closeAll(false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Connection failed. Please check host and port address.", "Warning", JOptionPane.WARNING_MESSAGE);
            isConnected = false;
        }
    }

    private String readMsg () {
        String msg = "\n*** Connection is lost! ***\n";
        // 0703
        if (!isConnected) {
            closeAll(true);
            setAfterDisconnect();
            return msg;
        }
        //
        try {
            msg = this.bufferedReader.readLine();
        } catch (IOException e) {
//            e.printStackTrace();
            isConnected = false;
            closeAll(true);
            setAfterDisconnect();
        }
        return msg;
    }

    private void disconnectToServer () {
        try {
            bufferedWriter.write("/Disconnect");
            bufferedWriter.newLine();
            bufferedWriter.flush();
//            clientThread.stop();
            closeAll(false);
            setAfterDisconnect();
            isConnected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeAll (boolean warning) {
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
            if (client.isClosed() && warning) {
                JOptionPane.showMessageDialog(null, "Connection is closed.", "Warning", JOptionPane.WARNING_MESSAGE);
                isConnected = false;
            }
        } catch (IOException e) {
//            e.printStackTrace();
        }
    }

    private void setAfterConnected () {
        usernameTF.setEditable(false);
        hostTF.setEditable(false);
        portTF.setEditable(false);
        connectButton.setEnabled(false);
        disconnectButton.setEnabled(true);
        sendInputTF.setEditable(true);
        sendButton.setEnabled(true);
        usernameBox.setEnabled(true);
        usernameBox.setEditable(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    private void setAfterDisconnect () {
        usernameTF.setEditable(true);
        hostTF.setEditable(true);
        portTF.setEditable(true);
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        sendInputTF.setEditable(false);
        sendButton.setEnabled(false);
        usernameBox.setEnabled(false);
        usernameBox.removeAllItems();
        userList.removeAllElements();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private String getTime () {
        @SuppressWarnings("SimpleDateFormat") SimpleDateFormat time = new SimpleDateFormat();
        time.applyPattern("HH:mm:ss yyyy/MM/dd");
        Date date = new Date();
        return "(" + time.format(date) + ")";
    }

    private void confirmQuitDialog() {
        int result = JOptionPane.showConfirmDialog(null, "Do you really want to quit?", "Quit", JOptionPane.YES_NO_OPTION);
        if (result==JOptionPane.OK_OPTION) {
            if (isConnected) {
                disconnectToServer();
                setAfterDisconnect();
            }
            System.exit(0);
        }
        else {
            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        confirmQuitDialog();
    }

    private void initGUI () {
        userListBoard.setPreferredSize(new Dimension(100, 310));
        userListBoard.setBackground(new Color(238, 238, 238));
        messageBoard.setBackground(new Color(238, 238, 238));
        messagePanel.setBackground(new Color(238, 238, 238));
        messageBoard.setLineWrap(true);
        userListBoard.setSelectionForeground(Color.BLUE);
        userListBoard.setSelectionBackground(new Color(238, 238, 238));
        this.setTitle(" *ChaToGo*     CS513 Class Project - Junying Li");

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        this.add(headPanel);
        this.add(messagePanel);
        this.add(userPanel);
        this.add(sendPanel);
        GridBagConstraints constraint= new GridBagConstraints();
        constraint.fill = GridBagConstraints.HORIZONTAL;
        constraint.gridx = 0;
        constraint.gridy = 0;
        constraint.gridwidth = 3;
        constraint.weightx = 0;
        constraint.weighty = 0;
        layout.setConstraints(headPanel, constraint);
        constraint.gridx = 0;
        constraint.gridy = 1;
        constraint.gridwidth = 1;
        constraint.weightx = 0;
        constraint.weighty = 0;
        layout.setConstraints(messagePanel, constraint);
        constraint.gridx = 1;
        constraint.gridy = 1;
        constraint.gridwidth = 1;
        constraint.weightx = 1;
        constraint.weighty = 0;
        layout.setConstraints(userPanel, constraint);
        constraint.gridx = 0;
        constraint.gridy = 2;
        constraint.weightx = 0;
        constraint.weighty = 0;
        layout.setConstraints(sendPanel, constraint);



        headPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        headPanel.add(hostLabel);
        headPanel.add(hostTF);
        headPanel.add(portLabel);
        headPanel.add(portTF);
        headPanel.add(usernameLabel);
        headPanel.add(usernameTF);
        headPanel.add(connectButton);
        headPanel.add(disconnectButton);
        sendPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        sendPanel.add(sendTo);
        sendPanel.add(usernameBox);
        sendPanel.add(blank);
        sendPanel.add(sendInputTF);
        sendPanel.add(sendButton);
        userPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        userPanel.add(userListBoard);

        headPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
        messagePanel.setViewportView(messageBoard);
        messagePanel.setBorder(BorderFactory.createTitledBorder("Group Chat"));
        userPanel.setBorder(BorderFactory.createTitledBorder("Online Users"));
    }

    public static void main(String[] args) throws IOException {
        int width = 900;
        int height = 550;
        ClientGUI client = new ClientGUI();
        client.setSize(width, height);
        int screenWidth = (int)client.getToolkit().getScreenSize().getWidth();
        int screenHeight = (int)client.getToolkit().getScreenSize().getHeight();
        client.setLocation((screenWidth-width)/2, (screenHeight-height)/2);
        client.initGUI();
        client.addWindowListener(client);
        client.setVisible(true);
    }
}
