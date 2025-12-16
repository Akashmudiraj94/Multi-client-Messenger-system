import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    // List to keep track of all connected client threads
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static final int PORT = 12345;

    public static void main(String[] args) {
        System.out.println("Starting Chat Server on port " + PORT + "...");
        
        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                // Wait for a new client connection
                Socket clientSocket = listener.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                
                // Create a new thread to handle the client
                ClientHandler clientThread = new ClientHandler(clientSocket);
                clientHandlers.add(clientThread);
                new Thread(clientThread).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // Method to broadcast a message to all connected clients
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler handler : clientHandlers) {
            // Send to everyone except the sender
            if (handler != sender) {
                handler.sendMessage(message);
            }
        }
    }
    
    // Method to remove a disconnected client from the set
    public static void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
        System.out.println("Client disconnected.");
    }

    // Inner class to handle communication with a single client
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String userName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                // Get input and output streams
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Prompt and read the client's username
                out.println("Enter your username:");
                userName = in.readLine();
                
                if (userName == null) {
                    return; // Client disconnected before entering username
                }

                System.out.println("Client '" + userName + "' established.");
                broadcast(">> " + userName + " has joined the chat.", this);
                
                String clientMessage;
                // Main loop: read messages from the client and broadcast them
                while ((clientMessage = in.readLine()) != null) {
                    String fullMessage = "[" + userName + "]: " + clientMessage;
                    System.out.println(fullMessage);
                    broadcast(fullMessage, this);
                }

            } catch (IOException e) {
                // Client disconnected unexpectedly
                System.err.println("Connection lost for client '" + userName + "'.");
            } finally {
                // Cleanup when the client disconnects or an error occurs
                if (userName != null) {
                    broadcast("<< " + userName + " has left the chat.", this);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    /* ignore */
                }
                removeClient(this);
            }
        }

        // Method for the server to send a message to this specific client
        public void sendMessage(String message) {
            out.println(message);
        }
    }
}