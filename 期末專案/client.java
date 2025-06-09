import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket; // 確保有這個
import java.util.Map; // 確保有這個
import javax.swing.ImageIcon; // 確保有這個
import javax.swing.JFrame; // 確保有這個
import javax.swing.JPanel; // 確保有這個

public class client extends JPanel implements KeyListener, Runnable {
    // 主角屬性
    private int playerX = 50, playerY; // 主角初始位置
    private int playerWidth = 50, playerHeight = 50;
    private int playerSpeed = 5;
    private Image playerImage, bigplayerImage; // 角色圖片
    private Image playerjump, bigplayerjump;
    private ImageIcon playerGif, bigplayerGif; // 角色 GIF
    private boolean isMoving = false; // 判斷角色是否在移動
    private boolean jump = false;
    private boolean gameOver = false; // 是否顯示Game Over
    private boolean isbigmario = false;

    // 背景屬性
    private Image[] backgrounds = new Image[4];
    private Image gameover;
    private int currentBackgroundIndex = 0; // 當前背景索引
    private int screenWidth = 800; // 視窗寬度  <-- 確保這些變數存在
    private int screenHeight = 600; // 視窗高度 <-- 確保這些變數存在
    private int groundHeight = 120; // 地面高度
    private int groundLevel; // 地面頂部位置
    private int currentImageIndex = 0;

    // 移動邏輯
    private boolean isJumping = false;
    private boolean isFalling = false;
    private int jumpStrength = -15; // 跳躍力量
    private int gravity = 1; // 重力
    private int initialJumpY; // 記錄跳躍的初始Y座標

    // 磚塊屬性
    private int blockX = 200, blockY = 450; // 普通磚塊位置
    private int itemBlockX = 400, itemBlockY = 350; // 道具磚塊位置
    private int blockWidth = 50, blockHeight = 50;

    // 蘑菇屬性
    private int mushroomX, mushroomY;
    private int mushroomWidth = 30, mushroomHeight = 30;
    private boolean mushroomVisible = false;

    // 敵人屬性 (Goomba)
    private int goombaX = 600, goombaY;
    private int goombaWidth = 40, goombaHeight = 40;
    private int goombaSpeed = 2;
    private boolean goombaAlive = true;

    // 網路相關變數
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String assignedPlayerName; // 儲存 P1, P2 等
    private Map<String, int[]> allPlayerPositions; // 儲存所有玩家位置

    public client(String serverAddress, int serverPort) {
        setFocusable(true);
        addKeyListener(this);
        setDoubleBuffered(true);

        groundLevel = screenHeight - groundHeight;
        playerY = groundLevel - playerHeight; // 初始時角色站在地面上

        loadImages(); // 加載遊戲圖片

        // --- 客戶端網路設定 ---
        try {
            System.out.println("客戶端嘗試連接伺服器: " + serverAddress + ":" + serverPort);
            socket = new Socket(serverAddress, serverPort); // 連接到伺服器
            System.out.println("客戶端連接成功！");

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("客戶端串流初始化成功！");

            // 從伺服器接收分配的玩家名稱
            assignedPlayerName = (String) in.readObject(); // 接收 P1, P2 等
            System.out.println("您是: " + assignedPlayerName);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("連接伺服器失敗或讀取初始數據錯誤: " + e.getMessage());
            e.printStackTrace(); // 打印完整的錯誤堆棧資訊
            System.exit(1); // 連接失敗就退出程式
        }

        // 啟動兩個線程：一個用於客戶端遊戲邏輯和渲染，一個用於從伺服器讀取數據
        new Thread(this).start(); // 啟動遊戲主循環（用於渲染和發送輸入）
        new Thread(new ServerReader()).start(); // 啟動線程從伺服器讀取數據
    }

    private void loadImages() {
        // 載入主角圖片
        playerImage = new ImageIcon("mario.png").getImage();
        bigplayerImage = new ImageIcon("bigmario.png").getImage();
        playerjump = new ImageIcon("mariojump.png").getImage();
        bigplayerjump = new ImageIcon("bigmariojump.png").getImage();
        playerGif = new ImageIcon("mario.gif");
        bigplayerGif = new ImageIcon("bigmario.gif");

        // 載入背景圖片 (假設有 background1.png, background2.png 等)
        for (int i = 0; i < backgrounds.length; i++) {
            backgrounds[i] = new ImageIcon("background" + (i + 1) + ".png").getImage();
        }
        gameover = new ImageIcon("gameover.png").getImage();
    }

    // 負責從伺服器讀取數據的線程
    private class ServerReader implements Runnable {
        @Override
        public void run() {
            try {
                while (socket.isConnected()) {
                    Object serverData = in.readObject();
                    if (serverData instanceof Map) {
                        // 接收所有玩家的位置資訊
                        allPlayerPositions = (Map<String, int[]>) serverData;
                        repaint(); // 收到新數據後，請求重繪畫面
                    }
                    // 在這裡處理其他從伺服器接收到的遊戲狀態數據，例如敵人位置、物品狀態等
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("從伺服器讀取數據時發生錯誤: " + e.getMessage());
                // 如果連接斷開，嘗試清理資源
            } finally {
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

        // 繪製所有玩家（根據從伺服器接收到的位置）
        if (allPlayerPositions != null) {
            for (Map.Entry<String, int[]> entry : allPlayerPositions.entrySet()) {
                String pName = entry.getKey();
                int[] pos = entry.getValue();
                int currentP_X = pos[0];
                int currentP_Y = pos[1];

                // 繪製玩家圖片 (這裡簡化，所有玩家使用同一張圖片)
                // 您可以根據 assignedPlayerName 或從伺服器傳輸其他屬性來選擇不同的圖片
                Image playerToDraw = isbigmario ? bigplayerImage : playerImage; // 可以根據當前客戶端是否為大瑪利歐來決定

                // 這裡應該根據從伺服器接收到的玩家狀態來繪製各自玩家的圖片
                // 目前僅用本地的 playerImage，這需要從伺服器獲取或在客戶端本地判斷
                // 為了演示，我們假設所有玩家都用 playerImage
                g.drawImage(playerImage, currentP_X, currentP_Y, playerWidth, playerHeight, this);


                // 繪製玩家名稱在角色上方
                g.setColor(Color.WHITE);
                // 調整名稱位置使其居中顯示在角色上方
                int nameWidth = g.getFontMetrics().stringWidth(pName);
                g.drawString(pName, currentP_X + (playerWidth - nameWidth) / 2, currentP_Y - 10);
            }
        }

        // 以下為單機遊戲的渲染邏輯，在多人模式下大部分應由伺服器控制並同步到客戶端
        // 繪製磚塊
        g.setColor(Color.ORANGE);
        g.fillRect(blockX, blockY, blockWidth, blockHeight);
        g.setColor(Color.RED);
        g.fillRect(itemBlockX, itemBlockY, blockWidth, blockHeight);

        // 繪製蘑菇 (如果可見)
        if (mushroomVisible) {
            g.setColor(Color.PINK);
            g.fillOval(mushroomX, mushroomY, mushroomWidth, mushroomHeight);
        }

        // 繪製 Goomba (如果還活著)
        if (goombaAlive) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(goombaX, goombaY, goombaWidth, goombaHeight);
        }

        // 繪製地面
        g.setColor(new Color(139, 69, 19)); // 棕色
        g.fillRect(0, groundLevel, screenWidth, groundHeight);

        if (gameOver) {
            g.drawImage(gameover, 0, 0, screenWidth, screenHeight, this);
        }
    }


    // 遊戲邏輯更新 (在客戶端，主要處理本地的動畫和發送指令)
    private void gameLoop() {
        if (!gameOver) {
            // 這個方法在客戶端現在主要用於繪圖更新和發送輸入
            // 實際的遊戲狀態更新（例如移動、碰撞）由伺服器處理

            // Goomba 移動 (如果伺服器沒有處理，客戶端會獨立處理)
            // 在多人遊戲中，Goomba 的位置應由伺服器發送
            // if (goombaAlive) {
            //     goombaX += goombaSpeed;
            //     if (goombaX < 0 || goombaX > screenWidth - goombaWidth) {
            //         goombaSpeed *= -1;
            //     }
            // }

            // 判斷是否掉落
            if (playerY < groundLevel - playerHeight && !isJumping) {
                isFalling = true;
            } else if (playerY >= groundLevel - playerHeight) {
                isFalling = false;
                playerY = groundLevel - playerHeight;
            }

            // 應用重力
            if (isFalling) {
                playerY += gravity;
            }

            // 碰撞檢測 (這個應該主要在伺服器端處理，客戶端只做顯示)
            // checkCollision();
            // checkGoombaCollision();

            // 調整蘑菇位置 (如果伺服器沒有處理，客戶端會獨立處理)
            if (mushroomVisible) {
                mushroomY += gravity; // 蘑菇也會受重力影響
                if (mushroomY >= groundLevel - mushroomHeight) {
                    mushroomY = groundLevel - mushroomHeight;
                }
            }

            // 調整 Goomba 的 Y 座標到地面
            goombaY = groundLevel - goombaHeight;

            repaint();
        }
    }

    @Override
    public void run() {
        while (true) {
            // 客戶端的主要遊戲循環，負責渲染和可能的本地動畫
            gameLoop(); // 運行客戶端本地的遊戲邏輯（例如渲染）
            try {
                Thread.sleep(1000 / 60); // 每秒 60 幀
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            // 客戶端發送玩家輸入給伺服器
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                out.writeObject("MOVE_LEFT"); // 發送指令給伺服器
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                out.writeObject("MOVE_RIGHT"); // 發送指令給伺服器
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                out.writeObject("JUMP"); // 發送指令給伺服器
            }
            // 可以根據需要添加其他按鍵事件
        } catch (IOException ex) {
            System.err.println("發送指令到伺服器時發生錯誤: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // 在多人遊戲中，通常不需要在按鍵釋放時發送指令，
        // 除非你需要停止移動的指令。伺服器通常會處理連續移動。
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // 不常用於遊戲控制
    }

    // 碰撞檢測方法 (這些應該主要在伺服器端進行計算)
    private void checkCollision() {
        // 檢查與普通磚塊的碰撞
        checkBlockCollision(blockX, blockY);
        // 檢查與道具磚塊的碰撞
        checkBlockCollision(itemBlockX, itemBlockY);

        // 檢查與蘑菇的碰撞
        if (mushroomVisible &&
                playerX < mushroomX + mushroomWidth && playerX + playerWidth > mushroomX &&
                playerY < mushroomY + mushroomHeight && playerY + playerHeight > mushroomY) {
            mushroomVisible = false; // 蘑菇消失
            isbigmario = true; // 變成大瑪利歐
            playerWidth = 60;
            playerHeight = 60;
            // 通知伺服器玩家狀態改變
            // out.writeObject("PICKUP_MUSHROOM"); // 範例：發送撿到蘑菇的指令
        }
    }

    private void checkGoombaCollision() {
        if (goombaAlive &&
                playerX < goombaX + goombaWidth && playerX + playerWidth > goombaX &&
                playerY < goombaY + goombaHeight && playerY + playerHeight > goombaY) {
            // 如果角色從上方踩到 Goomba
            if (playerY + playerHeight - gravity <= goombaY) { // 確保是從上方踩到
                goombaAlive = false; // Goomba 死亡
                // 通知伺服器 Goomba 死亡
                // out.writeObject("KILL_GOOMBA"); // 範例：發送殺死 Goomba 的指令
            } else {
                gameOver = true; // 遊戲結束
                // 通知伺服器遊戲結束
                // out.writeObject("GAME_OVER"); // 範例：發送遊戲結束指令
            }
        }
    }

    private void checkBlockCollision(int blockX, int blockY) {
        // 角色與磚塊的通用碰撞檢測
        if (playerX < blockX + blockWidth && playerX + playerWidth > blockX &&
                playerY < blockY + blockHeight && playerY + playerHeight > blockY) {

            // 底部碰撞 (角色頭部撞到磚塊底部)
            if (isJumping && playerY <= blockY + blockHeight && playerY + playerHeight > blockY + blockHeight) {
                playerY = blockY + blockHeight; // 將角色Y座標設置在磚塊下方
                isJumping = false;
                isFalling = true; // 開始下落

                // 如果是道具磚塊，生成蘑菇
                if (blockX == itemBlockX && blockY == itemBlockY && !mushroomVisible) {
                    mushroomVisible = true;
                    mushroomX = itemBlockX + blockWidth / 2 - mushroomWidth / 2;
                    mushroomY = itemBlockY - mushroomHeight; // 蘑菇出現在道具磚塊上方
                }
            }

            // 左側碰撞
            else if (playerX + playerWidth > blockX && playerX < blockX + blockWidth / 2) {
                playerX = blockX - playerWidth; // 阻止角色穿過磚塊左側
            }

            // 右側碰撞
            else if (playerX < blockX + blockWidth && playerX + playerWidth > blockX + blockWidth / 2) {
                playerX = blockX + blockWidth; // 阻止角色穿過磚塊右側
            }
        }
    }


    private boolean isStandingOnBlock() { //站在地面或磚塊上
        if (isJumping || isFalling) {
            return false; // d若角色正在跳躍或下落，不進行支撐物判斷
        }

        if (playerX + playerWidth > blockX && playerX < blockX + blockWidth &&
                playerY + playerHeight == blockY) { // 檢查是否站在普通磚塊上
            return true;
        }

        if (playerX + playerWidth > itemBlockX && playerX < itemBlockX + blockWidth &&
                playerY + playerHeight == itemBlockY) { // 檢查是否站在道具磚塊上
            return true;
        }

        // 檢查是否站在地面上
        if (playerY + playerHeight == groundLevel) {
            return true;
        }

        return false; // 如果沒有支撐物，返回 false
    }

    // --- 主方法 ---
    public static void main(String[] args) {
        String serverAddress = "localhost"; // 預設為本地主機
        int serverPort = 12345; // 預設 Port

        if (args.length > 0) {
            serverAddress = args[0]; // 允許透過啟動參數指定伺服器地址
        }
        if (args.length > 1) {
            try {
                serverPort = Integer.parseInt(args[1]); // 允許透過啟動參數指定 Port
            } catch (NumberFormatException e) {
                System.err.println("無效的 Port 號碼，使用預設 Port: " + 12345);
            }
        }

        JFrame frame = new JFrame("馬力歐遊戲雛型 (客戶端)");
        client game = new client(serverAddress, serverPort); // <--- 在這裡創建實例
        frame.add(game);
        frame.setSize(game.screenWidth, game.screenHeight); // <--- 透過實例存取 screenWidth 和 screenHeight
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        // game.startGameLoop(); // 此行不再需要，因為線程已在構造函數中啟動
    }
}