import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class client extends JPanel implements KeyListener, Runnable {
    // Player attributes (used for local rendering, synced from server)
    private int playerWidth = 50, playerHeight = 50;
    private Image playerImage, bigplayerImage;
    private Image fireballImage;
    private Image playerjump, bigplayerjump;
    private ImageIcon playerGif, bigplayerGif;

    // Background attributes
    private Image[] backgrounds = new Image[4];
    private Image gameover;
    private int currentBackgroundIndex = 0;
    private int screenWidth = 800;
    private int screenHeight = 600;
    private int groundHeight = 120;
    private int backgroundOffset = 0; // Offset for scrolling background
    private int lastPlayerX = -1;     // Last known X position of the player

    // Game state (received and updated from server)
    private Map<String, int[]> playerPositions = Collections.synchronizedMap(new HashMap<>());
    private List<Map<String, Object>> currentBlocks = Collections.synchronizedList(new ArrayList<>());
    private List<Map<String, Object>> currentMushrooms = Collections.synchronizedList(new ArrayList<>());
    private List<Map<String, Object>> currentGoombas = Collections.synchronizedList(new ArrayList<>());
    private int groundLevel;

    private List<Map<String, Object>> currentFireballs = Collections.synchronizedList(new ArrayList<>());

    // Network-related
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String myPlayerName;

    // Key states
    private Set<Integer> pressedKeys = Collections.synchronizedSet(new HashSet<>());
    private boolean fireballRequested = false;

    public client(String serverAddress, int serverPort) {
        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.CYAN);

        try {
            // Load all images with "images/" prefix
            playerImage = new ImageIcon("images/mario.png").getImage();
            bigplayerImage = new ImageIcon("images/bigmario.png").getImage();
            playerjump = new ImageIcon("images/mario_jump.png").getImage();
            bigplayerjump = new ImageIcon("images/bigmario_jump.png").getImage();
            gameover = new ImageIcon("images/gameover.png").getImage();

            try {
                fireballImage = new ImageIcon(getClass().getResource("/images/Player_fireball.png")).getImage();
                if (fireballImage == null) {
                    System.err.println("Client: Failed to load fireball image using getClass().getResource(\"/images/Player_fireball.png\")!");
                } else {
                    System.out.println("Client: Successfully loaded fireball image using getClass().getResource(\"/images/Player_fireball.png\").");
                }
            } catch (Exception e) {
                System.err.println("Client: Severe error loading fireball image: " + e.getMessage());
                e.printStackTrace();
            }
            if (fireballImage == null) {
                System.err.println("Client: fireball.png image loading failed! (fireballImage is null)");
            } else {
                System.out.println("Client: fireball.png image loaded successfully.");
            }

            backgrounds[0] = new ImageIcon("images/background1.png").getImage();
            backgrounds[1] = new ImageIcon("images/background2.png").getImage();
            backgrounds[2] = new ImageIcon("images/background3.png").getImage();
            backgrounds[3] = new ImageIcon("images/background4.png").getImage();

        } catch (Exception e) {
            System.err.println("Failed to load images: " + e.getMessage());
        }

        try {
            socket = new Socket(serverAddress, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            myPlayerName = (String) in.readObject();
            System.out.println("Connected successfully, your player name is: " + myPlayerName);
            System.out.println("Client streams initialized successfully!");
            System.out.println("You are: " + myPlayerName + " - Received server-assigned name");

            new Thread(new ServerReader()).start();
            System.out.println("ServerReader thread started.");
            System.out.println("ServerReader running, awaiting server data...");

            new Thread(this).start();
            System.out.println("Key sending thread started.");

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw scrolling background
        if (backgrounds.length > 0 && backgrounds[0] != null) {
            int bgWidth = screenWidth; // Assume background width equals screen width
            int totalBgWidth = bgWidth * backgrounds.length;

            // Normalize offset to stay within total background width
            int offset = backgroundOffset % totalBgWidth;
            if (offset < 0) offset += totalBgWidth;

            int firstBgIndex = (offset / bgWidth) % backgrounds.length;
            int secondBgIndex = (firstBgIndex + 1) % backgrounds.length;

            int x1 = -(offset % bgWidth);
            int x2 = x1 + bgWidth;

            // Draw two backgrounds to ensure seamless scrolling
            g.drawImage(backgrounds[firstBgIndex], x1, 0, screenWidth, screenHeight, this);
            g.drawImage(backgrounds[secondBgIndex], x2, 0, screenWidth, screenHeight, this);

            // Draw a third background if needed to cover the screen fully
            if (x2 < screenWidth) {
                int thirdBgIndex = (secondBgIndex + 1) % backgrounds.length;
                int x3 = x2 + bgWidth;
                g.drawImage(backgrounds[thirdBgIndex], x3, 0, screenWidth, screenHeight, this);
            }
        }

        // Draw ground
        g.setColor(new Color(139, 69, 19));
        g.fillRect(0, groundLevel, screenWidth, screenHeight - groundLevel);

        // Draw blocks
        synchronized (currentBlocks) {
            for (Map<String, Object> block : currentBlocks) {
                int blockX = (int) block.get("x");
                int blockY = (int) block.get("y");
                int blockWidth = (int) block.get("width");
                int blockHeight = (int) block.get("height");
                String type = (String) block.get("type");

                if ("item".equals(type) && (boolean) block.get("isHit")) {
                    g.setColor(Color.LIGHT_GRAY);
                } else if ("item".equals(type)) {
                    g.setColor(Color.ORANGE);
                } else {
                    g.setColor(Color.DARK_GRAY);
                }
                g.fillRect(blockX, blockY, blockWidth, blockHeight);
            }
        }

        // Draw mushrooms
        synchronized (currentMushrooms) {
            for (Map<String, Object> mushroom : currentMushrooms) {
                if ((boolean) mushroom.get("isVisible")) {
                    g.setColor(Color.RED);
                    int mushX = (int) mushroom.get("x");
                    int mushY = (int) mushroom.get("y");
                    int mushW = (int) mushroom.get("width");
                    int mushH = (int) mushroom.get("height");
                    g.fillOval(mushX, mushY, mushW, mushH);
                }
            }
        }

        // Draw goombas
        synchronized (currentGoombas) {
            for (Map<String, Object> goomba : currentGoombas) {
                if ((boolean) goomba.get("isAlive")) {
                    g.setColor(Color.decode("#94471B"));
                    int gX = (int) goomba.get("x");
                    int gY = (int) goomba.get("y");
                    int gW = (int) goomba.get("width");
                    int gH = (int) goomba.get("height");
                    g.fillOval(gX, gY, gW, gH);
                }
            }
        }

        // Draw fireballs
        System.out.println("Client: Current fireball count: " + currentFireballs.size());
        if (fireballImage != null) {
            synchronized (currentFireballs) {
                System.out.println("Client drawing: Attempting to draw fireballs, count: " + currentFireballs.size());
                for (Map<String, Object> fireball : currentFireballs) {
                    Boolean fbAlive = (Boolean) fireball.getOrDefault("isAlive", false);
                    if (fbAlive) {
                        int fbX = (int) fireball.getOrDefault("x", 0);
                        int fbY = (int) fireball.getOrDefault("y", 0);
                        int fbWidth = (int) fireball.getOrDefault("width", 20);
                        int fbHeight = (int) fireball.getOrDefault("height", 20);

                        g.drawImage(fireballImage, fbX, fbY, fbWidth, fbHeight, this);
                        System.out.println("Client drawing: Drawing fireball at X:" + fbX + ", Y:" + fbY + ", Width:" + fbWidth + ", Height:" + fbHeight);
                    }
                }
            }
        } else {
            System.err.println("Client drawing: fireballImage is null, cannot draw fireballs.");
        }

        // Draw players
        synchronized (playerPositions) {
            playerPositions.forEach((name, pos) -> {
                int pX = pos[0];
                int pY = pos[1];
                int pW = pos[2];
                int pH = pos[3];
                boolean isBigMario = (pos[4] == 1);
                boolean isGameOver = (pos[5] == 1);

                if (isGameOver) {
                    if (name.equals(myPlayerName) && gameover != null) {
                        g.drawImage(gameover, screenWidth / 2 - gameover.getWidth(this) / 2,
                                    screenHeight / 2 - gameover.getHeight(this) / 2, this);
                    }
                    return;
                }

                Image currentImage;
                if (pY + pH < groundLevel) {
                    currentImage = isBigMario ? bigplayerjump : playerjump;
                } else {
                    currentImage = isBigMario ? bigplayerImage : playerImage;
                }

                if (currentImage != null) {
                    g.drawImage(currentImage, pX, pY, pW, pH, this);
                }

                g.setColor(Color.BLACK);
                g.drawString(name, pX + pW / 2 - g.getFontMetrics().stringWidth(name) / 2, pY - 5);
            });
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                sendPlayerInput();
                Thread.sleep(1000 / 60);
            } catch (IOException | InterruptedException e) {
                System.err.println("Error sending key commands or thread interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void sendPlayerInput() throws IOException {
        Map<String, Boolean> keyStates = new HashMap<>();
        synchronized (pressedKeys) {
            keyStates.put("MOVE_LEFT", pressedKeys.contains(KeyEvent.VK_A));
            keyStates.put("MOVE_RIGHT", pressedKeys.contains(KeyEvent.VK_D));
            keyStates.put("JUMP", pressedKeys.contains(KeyEvent.VK_SPACE));
            keyStates.put("FIREBALL_REQUESTED", fireballRequested);
        }

        out.writeObject(keyStates);
        out.flush();

        if (fireballRequested) {
            fireballRequested = false;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (pressedKeys) {
            pressedKeys.add(e.getKeyCode());
            if (e.getKeyCode() == KeyEvent.VK_E) {
                if (!fireballRequested) {
                    fireballRequested = true;
                    System.out.println("Client: E key pressed, fireballRequested set to true.");
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        synchronized (pressedKeys) {
            pressedKeys.remove(e.getKeyCode());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private class ServerReader implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof Map) {
                        Map<String, Object> gameState = (Map<String, Object>) obj;

                        if (gameState.containsKey("players")) {
                            synchronized (playerPositions) {
                                playerPositions.clear();
                                playerPositions.putAll((Map<String, int[]>) gameState.get("players"));

                                // Update background offset based on player's movement
                                int[] myPos = playerPositions.get(myPlayerName);
                                if (myPos != null) {
                                    int currentPlayerX = myPos[0];
                                    if (lastPlayerX != -1) {
                                        int deltaX = currentPlayerX - lastPlayerX;
                                        backgroundOffset -= deltaX; // Scroll background opposite to player movement
                                    }
                                    lastPlayerX = currentPlayerX;
                                }
                            }
                        }

                        if (gameState.containsKey("blocks")) {
                            synchronized (currentBlocks) {
                                currentBlocks.clear();
                                currentBlocks.addAll((List<Map<String, Object>>) gameState.get("blocks"));
                            }
                        }

                        if (gameState.containsKey("mushrooms")) {
                            synchronized (currentMushrooms) {
                                currentMushrooms.clear();
                                currentMushrooms.addAll((List<Map<String, Object>>) gameState.get("mushrooms"));
                            }
                        }

                        if (gameState.containsKey("goombas")) {
                            synchronized (currentGoombas) {
                                currentGoombas.clear();
                                currentGoombas.addAll((List<Map<String, Object>>) gameState.get("goombas"));
                            }
                        }

                        if (gameState.containsKey("fireballs")) {
                            synchronized (currentFireballs) {
                                currentFireballs.clear();
                                currentFireballs.addAll((List<Map<String, Object>>) gameState.get("fireballs"));
                            }
                        }

                        if (gameState.containsKey("groundLevel")) {
                            groundLevel = (int) gameState.get("groundLevel");
                        }

                        repaint();
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error reading data from server: " + e.getMessage());
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException ex) {
                    System.err.println("Error closing client connection resources: " + ex.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 12345;

        if (args.length > 0) {
            serverAddress = args[0];
        }
        if (args.length > 1) {
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default port: " + 12345);
            }
        }

        JFrame frame = new JFrame("Mario Game Prototype (Client)");
        client game = new client(serverAddress, serverPort);
        frame.add(game);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setResizable(false);
    }
}