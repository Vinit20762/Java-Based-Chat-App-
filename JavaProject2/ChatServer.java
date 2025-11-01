import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class ChatServer extends JFrame {
    private JTextArea messageArea;
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;
    private ServerSocket serverSocket;
    private java.util.List<ClientHandler> clientHandlers = Collections.synchronizedList(new ArrayList<>());
    private int port = 12345; // Default port

    public ChatServer() {
        setTitle("Chat Server");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        clientScrollPane.setPreferredSize(new Dimension(120, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientScrollPane, scrollPane);
        splitPane.setDividerLocation(120);
        add(splitPane);

        setVisible(true);

        startServer();
    }

    // Start the server and accept clients in separate thread
    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            appendMessage("Server started on port " + port);

            // Accept clients in a separate thread
            new Thread(() -> {
                try {
                    while (!serverSocket.isClosed()) {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket, this);
                        clientHandlers.add(handler);
                        handler.start();
                    }
                } catch (IOException e) {
                    appendMessage("Server socket closed or error occurred.");
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error starting server: " + e.getMessage());
        }
    }

    // Broadcast messages to all clients
    public void broadcast(String message) {
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers) {
                handler.sendMessage(message);
            }
        }
        appendMessage(message);
    }

    // Add a client name to GUI list
    public void addClient(String clientName) {
        clientListModel.addElement(clientName);
    }

    // Remove a client name from GUI list
    public void removeClient(String clientName) {
        clientListModel.removeElement(clientName);
    }

    // Show a message in the server GUI
    public void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(msg + "\n");
        });
    }

    // Remove client handler (thread) when disconnected
    public void removeClientHandler(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatServer::new);
    }
}


//   Handles each connected client communication in a separate thread.
 
class ClientHandler extends Thread {
    private Socket socket;
    private ChatServer server;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String clientName;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            server.appendMessage("Error initializing client handler streams.");
        }
    }

    // Send a message to this client
    public void sendMessage(String message) {
        try {
            dos.writeUTF(message);
        } catch (IOException e) {
            // Client might be disconnected, ignore silently
        }
    }

    public void run() {
        try {
            // First message from client is their name
            clientName = dis.readUTF();
            server.addClient(clientName);
            server.broadcast(clientName + " joined the chat.");

            // Relay received messages to all clients
            String message;
            while ((message = dis.readUTF()) != null) {
                server.broadcast(clientName + ": " + message);
            }
        } catch (IOException e) {
            server.appendMessage(clientName + " disconnected.");
        } finally {
            try {
                // Clean up
                server.removeClient(clientName);
                server.broadcast(clientName + " left the chat.");
                server.removeClientHandler(this);
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (socket != null) socket.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
}
