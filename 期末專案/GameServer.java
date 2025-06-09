import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {
    private static final int DEFAULT_PORT = 12345; // Default port
    private static final int MAX_PLAYERS = 4; // Max players
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private Map<String, PlayerState> playerStates = Collections.synchronizedMap(new HashMap<>());
    private AtomicInteger playerCounter = new AtomicInteger(1); // For P1, P2, P3, P4 assignment

    public GameServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("遊戲伺服器已啟動，監聽 Port: " + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                try {
                    // Accept new client connections
                    Socket clientSocket = serverSocket.accept();
                    if (clients.size() < MAX_PLAYERS) {
                        System.out.println("新玩家連線: " + clientSocket.getInetAddress().getHostAddress());
                        ClientHandler clientHandler = new ClientHandler(clientSocket);
                        clients.add(clientHandler);
                        new Thread(clientHandler).start(); // Start a new thread for each client
                    } else {
                        System.out.println("伺服器已滿，拒絕連線: " + clientSocket.getInetAddress().getHostAddress());
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    System.err.println("接受客戶端連線時發生錯誤: " + e.getMessage());
                }
            }
        }).start();

        // Game loop for server (broadcasting updates)
        new Thread(() -> {
            while (true) {
                // In a real game, you would update game logic here (e.g., enemy movement, item spawns)
                // and then broadcast the updated state.
                broadcastGameState();
                try {
                    Thread.sleep(1000 / 60); // Roughly 60 FPS server updates
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    // Broadcast the current game state to all connected clients
    private void broadcastGameState() {
        // Create a serializable representation of the game state
        // For simplicity, let's assume we're sending player positions and names
        Map<String, int[]> positions = new HashMap<>();
        playerStates.forEach((name, state) -> positions.put(name, new int[]{state.playerX, state.playerY}));

        // Create a copy of the clients list to avoid ConcurrentModificationException
        List<ClientHandler> currentClients = new ArrayList<>(clients);
        for (ClientHandler client : currentClients) {
            try {
                client.sendGameState(positions); // Send player positions and names
            } catch (IOException e) {
                System.err.println("向客戶端發送遊戲狀態時發生錯誤: " + client.getPlayerName() + " - " + e.getMessage());
                // Handle disconnected clients (remove from list)
                clients.remove(client);
                playerStates.remove(client.getPlayerName());
            }
        }
    }

    // Inner class to handle individual client connections
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String playerName;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                // Assign player name (P1, P2, etc.)
                playerName = "P" + playerCounter.getAndIncrement();
                playerStates.put(playerName, new PlayerState(playerName, 50, 400)); // Initial player position
                out.writeObject(playerName); // Send assigned name to client
            } catch (IOException e) {
                System.err.println("初始化客戶端處理器時發生錯誤: " + e.getMessage());
            }
        }

        public String getPlayerName() {
            return playerName;
        }

        public void sendGameState(Map<String, int[]> positions) throws IOException {
            out.writeObject(positions); // Send the map of player positions
        }

        @Override
        public void run() {
            try {
                while (clientSocket.isConnected()) {
                    // Read client input (e.g., key presses)
                    Object clientInput = in.readObject();
                    if (clientInput instanceof String) {
                        String command = (String) clientInput;
                        // Process commands (e.g., "MOVE_LEFT", "JUMP")
                        PlayerState currentPlayerState = playerStates.get(playerName);
                        if (currentPlayerState != null) {
                            switch (command) {
                                case "MOVE_LEFT":
                                    currentPlayerState.playerX -= 5;
                                    break;
                                case "MOVE_RIGHT":
                                    currentPlayerState.playerX += 5;
                                    break;
                                case "JUMP":
                                    // Implement jump logic on server
                                    break;
                                // ... handle other commands
                            }
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("客戶端 " + playerName + " 連線斷開: " + e.getMessage());
            } finally {
                try {
                    if (!clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                    // Remove client from shared lists
                    clients.remove(this);
                    playerStates.remove(playerName);
                    System.out.println("客戶端 " + playerName + " 已斷開連線。");
                } catch (IOException e) {
                    System.err.println("關閉客戶端連線時發生錯誤: " + e.getMessage());
                }
            }
        }
    }

    // A simple class to hold player state on the server
    private static class PlayerState implements Serializable {
        public String name;
        public int playerX, playerY;
        // Add other player-specific states (e.g., isJumping, hasBigMushroom)

        public PlayerState(String name, int playerX, int playerY) {
            this.name = name;
            this.playerX = playerX;
            this.playerY = playerY;
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]); // Allow custom port via launch argument
            } catch (NumberFormatException e) {
                System.err.println("無效的 Port 號碼，使用預設 Port: " + DEFAULT_PORT);
            }
        }
        GameServer server = new GameServer(port); // Line 166
        server.start();
    }
}
