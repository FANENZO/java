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
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class client extends JPanel implements KeyListener, Runnable {
    // 主角屬性 (這些變數現在只用於本地繪圖，其值會從伺服器同步)
    private int playerWidth = 50, playerHeight = 50; // 客戶端本地定義，伺服器會傳遞實際尺寸
    private Image playerImage, bigplayerImage; // 角色圖片
    private Image playerjump, bigplayerjump; // 跳躍圖片
    // 移除 ImageIcon playerGif, bigplayerGif; 如果你不打算在客戶端處理GIF動畫，而是靜態圖片。
    // 如果是動畫，並且希望ImageIcon自動處理動畫，可以保留並在繪圖時使用 playerGif.getImage()

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
    private Map<String, int[]> allPlayerPositions; // 儲存所有玩家位置及狀態
    private boolean gameOver = false; // 客戶端本地的遊戲結束狀態

    // 網路相關變數
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String assignedPlayerName; // 儲存 P1, P2 等

    //蘑菇
    private Image mushroomImage;
    //goomba
    private Image goombaImage;

    public client(String serverAddress, int serverPort) {
        setFocusable(true);
        addKeyListener(this);
        setDoubleBuffered(true); // 啟用雙緩衝，減少閃爍

        loadImages(); // 加載遊戲圖片

        try {
            System.out.println("客戶端嘗試連接伺服器: " + serverAddress + ":" + serverPort);
            socket = new Socket(serverAddress, serverPort);
            System.out.println("客戶端連接成功！");

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("客戶端串流初始化成功！");

            // 從伺服器接收分配的玩家名稱
            assignedPlayerName = (String) in.readObject();
            System.out.println("您是: " + assignedPlayerName);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("連接伺服器失敗或讀取初始數據錯誤: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // 啟動兩個線程：一個用於客戶端遊戲邏輯和渲染，一個用於從伺服器讀取數據
        // 這裡不再需要 new Thread(this).start();
        // 因為 repaint() 將完全由 ServerReader 驅動
        new Thread(new ServerReader()).start(); // 啟動線程從伺服器讀取數據
    }

    private void loadImages() {
        // 載入主角圖片
        playerImage = new ImageIcon("mario.png").getImage();
        bigplayerImage = new ImageIcon("bigmario.png").getImage();
        playerjump = new ImageIcon("mariojump.png").getImage();
        bigplayerjump = new ImageIcon("bigmariojump.png").getImage();
        // 如果這些是GIF動畫，並且你希望ImageIcon自行處理幀更新，可以保留它們：
        // playerGif = new ImageIcon("mario.gif");
        // bigplayerGif = new ImageIcon("bigmario.gif");

        // 載入背景圖片 (假設有 background1.png, background2.png 等)
        for (int i = 0; i < backgrounds.length; i++) {
            backgrounds[i] = new ImageIcon("background" + (i + 1) + ".png").getImage();
        }
        gameover = new ImageIcon("gameover.png").getImage();
        // 載入蘑菇圖片
        mushroomImage = new ImageIcon("images/mushroom.png").getImage();
        // 載入 Goomba 圖片
        goombaImage = new ImageIcon("images/8-bit-goomba-removebg-preview.png").getImage();

    }

    // 負責從伺服器讀取數據的線程
    private class ServerReader implements Runnable {
        @Override
        public void run() {
            try {
                while (socket.isConnected()) {
                    Object serverData = in.readObject();
                    if (serverData instanceof Map) {
                        Map<String, Object> fullGameState = (Map<String, Object>) serverData;

                        allPlayerPositions = (Map<String, int[]>) fullGameState.get("players");
                        receivedBlocks = (List<Map<String, Object>>) fullGameState.get("blocks");
                        receivedMushrooms = (List<Map<String, Object>>) fullGameState.get("mushrooms");
                        receivedGoombas = (List<Map<String, Object>>) fullGameState.get("goombas");
                        groundLevel = (int) fullGameState.get("groundLevel");

                        // 判斷當前客戶端玩家是否遊戲結束
                        if (allPlayerPositions != null && allPlayerPositions.containsKey(assignedPlayerName)) {
                            int[] currentPlayerStateArray = allPlayerPositions.get(assignedPlayerName);
                            // 假設 gameOver 資訊在陣列的第5個索引 (0-based: X, Y, Width, Height, IsBig, IsGameOver)
                            if (currentPlayerStateArray.length > 5) {
                                gameOver = (currentPlayerStateArray[5] == 1);
                            }
                        }

                        // 只有在接收到新數據時才調用 repaint()
                        repaint();
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("從伺服器讀取數據時發生錯誤: " + e.getMessage());
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

        // 繪製所有玩家
        if (allPlayerPositions != null) {
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
                    // 如果這個玩家遊戲結束，可能就不再繪製他
                    continue;
                }

                Image playerToDraw;
                // 注意：如果你有跑步動畫的 GIF，並且希望它自動播放，
                // 則需要使用 ImageIcon.getImage() 來獲取當前幀。
                // 如果是靜態圖片，根據狀態選擇圖片。
                // 這裡假設 playerImage/bigplayerImage 已經是載入好的靜態圖片
                // 或者你希望 ImageIcon 自動處理 GIF 動畫。
                if (isBigMario) {
                    playerToDraw = bigplayerImage;
                    // 如果有大瑪利歐GIF動畫，可以這樣使用：
                    // playerToDraw = bigplayerGif.getImage();
                } else {
                    playerToDraw = playerImage;
                    // 如果有小瑪利歐GIF動畫，可以這樣使用：
                    // playerToDraw = playerGif.getImage();
                }

                g.drawImage(playerToDraw, currentP_X, currentP_Y, currentP_Width, currentP_Height, this);

                // 繪製玩家名稱
                g.setColor(Color.WHITE);
                int nameWidth = g.getFontMetrics().stringWidth(pName);
                g.drawString(pName, currentP_X + (currentP_Width - nameWidth) / 2, currentP_Y - 10);
            }
        }

        // 繪製磚塊
        if (receivedBlocks != null) {
            for (Map<String, Object> block : receivedBlocks) {
                int bx = (int) block.get("x");
                int by = (int) block.get("y");
                int bwidth = (int) block.get("width");
                int bheight = (int) block.get("height");
                String type = (String) block.get("type");
                boolean isHit = (type.equals("item") && block.containsKey("isHit")) ? (boolean) block.get("isHit") : false;

                if (type.equals("item")) {
                    g.setColor(isHit ? Color.GRAY : Color.RED);
                } else {
                    g.setColor(Color.ORANGE);
                }
                g.fillRect(bx, by, bwidth, bheight);
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
                    g.drawImage(mushroomImage, mx, my, mwidth, mheight, this); // 使用你的蘑菇圖片
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

        // 繪製地面
        g.setColor(new Color(139, 69, 19)); // 棕色
        g.fillRect(0, groundLevel, screenWidth, screenHeight - groundLevel);

        // 如果當前客戶端玩家遊戲結束，顯示 Game Over 畫面
        if (gameOver) {
            g.drawImage(gameover, 0, 0, screenWidth, screenHeight, this);
        }
    }

    @Override
    public void run() {
        // 這個 run 方法現在變得無用，因為 repaint() 完全由 ServerReader 驅動。
        // 你可以安全地移除這個方法，或者讓它做一些非繪圖的客戶端本地更新，
        // 但目前遊戲狀態是完全由伺服器驅動的。
        // 為了確保 Swing 執行緒安全，所有 Swing 繪圖應該在 Event Dispatch Thread (EDT) 上進行。
        // repaint() 自動處理這個。

        // 為了避免空循環導致CPU空轉，可以這樣做，但它不會觸發額外的 repaint()
        while(true) {
            try {
                Thread.sleep(100); // 減少 CPU 消耗，但不再主動觸發繪圖
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            if (out != null) { // 確保輸出流已初始化
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    out.writeObject("MOVE_LEFT");
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    out.writeObject("MOVE_RIGHT");
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    out.writeObject("JUMP");
                }
                out.flush(); // 確保數據立即發送
            }
        } catch (IOException ex) {
            System.err.println("發送指令到伺服器時發生錯誤: " + ex.getMessage());
            // ex.printStackTrace(); // 在正常運行時可以註釋掉這行，避免過多輸出
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // 通常用於停止移動指令，目前伺服器模型可能不需要
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
        frame.setVisible(true);
    }
}