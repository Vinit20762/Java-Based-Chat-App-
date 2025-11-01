import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

/**
 * Chat Client Application
 * Enables real-time chatting with Swing GUI. Runs socket I/O on a separate thread.
 */
public class ChatClient extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket socket;
    private String userName;
    private String serverHost = "localhost"; // Default host
    private int serverPort = 12345; // Default port

    public ChatClient() {
        setTitle("Chat Client");
        setSize(400, 350);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        setVisible(true);

        promptForName();
        connectToServer();

        // Start message receiving on a new thread
        new Thread(this::receiveMessages).start();
    }

    // Ask the user for their chat name
    private void promptForName() {
        userName = JOptionPane.showInputDialog(this, "Enter your name:");
        if (userName == null || userName.trim().isEmpty()) {
            userName = "User" + (int)(Math.random() * 1000);
        }
    }

    // Connect to server with sockets and streams
    private void connectToServer() {
        try {
            socket = new Socket(serverHost, serverPort);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            // First message sent: user's name
            dos.writeUTF(userName);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error connecting to server: " + e.getMessage());
            System.exit(0);
        }
    }

    // Send a message to the server
    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            try {
                dos.writeUTF(msg);
                inputField.setText("");
            } catch (IOException e) {
                chatArea.append("Failed to send message.\n");
            }
        }
    }

    // Receives messages from the server on a separate thread
    private void receiveMessages() {
        try {
            String message;
            while ((message = dis.readUTF()) != null) {
                chatArea.append(message + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        } catch (IOException e) {
            chatArea.append("Disconnected from server.\n");
        } finally {
            try {
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (socket != null) socket.close();
            } catch (IOException ex) {
                // Silently ignore
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}
