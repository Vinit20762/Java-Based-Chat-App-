package JavaProject1;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class ChatClient extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton, connectButton, disconnectButton;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String userName;
    private volatile boolean isConnected = false;

    public ChatClient() {
        setTitle("Chat Client");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollChat = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);

        JPanel topPanel = new JPanel();
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(scrollChat, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Button actions ---
        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnectFromServer());
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    private void connectToServer() {
        userName = JOptionPane.showInputDialog(this, "Enter your name:");
        if (userName == null || userName.trim().isEmpty()) return;

        try {
            socket = new Socket("localhost", 5000);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF(userName); // send username first

            chatArea.append("Connected to server at localhost:5000\n");

            isConnected = true;
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);

            // Thread for listening messages
            new Thread(() -> {
                try {
                    String msg;
                    while (isConnected && (msg = dis.readUTF()) != null) {
                        chatArea.append(msg + "\n");
                    }
                } catch (IOException e) {
                    chatArea.append("Connection closed.\n");
                } finally {
                    disconnectFromServer();
                }
            }).start();

        } catch (IOException e) {
            chatArea.append("Unable to connect: " + e.getMessage() + "\n");
        }
    }

    private void sendMessage() {
        if (!isConnected) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        try {
            dos.writeUTF(text);
            inputField.setText("");
        } catch (IOException e) {
            chatArea.append("Error sending message.\n");
        }
    }

    private void disconnectFromServer() {
        try {
            isConnected = false;
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
            chatArea.append("Disconnected from server.\n");
        } catch (IOException ignored) {
        } finally {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient().setVisible(true));
    }
}
