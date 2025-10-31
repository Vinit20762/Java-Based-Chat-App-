import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer extends JFrame {
    private JTextArea chatArea;
    private JList<String> clientListUI;
    private DefaultListModel<String> clientListModel;
    private JButton startButton, stopButton;
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    // List of active clients
    private final ArrayList<ClientHandler> clients = new ArrayList<>();

    public ChatServer() {
        setTitle("Chat Server");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Left side: Chat messages ---
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollChat = new JScrollPane(chatArea);

        // --- Right side: Connected clients ---
        clientListModel = new DefaultListModel<>();
        clientListUI = new JList<>(clientListModel);
        JScrollPane scrollClients = new JScrollPane(clientListUI);
        scrollClients.setPreferredSize(new Dimension(150, 0));
        scrollClients.setBorder(BorderFactory.createTitledBorder("Connected Clients:"));

        // --- Top Buttons ---
        JPanel topPanel = new JPanel();
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);
        topPanel.add(startButton);
        topPanel.add(stopButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollChat, BorderLayout.CENTER);
        add(scrollClients, BorderLayout.EAST);

        // --- Button actions ---
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(5000);
            isRunning = true;
            chatArea.append("Server started on port 5000\n");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            // Separate thread to accept clients
            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket socket = serverSocket.accept();
                        DataInputStream dis = new DataInputStream(socket.getInputStream());
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                        // First message from client is the username
                        String clientName = dis.readUTF();

                        // Add client to list
                        ClientHandler handler = new ClientHandler(socket, clientName, dis, dos);
                        synchronized (clients) {
                            clients.add(handler);
                        }
                        clientListModel.addElement(clientName);

                        chatArea.append(clientName + " connected from " + socket.getInetAddress() + "\n");
                        broadcastMessage("SERVER: " + clientName + " joined the chat.");

                        handler.start();

                    } catch (IOException e) {
                        if (isRunning) e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            chatArea.append("Error starting server: " + e.getMessage() + "\n");
        }
    }

    private void stopServer() {
        try {
            isRunning = false;
            for (ClientHandler c : clients) c.closeConnection();
            if (serverSocket != null) serverSocket.close();
            chatArea.append("Server stopped.\n");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            clientListModel.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Broadcast a message to all connected clients
    private synchronized void broadcastMessage(String message) {
        chatArea.append(message + "\n");
        synchronized (clients) {
            for (ClientHandler c : clients) {
                try {
                    c.dos.writeUTF(message);
                } catch (IOException ignored) {
                }
            }
        }
    }

    // --- Client handler thread ---
    class ClientHandler extends Thread {
        Socket socket;
        String name;
        DataInputStream dis;
        DataOutputStream dos;

        public ClientHandler(Socket socket, String name, DataInputStream dis, DataOutputStream dos) {
            this.socket = socket;
            this.name = name;
            this.dis = dis;
            this.dos = dos;
        }

        @Override
        public void run() {
            String msg;
            try {
                while ((msg = dis.readUTF()) != null) {
                    broadcastMessage(name + ": " + msg);
                }
            } catch (IOException e) {
                chatArea.append(name + " disconnected.\n");
            } finally {
                closeConnection();
            }
        }

        public void closeConnection() {
            try {
                synchronized (clients) {
                    clients.remove(this);
                }
                clientListModel.removeElement(name);
                broadcastMessage("SERVER: " + name + " left the chat.");
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (socket != null) socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatServer().setVisible(true));
    }
}
