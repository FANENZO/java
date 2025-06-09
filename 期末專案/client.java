import java.io.*;
import java.net.*;
import java.util.Map;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class client extends JPanel implements KeyListener, Runnable {
    // ... (existing game variables, many will become "received" from server)

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String assignedPlayerName; // To store P1, P2, etc.
    private Map<String, int[]> allPlayerPositions; // To store positions of all players

    // --- Client setup ---
    public client(String serverAddress, int serverPort) {
        // ... (existing constructor code)

        try {
            socket = new Socket(serverAddress, 12345); // Connect to server
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Receive assigned player name from server
            assignedPlayerName = (String) in.readObject(); // Receive P1, P2 etc.
            System.out.println("您是: " + assignedPlayerName);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("連接伺服器失敗: " + e.getMessage());
            System.exit(1);
        }

        new Thread(this).start(); // Start game loop (for rendering and sending input)
        new Thread(new ServerReader()).start(); // Start thread to read from server
    }

    // --- ServerReader Thread ---
    private class ServerReader implements Runnable {
        @Override
        public void run() {
            try {
                while (socket.isConnected()) {
                    Object serverData = in.readObject();
                    if (serverData instanceof Map) {
                        allPlayerPositions = (Map<String, int[]>) serverData; // Update all player positions
                        repaint(); // Request repaint to show updated positions
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("從伺服器讀取數據時發生錯誤: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("關閉客戶端 Socket 時發生錯誤: " + e.getMessage());
                }
            }
        }
    }

    // --- KeyListener Modifications ---
    @Override
    public void keyPressed(KeyEvent e) {
        try {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                out.writeObject("MOVE_LEFT"); // Send command to server
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                out.writeObject("MOVE_RIGHT"); // Send command to server
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                out.writeObject("JUMP"); // Send command to server
            }
        } catch (IOException ex) {
            System.err.println("發送指令到伺服器時發生錯誤: " + ex.getMessage());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // For continuous movement, you might send "STOP_LEFT" etc.
        // Or the server could just track key states.
    }

    // --- Painting Logic ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw background (still client-side)
        g.drawImage(backgrounds[currentBackgroundIndex], 0, 0, screenWidth, screenHeight, this);

        // Draw all players based on `allPlayerPositions` received from server
        if (allPlayerPositions != null) {
            for (Map.Entry<String, int[]> entry : allPlayerPositions.entrySet()) {
                String playerName = entry.getKey();
                int[] pos = entry.getValue();
                int pX = pos[0];
                int pY = pos[1];

                // Draw player image (all players use the same image, but can be dynamic)
                g.drawImage(playerImage, pX, pY, playerWidth, playerHeight, this); // Use a default image

                // Draw player name above the character
                g.setColor(Color.WHITE);
                g.drawString(playerName, pX + playerWidth / 2 - g.getFontMetrics().stringWidth(playerName) / 2, pY - 10);
            }
        }

        // ... (remove drawing of blocks, goombas, mushrooms, etc. as they are server-controlled)
        // The server would send their positions as well, and you'd draw them similarly.
    }

    // --- Main method for client ---
    public static void main(String[] args) {
        String serverAddress = "localhost"; // Default to localhost
        int serverPort = 12345; // Default port

        if (args.length > 0) {
            serverAddress = args[0]; // Allow custom server address
        }
        if (args.length > 1) {
            try {
                serverPort = Integer.parseInt(args[1]); // Allow custom port
            } catch (NumberFormatException e) {
                System.err.println("無效的 Port 號碼，使用預設 Port: " + 12345);
            }
        }

        JFrame frame = new JFrame("馬力歐遊戲雛型 (客戶端)");
        client game = new client(serverAddress, serverPort);
        frame.add(game);
        frame.setSize(game.screenWidth, game.screenHeight);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        game.startGameLoop(); // Start client-side rendering loop
    }

    // The run method (game loop) on the client will primarily focus on repainting
    // and potentially sending input based on continuous movement if implemented.
    @Override
    public void run() {
        // Client's game loop (mostly for rendering updates received from server)
        while (true) {
            // The actual game state update comes from ServerReader thread.
            // This loop ensures continuous rendering.
            repaint();
            try {
                Thread.sleep(1000 / 60); // Client rendering at 60 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // A placeholder for starting the game loop, actual game state is server driven.
    public void startGameLoop() {
        // This can be empty or just handle client-side animations if any.
        // The main logic is handled by the `ServerReader` and `keyPressed` methods.
    }
}