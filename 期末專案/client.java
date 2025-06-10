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
    // 主角屬性 (這些變數現在只用於本地繪圖，其值會從伺服器同步)
    private int playerWidth = 50, playerHeight = 50; 
    private Image playerImage, bigplayerImage; 
    private Image fireballImage; 
    private Image playerjump, bigplayerjump; 
    private ImageIcon playerGif, bigplayerGif; 

    // 背景屬性
    private Image[] backgrounds = new Image[4];
    private Image gameover;
    private int currentBackgroundIndex = 0;
    private int screenWidth = 800;
    private int screenHeight = 600;
    private int groundHeight = 120; 

    // 遊戲狀態 (從伺服器接收並更新)
    private Map<String, int[]> playerPositions = Collections.synchronizedMap(new HashMap<>());
    private List<Map<String, Object>> currentBlocks = Collections.synchronizedList(new ArrayList<>());
    private List<Map<String, Object>> currentMushrooms = Collections.synchronizedList(new ArrayList<>());
    private List<Map<String, Object>> currentGoombas = Collections.synchronizedList(new ArrayList<>());
    private int groundLevel;

    private List<Map<String, Object>> currentFireballs = Collections.synchronizedList(new ArrayList<>());

    // 網路相關
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String myPlayerName; 

    // 按鍵狀態
    private Set<Integer> pressedKeys = Collections.synchronizedSet(new HashSet<>());
    private boolean fireballRequested = false; 

    public client(String serverAddress, int serverPort) {
        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.CYAN);

        try {
            // 所有圖片路徑都加上 "images/" 前綴
            playerImage = new ImageIcon("images/mario1.png").getImage();
            bigplayerImage = new ImageIcon("images/bigmario1.jpg").getImage();
            playerjump = new ImageIcon("images/mariojump.png").getImage();
            bigplayerjump = new ImageIcon("images/bigmariojump.png").getImage();
            gameover = new ImageIcon("images/gameover.png").getImage();
            fireballImage = new ImageIcon("images/Player_fireball.png").getImage(); 

            backgrounds[0] = new ImageIcon("images/background1.png").getImage();
            backgrounds[1] = new ImageIcon("images/background2.png").getImage();
            backgrounds[2] = new ImageIcon("images/background3.png").getImage();
            backgrounds[3] = new ImageIcon("images/background4.png").getImage();

        } catch (Exception e) {
            System.err.println("載入圖片失敗: " + e.getMessage());
        }

        try {
            socket = new Socket(serverAddress, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            myPlayerName = (String) in.readObject();
            System.out.println("連線成功，您的玩家名稱是: " + myPlayerName);
            System.out.println("客戶端串流初始化成功！"); // Debug
            System.out.println("您是: " + myPlayerName + " - 接收到伺服器分配的名字"); // Debug

            new Thread(new ServerReader()).start();
            System.out.println("ServerReader 線程已啟動。"); // Debug
            System.out.println("ServerReader 執行中，等待伺服器數據..."); // Debug

            new Thread(this).start();
            System.out.println("按鍵發送線程已啟動。"); // Debug

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("連線伺服器失敗: " + e.getMessage());
            System.exit(1); 
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgrounds[currentBackgroundIndex] != null) {
            g.drawImage(backgrounds[currentBackgroundIndex], 0, 0, screenWidth, screenHeight, this);
        }

        g.setColor(new Color(139, 69, 19)); 
        g.fillRect(0, groundLevel, screenWidth, screenHeight - groundLevel);

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

        synchronized (currentFireballs) {
            for (Map<String, Object> fireball : currentFireballs) {
                int fbX = (int) fireball.get("x");
                int fbY = (int) fireball.get("y");
                int fbWidth = (int) fireball.get("width");
                int fbHeight = (int) fireball.get("height");
                boolean fbAlive = (boolean) fireball.get("isAlive");

                if (fbAlive && fireballImage != null) {
                    g.drawImage(fireballImage, fbX, fbY, fbWidth, fbHeight, this);
                }
            }
        }

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
            }
            catch (IOException | InterruptedException e) {
                System.err.println("發送按鍵指令時發生錯誤或線程被中斷: " + e.getMessage());
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
            
            // 注意這裡：fireballRequested 只有在發送後才設為 false
            keyStates.put("FIREBALL_REQUESTED", fireballRequested); 
            //System.out.println("客戶端：準備發送 keyStates: " + keyStates); // Debug
        }
        
        // 確保在發送後才重置 fireballRequested
        out.writeObject(keyStates);
        out.flush();
        
        // 成功發送後重置 fireballRequested，確保每次按 E 只發送一次
        if (fireballRequested) { // 只有當它為 true 時才重置
             fireballRequested = false; 
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (pressedKeys) {
            pressedKeys.add(e.getKeyCode());
            if (e.getKeyCode() == KeyEvent.VK_E) { 
                // 只有當前為 false 時才設為 true，避免重複發送請求
                if (!fireballRequested) {
                    fireballRequested = true; 
                    System.out.println("客戶端：按下 E 鍵，fireballRequested 設為 true。"); // Debug
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
                System.err.println("從伺服器讀取數據時發生錯誤: " + e.getMessage());
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException ex) {
                    System.err.println("關閉客戶端連線資源時發生錯誤: " + ex.getMessage());
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
                System.err.println("無效的 Port 號碼，使用預設 Port: " + 12345);
            }
        }

        JFrame frame = new JFrame("馬力歐遊戲雛型 (客戶端)");
        client game = new client(serverAddress, serverPort); 
        frame.add(game);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setResizable(false);
    }
}