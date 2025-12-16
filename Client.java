import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    // GUI Components
    private JFrame frame = new JFrame("Java Swing Chat Client");
    private JTextArea messageArea = new JTextArea(20, 50);
    private JTextField messageField = new JTextField(40);
    private PrintWriter out;
    private String userName;

    public Client() {
        // --- 1. Set up the GUI ---
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        
        // Panel for input field and button
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());
        inputPanel.add(messageField);
        
        JButton sendButton = new JButton("Send");
        inputPanel.add(sendButton);

        // Frame setup
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(inputPanel, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // --- 2. Add Listeners ---
        // Listener for the Send button and Enter key press in the text field
        ActionListener sendListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        };
        sendButton.addActionListener(sendListener);
        messageField.addActionListener(sendListener);
        
        frame.setVisible(true);

        // --- 3. Connect to Server ---
        connectToServer();
    }
    
    // Method to handle sending a message
    private void sendMessage() {
        String message = messageField.getText();
        if (message != null && !message.trim().isEmpty() && out != null) {
            out.println(message); // Send to server
            // Display your own message in the chat window immediately
            messageArea.append("[You]: " + message + "\n"); 
            messageField.setText(""); // Clear the input field
        }
    }

    // Method to handle the entire connection process
    private void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Prompt for the username (using a Swing dialog)
            userName = JOptionPane.showInputDialog(
                frame,
                "Choose a screen name:",
                "Screen Name Selection",
                JOptionPane.PLAIN_MESSAGE
            );
            
            if (userName == null || userName.trim().isEmpty()) {
                System.exit(0); // Exit if user cancels or enters no name
            }
            frame.setTitle("Java Swing Chat Client - " + userName);

            // Send username to the server
            out.println(userName);

            // Start the separate thread to listen for server messages
            new Thread(new ServerListener(in)).start();

        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(frame, "Server not found: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "I/O Error: Server may not be running. " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Inner class to continuously read messages from the server
    private class ServerListener implements Runnable {
        private BufferedReader in;

        public ServerListener(BufferedReader in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                String serverResponse;
                while ((serverResponse = in.readLine()) != null) {
                    // Update the GUI (message area) with the received message
                    messageArea.append(serverResponse + "\n");
                }
            } catch (IOException e) {
                // Connection closed or lost
                messageArea.append("\n*** Connection to the server lost. ***\n");
            }
        }
    }

    public static void main(String[] args) {
        // Use the event dispatch thread for Swing applications (best practice)
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Client();
            }
        });
    }
}