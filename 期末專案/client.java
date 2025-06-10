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
    private int playerWidth = 50, playerHeight = 50; // 客戶端本地定義，伺服器會傳遞實際尺寸
    private Image playerImage, bigplayerImage; // 角色圖片
    private Image fireballImage; //火球圖片
    private Image playerjump, bigplayerjump; // 跳躍圖片
    private ImageIcon playerGif, bigplayerGif; // 如果你有 GIF 動畫，這些可以用於動畫播放

    // 背景屬性
    private Image[] backgrounds = new Image[4];
    private Image gameover;
    private int currentBackgroundIndex = 0;
    private int screenWidth = 800;
    private int screenHeight = 600;
    private int groundHeight = 120; // 這個值現在主要參考用，實際 groundLevel 從伺服器來

    // 遊戲狀態變數 (這些將從伺服器接收和更新)
    private int groundLevel; // 從伺服器獲取
    private List<Map<String, Object>> receivedBlocks = new ArrayList<>();
    private List<Map<String, Object>> receivedMushrooms = new ArrayList<>();
    private List<Map<String, Object>> receivedGoombas = new ArrayList<>();
    private List<Map<String, Object>> receivedFireballs = new ArrayList<>();
    private Map<String, int[]> allPlayerPositions; // 儲存所有玩家位置及狀態
    private boolean gameOver = false; // 客戶端本地的遊戲結束狀態

    // 網路相關變數
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String assignedPlayerName; // 儲存 P1, P2 等

    // 蘑菇
    private Image mushroomImage;
    // Goomba
    private Image goombaImage;
    // 磚塊
    private Image normalblockImage; // 磚塊圖片
    private Image itemblockImage; // 物品磚塊圖片

    // 按鍵狀態管理，使用 synchronizedSet 確保線程安全
    private Set<Integer> pressedKeys = Collections.synchronizedSet(new HashSet<>());
    private final int KEY_SEND_INTERVAL = 50; // 毫秒，每 50 毫秒發送一次按鍵狀態

    public client(String serverAddress, int serverPort) {
        setFocusable(true);
        addKeyListener(this);
        setDoubleBuffered(true); // 啟用雙緩衝，減少閃爍

        loadImages(); // 加載遊戲圖片

        try {
            System.out.println("客戶端嘗試連接伺服器: " + serverAddress + ":" + serverPort); // DEBUG
            socket = new Socket(serverAddress, serverPort);
            System.out.println("客戶端連接成功！"); // DEBUG

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("客戶端串流初始化成功！"); // DEBUG

            // 從伺服器接收分配的玩家名稱
            assignedPlayerName = (String) in.readObject();
            System.out.println("您是: " + assignedPlayerName + " - 接收到伺服器分配的名字"); // DEBUG

            new Thread(new ServerReader()).start(); // 啟動線程從伺服器讀取數據
            System.out.println("ServerReader 線程已啟動。"); // DEBUG

            new Thread(this).start(); // 啟動按鍵狀態發送線程
            System.out.println("按鍵發送線程已啟動。"); // DEBUG

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("客戶端連接或初始化失敗: " + e.getMessage()); // DEBUG
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void loadImages() {
        // 載入主角圖片
        playerImage = new ImageIcon("images\\mario1.png").getImage();
        bigplayerImage = new ImageIcon("images\\bigmario1-removebg-preview.png").getImage();
        playerjump = new ImageIcon("images\\mariojmup.png").getImage();
        bigplayerjump = new ImageIcon("images\\bigmariojump-removebg-preview.png").getImage();
        playerGif = new ImageIcon("images/output-onlinegiftools.gif");
        bigplayerGif = new ImageIcon("images\\bigmariorun.gif");
        fireballImage = new ImageIcon("images\\Player_fireball.png").getImage();

        // 載入背景圖片 (假設有 background1.png, background2.png 等)
        for (int i = 0; i < backgrounds.length; i++) {
            backgrounds[i] = new ImageIcon("background" + (i + 1) + ".png").getImage();
        }
        gameover = new ImageIcon("gameover.png").getImage();
        // 載入蘑菇圖片
        mushroomImage = new ImageIcon("images/mushroom.png").getImage();
        // 載入 Goomba 圖片
        goombaImage = new ImageIcon("images/output-goomba.gif").getImage();
        // 載入磚塊圖片
        normalblockImage = new ImageIcon("images\\Brick.png").getImage();
        itemblockImage = new ImageIcon("images\\Bricks1.png").getImage();
    }

    // 負責從伺服器讀取數據的線程
    private class ServerReader implements Runnable {
        @Override
        public void run() {
            System.out.println("ServerReader 執行中，等待伺服器數據..."); // DEBUG
            try {
                while (socket.isConnected()) {
                    Object serverData = in.readObject();
                    //System.out.println("ServerReader 接收到數據: " + serverData.getClass().getName()); // DEBUG
                    if (serverData instanceof Map) {
                        Map<String, Object> fullGameState = (Map<String, Object>) serverData;
                        //System.out.println("ServerReader 數據為 Map，包含鍵: " + fullGameState.keySet()); // DEBUG

                        allPlayerPositions = (Map<String, int[]>) fullGameState.get("players");
                        receivedBlocks = (List<Map<String, Object>>) fullGameState.get("blocks");
                        receivedMushrooms = (List<Map<String, Object>>) fullGameState.get("mushrooms");
                        receivedGoombas = (List<Map<String, Object>>) fullGameState.get("goombas");
                        receivedFireballs = (List<Map<String, Object>>) fullGameState.get("fireballs");
                        groundLevel = (int) fullGameState.get("groundLevel");
                        //System.out.println("更新遊戲狀態。玩家數量: " + (allPlayerPositions != null ? allPlayerPositions.size() : 0)); // DEBUG

                        if (allPlayerPositions != null && allPlayerPositions.containsKey(assignedPlayerName)) {
                            int[] currentPlayerStateArray = allPlayerPositions.get(assignedPlayerName);
                            if (currentPlayerStateArray.length > 5) {
                                gameOver = (currentPlayerStateArray[5] == 1);
                            }
                        }
                        repaint(); // 收到新狀態後請求重繪畫面
                    } else {
                        System.out.println("ServerReader 接收到非 Map 數據: " + serverData); // DEBUG
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("ServerReader 讀取數據錯誤: " + e.getMessage()); // DEBUG
                e.printStackTrace();
            } finally {
                // 確保連接關閉
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("關閉客戶端 Socket 時發生錯誤: " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 繪製背景
        g.drawImage(backgrounds[currentBackgroundIndex], 0, 0, screenWidth, screenHeight, this);

        // 繪製地面 (使用伺服器傳遞的 groundLevel)
        g.setColor(new Color(139, 69, 19)); // 棕色
        if (groundLevel > 0) { // 確保 groundLevel 已從伺服器獲取
            g.fillRect(0, groundLevel, screenWidth, screenHeight - groundLevel);
        } else {
            // 如果還沒收到 groundLevel，先用預設值或不繪製
            g.fillRect(0, screenHeight - groundHeight, screenWidth, groundHeight);
            System.out.println("PaintComponent: groundLevel 未設定，使用預設地面。"); // DEBUG
        }
        
        // 繪製火球
        if (receivedFireballs != null) {
            for (Map<String, Object> fireball : receivedFireballs) {
                int fx = (int) fireball.get("x");
                int fy = (int) fireball.get("y");
                int fwidth = (int) fireball.get("width");
                int fheight = (int) fireball.get("height");
                g.drawImage(fireballImage, fx, fy, fwidth, fheight, this);
            }
        }

        // 只有當從伺服器接收到遊戲狀態時才進行繪製
        if (allPlayerPositions != null) {
            // 繪製所有玩家
            for (Map.Entry<String, int[]> entry : allPlayerPositions.entrySet()) {
                String pName = entry.getKey();
                int[] playerStateData = entry.getValue();

                int currentP_X = playerStateData[0];
                int currentP_Y = playerStateData[1];
                int currentP_Width = playerStateData[2];
                int currentP_Height = playerStateData[3];
                boolean isBigMario = (playerStateData[4] == 1);
                boolean isPlayerGameOver = (playerStateData[5] == 1);

                if (isPlayerGameOver) {
                    g.drawImage(gameover, currentP_X, currentP_Y, currentP_Width, currentP_Height, this);
                    g.setColor(Color.RED);
                    g.drawString("GAME OVER", currentP_X, currentP_Y - 10);
                    continue; // 跳過繪製該玩家的正常圖片
                }

                Image playerToDraw;
                if (isBigMario) {
                    playerToDraw = bigplayerGif.getImage();
                } else {
                    playerToDraw = playerGif.getImage();
                }

                g.drawImage(playerToDraw, currentP_X, currentP_Y, currentP_Width, currentP_Height, this);

                // 繪製玩家名稱
                g.setColor(Color.WHITE);
                int nameWidth = g.getFontMetrics().stringWidth(pName);
                g.drawString(pName, currentP_X + (currentP_Width - nameWidth) / 2, currentP_Y - 10);
            }

            // 繪製磚塊
            if (receivedBlocks != null) {
                for (Map<String, Object> block : receivedBlocks) {
                    int bx = (int) block.get("x");
                    int by = (int) block.get("y");
                    int bwidth = (int) block.get("width");
                    int bheight = (int) block.get("height");
                    String type = (String) block.get("type");

                    if (type.equals("item")) {
                        g.drawImage(itemblockImage, bx, by, bwidth, bheight, this);
                    } else {
                        g.drawImage(normalblockImage, bx, by, bwidth, bheight, this);
                    }
                }
            }

            // 繪製蘑菇
            if (receivedMushrooms != null) {
                for (Map<String, Object> mushroom : receivedMushrooms) {
                    boolean isVisible = (boolean) mushroom.get("isVisible");
                    if (isVisible) {
                        int mx = (int) mushroom.get("x");
                        int my = (int) mushroom.get("y");
                        int mwidth = (int) mushroom.get("width");
                        int mheight = (int) mushroom.get("height");
                        g.drawImage(mushroomImage, mx, my, mwidth, mheight, this);
                    }
                }
            }

            // 繪製 Goomba
            if (receivedGoombas != null) {
                for (Map<String, Object> goomba : receivedGoombas) {
                    boolean isAlive = (boolean) goomba.get("isAlive");
                    if (isAlive) {
                        int gx = (int) goomba.get("x");
                        int gy = (int) goomba.get("y");
                        int gwidth = (int) goomba.get("width");
                        int gheight = (int) goomba.get("height");
                        g.drawImage(goombaImage, gx, gy, gwidth, gheight, this);
                    }
                }
            }
            //System.out.println("PaintComponent: 正在繪製遊戲物體。"); // DEBUG
        } else {
            // 如果還沒有收到伺服器狀態，顯示連接訊息
            g.setColor(Color.WHITE);
            g.drawString("等待伺服器資料...", screenWidth / 2 - 60, screenHeight / 2);
            System.out.println("PaintComponent: allPlayerPositions 為空，顯示等待訊息。"); // DEBUG
        }

        // 如果當前客戶端玩家遊戲結束，顯示 Game Over 畫面覆蓋所有
        if (gameOver) {
            g.drawImage(gameover, 0, 0, screenWidth, screenHeight, this);
            System.out.println("PaintComponent: 遊戲結束畫面已繪製。"); // DEBUG
        }
    }

    // 加入一個旗標表示「是否發射火球」
    private volatile boolean fireballRequested = false;

    @Override
    public void run() {
        // 這個 run 方法現在負責定時發送按鍵指令
        while (socket.isConnected()) {
            try {
                Map<String, Boolean> currentKeyStatesToSend = new HashMap<>();
                currentKeyStatesToSend.put("MOVE_LEFT", false);
                currentKeyStatesToSend.put("MOVE_RIGHT", false);
                currentKeyStatesToSend.put("JUMP", false);
                currentKeyStatesToSend.put("FIREBALL", false); // 新增 fireball 欄位

                synchronized (pressedKeys) { // 同步訪問 pressedKeys 集合
                    if (pressedKeys.contains(KeyEvent.VK_LEFT) || pressedKeys.contains(KeyEvent.VK_A)) {
                        currentKeyStatesToSend.put("MOVE_LEFT", true);
                    }
                    if (pressedKeys.contains(KeyEvent.VK_RIGHT) || pressedKeys.contains(KeyEvent.VK_D)) {
                        currentKeyStatesToSend.put("MOVE_RIGHT", true);
                    }
                    if (pressedKeys.contains(KeyEvent.VK_SPACE) || pressedKeys.contains(KeyEvent.VK_W)) {
                        currentKeyStatesToSend.put("JUMP", true);
                    }
                }
                // 如果之前按過 E，發射火球狀態設 true，發送一次後重置
                if (fireballRequested) {
                    currentKeyStatesToSend.put("FIREBALL", true);
                    fireballRequested = false; // 重置
                }

                out.writeObject(currentKeyStatesToSend); // 總是發送地圖
                out.flush();
                // System.out.println("已發送按鍵狀態: " + currentKeyStatesToSend); // DEBUG: 如果輸出太多可以註釋掉

                Thread.sleep(KEY_SEND_INTERVAL);
            } catch (IOException | InterruptedException e) {
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
    
            keyStates.put("FIREBALL_REQUESTED", fireballRequested); 
            System.out.println("客戶端：準備發送 keyStates: " + keyStates); // <<=== 添加此行
            fireballRequested = false; // 發送後立即重置為 false，確保每次按下只發射一次火球
        }
        out.writeObject(keyStates);
        out.flush();
    }

@Override
public void keyPressed(KeyEvent e) {
    synchronized (pressedKeys) {
        pressedKeys.add(e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_E) {
            fireballRequested = true; // 按下 E 觸發發射火球請求
            System.out.println("客戶端：按下 E 鍵，fireballRequested 設為 true。"); // <<=== 添加此行
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
        // 不常用於遊戲控制
    }

    // --- 主方法 ---
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
        frame.setSize(game.screenWidth, game.screenHeight);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}