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
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {

    private static final int DEFAULT_PORT = 12345;
    private static final int MAX_PLAYERS = 4;

    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final int GROUND_HEIGHT = 120;
    private static final int GROUND_LEVEL = SCREEN_HEIGHT - GROUND_HEIGHT;

    // 使用 ConcurrentHashMap 來儲存玩家狀態，它本身對增刪改查是線程安全的
    private Map<String, PlayerState> playerStates = new ConcurrentHashMap<>();
    // clients 列表用於遍歷發送，使用 synchronizedList 確保基本操作線程安全，但迭代仍需額外同步
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    private List<BlockState> blocks = Collections.synchronizedList(new ArrayList<>());
    private List<MushroomState> mushrooms = Collections.synchronizedList(new ArrayList<>());
    private List<GoombaState> goombas = Collections.synchronizedList(new ArrayList<>());


    public GameServer(int port) {
        // 初始化遊戲物件
        blocks.add(new BlockState(200, 450, 50, 50));
        blocks.add(new ItemBlockState(400, 350, 50, 50, false));

        for (int i = 0; i < 5; i++) {
            blocks.add(new BlockState(50 + i * 100, GROUND_LEVEL - 100, 50, 50));
        }

        mushrooms.add(new MushroomState(0, 0, 30, 30, false));

        goombas.add(new GoombaState(600, GROUND_LEVEL - 40, 40, 40, true, -2));


        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("遊戲伺服器已啟動，監聽 Port: " + port);

            startGameLoop(); // 啟動遊戲主循環，獨立於客戶端連接

            while (true) {
                Socket clientSocket = serverSocket.accept();
                
                // 注意：clients.size() 在此處作為一個初步的判斷，
                // 更精確的玩家數量應該從 playerStates.size() 或一個專門的計數器獲取
                if (clients.size() < MAX_PLAYERS) { 
                    System.out.println("新玩家連線: " + clientSocket.getInetAddress().getHostAddress());
                    
                    String newPlayerName;
                    int playerNumForPos;

                    // 在分配玩家名稱時，確保 playerStates 不會被同時修改
                    // 這裡的邏輯是找到下一個可用的 Px 名稱
                    synchronized (playerStates) { 
                        int nextPlayerNum = 1;
                        while(playerStates.containsKey("P" + nextPlayerNum)) {
                            nextPlayerNum++;
                        }
                        newPlayerName = "P" + nextPlayerNum;
                        playerNumForPos = nextPlayerNum; // 記錄這個數字用於計算初始位置
                    }

                    ClientHandler clientThread = new ClientHandler(clientSocket, newPlayerName);
                    
                    // 在添加 ClientHandler 到 clients 列表時同步
                    synchronized (clients) { 
                        clients.add(clientThread);
                    }
                    
                    // 根據分配的玩家數字計算初始位置
                    playerStates.put(newPlayerName, new PlayerState(newPlayerName, 50 + (playerNumForPos - 1) * 100, GROUND_LEVEL - 50));
                    
                    new Thread(clientThread).start();
                    System.out.println("成功為新玩家分配名稱: " + newPlayerName); // Debugging
                } else {
                    System.out.println("伺服器已滿，拒絕連線: " + clientSocket.getInetAddress().getHostAddress());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("伺服器啟動失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startGameLoop() {
        new Thread(() -> {
            long lastUpdateTime = System.nanoTime();
            final double GAME_FPS = 60.0;
            final double TIME_PER_UPDATE = 1_000_000_000 / GAME_FPS; // nanoseconds per update

            while (true) {
                long now = System.nanoTime();
                long elapsed = now - lastUpdateTime;

                if (elapsed >= TIME_PER_UPDATE) {
                    lastUpdateTime = now;

                    // 遊戲邏輯更新（玩家移動、物體移動、碰撞等）
                    playerStates.forEach((name, player) -> {
                        if (player.gameOver) return;

                        // 1. 處理水平移動
                        if (player.movingLeft) {
                            player.playerX -= player.playerSpeed;
                        }
                        if (player.movingRight) {
                            player.playerX += player.playerSpeed;
                        }

                        // 2. 應用重力到垂直速度
                        player.velocityY += player.gravity;

                        // 3. 根據垂直速度更新玩家位置
                        player.playerY += player.velocityY;

                        // 4. 確保玩家不會掉出畫面底部 (或固定在地面上)
                        if (player.playerY + player.playerHeight >= GROUND_LEVEL) {
                            player.playerY = GROUND_LEVEL - player.playerHeight;
                            player.velocityY = 0; // 停止垂直移動
                            player.isOnGround = true; // 玩家現在在地面上
                        } else {
                            player.isOnGround = false; // 玩家不在地面上 (空中或下落)
                        }

                        // 5. 確保玩家不會跑出左右邊界
                        if (player.playerX < 0) player.playerX = 0;
                        if (player.playerX + player.playerWidth > SCREEN_WIDTH) player.playerX = SCREEN_WIDTH - player.playerWidth;

                        // 6. 更新玩家大小 (如果吃到了蘑菇或受傷)
                        if (player.isbigmario && player.playerWidth == 50) {
                            player.playerWidth = 60;
                            player.playerHeight = 60;
                            player.playerY -= 10; // 變大時稍微上移，防止陷地
                        } else if (!player.isbigmario && player.playerWidth == 60) {
                            player.playerWidth = 50;
                            player.playerHeight = 50;
                            player.playerY += 10; // 變小時稍微下移
                        }
                    });

                    mushrooms.forEach(mushroom -> {
                        if (mushroom.isVisible) {
                            // 蘑菇簡單的下落邏輯
                            if (mushroom.y < GROUND_LEVEL - mushroom.height) {
                                mushroom.y += 2;
                            }
                            if (mushroom.y > GROUND_LEVEL - mushroom.height) {
                                mushroom.y = GROUND_LEVEL - mushroom.height;
                            }
                        }
                    });

                    goombas.forEach(goomba -> {
                        if (goomba.isAlive) {
                            goomba.x += goomba.speed;
                            if (goomba.x < 0 || goomba.x + goomba.width > SCREEN_WIDTH) {
                                goomba.speed *= -1; // 碰到邊界反向
                            }
                            goomba.y = GROUND_LEVEL - goomba.height; // 固定在地面
                        }
                    });

                    handleCollisions(); // 處理碰撞

                    broadcastGameState(); // 廣播遊戲狀態給所有客戶端
                } else {
                    try {
                        long sleepTime = (long) ((TIME_PER_UPDATE - elapsed) / 1_000_000);
                        if (sleepTime > 0) {
                            Thread.sleep(sleepTime);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
    }

    private void handleCollisions() {
        playerStates.forEach((playerName, player) -> {
            if (player.gameOver) return;

            // 玩家與磚塊碰撞
            blocks.forEach(block -> {
                if (player.collidesWith(block.x, block.y, block.width, block.height)) {
                    // 判斷碰撞方向
                    // 玩家從上方下落或跳躍落地到磚塊上
                    if (player.velocityY >= 0 && // 玩家正在下落或靜止
                        player.playerY + player.playerHeight - player.velocityY <= block.y && // 檢查上一個位置是否在方塊上方，現在撞到了
                        player.playerX < block.x + block.width && player.playerX + player.playerWidth > block.x) { // 水平重疊

                        player.playerY = block.y - player.playerHeight; // 將玩家固定在磚塊頂部
                        player.velocityY = 0; // 停止下落
                        player.isOnGround = true; // 玩家現在在磚塊上，視為在地面
                    }
                    // 玩家從下方擊中磚塊 (頭部碰撞)
                    else if (player.velocityY < 0 && // 玩家正在上升
                             player.playerY - player.velocityY >= block.y + block.height && // 檢查上一個位置是否在方塊下方，現在撞到了
                             player.playerX < block.x + block.width && player.playerX + player.playerWidth > block.x) {

                        player.playerY = block.y + block.height; // 將玩家固定在磚塊底部
                        player.velocityY = 0; // 停止上升，開始下落
                        player.isOnGround = false; // 不再在地面上

                        if (block instanceof ItemBlockState) {
                            ItemBlockState itemBlock = (ItemBlockState) block;
                            if (!itemBlock.isHit) {
                                itemBlock.isHit = true;
                                System.out.println("伺服器：玩家 " + playerName + " 擊中道具磚塊。");
                                mushrooms.stream()
                                         .filter(m -> !m.isVisible) // 找到第一個不可見的蘑菇
                                         .findFirst()
                                         .ifPresent(m -> {
                                             m.x = itemBlock.x + (itemBlock.width - m.width) / 2;
                                             m.y = itemBlock.y - m.height;
                                             m.isVisible = true;
                                             System.out.println("伺服器：蘑菇已生成在 " + m.x + "," + m.y);
                                         });
                            }
                        }
                    }
                    // 玩家從側面撞擊磚塊 (阻止穿透)
                    // 只有當玩家的 Y 軸範圍與磚塊的 Y 軸範圍有重疊時才處理水平碰撞
                    else if (player.playerY < block.y + block.height && player.playerY + player.playerHeight > block.y) {
                        // 玩家從左邊撞
                        if (player.playerX + player.playerWidth > block.x && player.playerX < block.x) {
                            player.playerX = block.x - player.playerWidth;
                        }
                        // 玩家從右邊撞
                        else if (player.playerX < block.x + block.width && player.playerX + player.playerWidth > block.x + block.width) {
                            player.playerX = block.x + block.width;
                        }
                    }
                }
            });

            // 玩家與蘑菇碰撞
            mushrooms.forEach(mushroom -> {
                if (mushroom.isVisible && player.collidesWith(mushroom.x, mushroom.y, mushroom.width, mushroom.height)) {
                    player.isbigmario = true;
                    mushroom.isVisible = false;
                    System.out.println("伺服器：玩家 " + playerName + " 吃到蘑菇。");
                }
            });

            // 玩家與 Goomba 碰撞
            goombas.forEach(goomba -> {
                if (goomba.isAlive && player.collidesWith(goomba.x, goomba.y, goomba.width, goomba.height)) {
                    // 判斷是否踩到 Goomba (玩家底部在 Goomba 頂部之上)
                    if (player.velocityY >= 0 && // 玩家正在下落或靜止
                        player.playerY + player.playerHeight - player.velocityY <= goomba.y && player.playerY + player.playerHeight > goomba.y) { // 檢查上一個位置是否在 Goomba 上方
                        goomba.isAlive = false;
                        System.out.println("伺服器：玩家 " + playerName + " 踩死 Goomba。");
                        player.velocityY = player.initialJumpVelocity / 2; // 短暫向上彈跳
                        player.isOnGround = false;
                    } else { // 側面或下方碰撞，玩家受傷或死亡
                        if (player.isbigmario) {
                            player.isbigmario = false;
                            System.out.println("伺服器：玩家 " + playerName + " 受到傷害，變回小瑪利歐。");
                        } else {
                            player.gameOver = true;
                            System.out.println("伺服器：玩家 " + playerName + " 遊戲結束 (被 Goomba 撞到)。");
                        }
                    }
                }
            });
        });
    }

    private void broadcastGameState() {
        Map<String, Object> fullGameState = new HashMap<>();

        // 從 ConcurrentHashMap 獲取玩家狀態，這是線程安全的
        Map<String, int[]> playerPositions = new HashMap<>();
        playerStates.forEach((name, state) -> {
            playerPositions.put(name, new int[]{
                state.playerX,
                state.playerY,
                state.playerWidth,
                state.playerHeight,
                state.isbigmario ? 1 : 0,
                state.gameOver ? 1 : 0
            });
        });
        fullGameState.put("players", playerPositions);

        // 將磚塊數據打包
        List<Map<String, Object>> blockData = new ArrayList<>();
        blocks.forEach(block -> {
            Map<String, Object> b = new HashMap<>();
            b.put("x", block.x);
            b.put("y", block.y);
            b.put("width", block.width);
            b.put("height", block.height);
            b.put("type", (block instanceof ItemBlockState) ? "item" : "normal");
            if (block instanceof ItemBlockState) {
                b.put("isHit", ((ItemBlockState) block).isHit);
            }
            blockData.add(b);
        });
        fullGameState.put("blocks", blockData);

        // 將蘑菇數據打包
        List<Map<String, Object>> mushroomData = new ArrayList<>();
        mushrooms.forEach(mushroom -> {
            Map<String, Object> m = new HashMap<>();
            m.put("x", mushroom.x);
            m.put("y", mushroom.y);
            m.put("width", mushroom.width);
            m.put("height", mushroom.height);
            m.put("isVisible", mushroom.isVisible);
            mushroomData.add(m);
        });
        fullGameState.put("mushrooms", mushroomData);

        // 將 Goomba 數據打包
        List<Map<String, Object>> goombaData = new ArrayList<>();
        goombas.forEach(goomba -> {
            Map<String, Object> g = new HashMap<>();
            g.put("x", goomba.x);
            g.put("y", goomba.y);
            g.put("width", goomba.width);
            g.put("height", goomba.height);
            g.put("isAlive", goomba.isAlive);
            goombaData.add(g);
        });
        fullGameState.put("goombas", goombaData);

        fullGameState.put("groundLevel", GROUND_LEVEL);

        // *** 關鍵修改：同步 clients 列表的迭代 ***
        synchronized (clients) { 
            for (ClientHandler client : clients) {
                try {
                    if (client.out != null) {
                        client.sendGameState(fullGameState);
                    }
                } catch (IOException e) {
                    System.err.println("向客戶端 " + client.playerName + " 發送狀態時發生錯誤: " + e.getMessage());
                    // 這裡可以選擇將發送失敗的客戶端標記為斷開，以便在安全時從列表中移除
                    // 但由於 ClientHandler 的 finally 塊會處理移除，這裡暫時不直接移除
                }
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String playerName;

        public ClientHandler(Socket socket, String name) {
            this.clientSocket = socket;
            this.playerName = name;
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                out.writeObject(playerName); // 向客戶端發送其被分配的名稱
                out.flush();
            } catch (IOException e) {
                System.err.println("客戶端 " + playerName + " 串流初始化失敗: " + e.getMessage());
                closeClientResources(); // 如果初始化失敗，立即關閉資源
            }
        }

        public void sendGameState(Map<String, Object> gameState) throws IOException {
            if (out != null) {
                out.reset(); // 重置序列化流，防止舊對象緩存導致問題
                out.writeObject(gameState);
                out.flush();
            }
        }

        @Override
        public void run() {
            try {
                while (clientSocket.isConnected()) {
                    Object clientInput = in.readObject(); // 讀取客戶端發送的按鍵狀態
                    PlayerState currentPlayerState = playerStates.get(playerName);

                    if (currentPlayerState == null || currentPlayerState.gameOver) {
                        // 如果玩家狀態不存在或遊戲已結束，則不處理其輸入
                        continue; 
                    }

                    if (clientInput instanceof Map) {
                        Map<String, Boolean> keyStates = (Map<String, Boolean>) clientInput;

                        currentPlayerState.movingLeft = keyStates.getOrDefault("MOVE_LEFT", false);
                        currentPlayerState.movingRight = keyStates.getOrDefault("MOVE_RIGHT", false);

                        if (keyStates.getOrDefault("JUMP", false)) {
                            // 只有在玩家在地面上時才允許跳躍
                            if (currentPlayerState.isOnGround) {
                                currentPlayerState.velocityY = currentPlayerState.initialJumpVelocity; // 設置初始向上速度
                                currentPlayerState.isOnGround = false; // 不再在地面上
                                // System.out.println("伺服器：玩家 " + playerName + " 開始跳躍。"); // 頻繁輸出，可註釋
                            }
                        }
                    } 
                    // 兼容舊的字符串指令，建議最終移除此部分以簡化代碼
                    else if (clientInput instanceof String) {
                        String command = (String) clientInput;
                        System.out.println("伺服器收到來自 " + playerName + " 的舊式指令: " + command);
                        switch (command) {
                            case "MOVE_LEFT":
                                currentPlayerState.movingLeft = true;
                                currentPlayerState.movingRight = false;
                                break;
                            case "MOVE_RIGHT":
                                currentPlayerState.movingRight = true;
                                currentPlayerState.movingLeft = false;
                                break;
                            case "JUMP":
                                if (currentPlayerState.isOnGround) {
                                    currentPlayerState.velocityY = currentPlayerState.initialJumpVelocity;
                                    currentPlayerState.isOnGround = false;
                                    System.out.println("伺服器：玩家 " + playerName + " 舊式跳躍指令。");
                                }
                                break;
                            case "STOP_MOVE":
                                currentPlayerState.movingLeft = false;
                                currentPlayerState.movingRight = false;
                                break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("客戶端 " + playerName + " 連線斷開或讀取錯誤: " + e.getMessage());
            } finally {
                // 在 finally 塊中安全地移除客戶端資源和狀態
                closeClientResources();
                System.out.println("客戶端 " + playerName + " 已斷開連線。");
                
                // *** 關鍵修改：在修改 clients 列表時同步 ***
                synchronized (clients) { 
                    clients.remove(this);
                }
                playerStates.remove(this.playerName); // ConcurrentHashMap 對移除操作是線程安全的
            }
        }

        private void closeClientResources() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                System.err.println("關閉客戶端資源時發生錯誤: " + e.getMessage());
            }
        }
    }

    // ====================================================================================
    // 以下是遊戲狀態類別，這些類別必須實現 Serializable 接口才能透過網路傳輸
    // ====================================================================================

    private static class PlayerState implements Serializable {
        public String name;
        public int playerX, playerY;
        public int playerWidth = 50;
        public int playerHeight = 50;
        public int playerSpeed = 3; 

        public double velocityY = 0; 
        public double initialJumpVelocity = -15; 
        public double gravity = 0.8; 

        public boolean movingLeft = false;
        public boolean movingRight = false;
        public boolean isOnGround = false; 

        public boolean isbigmario = false;
        public boolean gameOver = false;

        public PlayerState(String name, int playerX, int playerY) {
            this.name = name;
            this.playerX = playerX;
            this.playerY = playerY;
            // 初始大小根據 isbigmario 設置
            if (this.isbigmario) {
                this.playerWidth = 60;
                this.playerHeight = 60;
            } else {
                this.playerWidth = 50;
                this.playerHeight = 50;
            }
        }

        // 碰撞檢測方法
        public boolean collidesWith(int otherX, int otherY, int otherWidth, int otherHeight) {
            return playerX < otherX + otherWidth &&
                   playerX + playerWidth > otherX &&
                   playerY < otherY + otherHeight &&
                   playerY + playerHeight > otherY;
        }
    }

    private static class BlockState implements Serializable {
        public int x, y, width, height;
        public BlockState(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class ItemBlockState extends BlockState implements Serializable {
        public boolean isHit = false; // 是否已被撞擊
        public ItemBlockState(int x, int y, int width, int height, boolean isHit) {
            super(x, y, width, height);
            this.isHit = isHit;
        }
    }

    private static class MushroomState implements Serializable {
        public int x, y, width, height;
        public boolean isVisible; // 蘑菇是否可見
        public MushroomState(int x, int y, int width, int height, boolean isVisible) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.isVisible = isVisible;
        }
    }

    private static class GoombaState implements Serializable {
        public int x, y, width, height;
        public boolean isAlive; // Goomba 是否存活
        public int speed; // Goomba 移動速度
        public GoombaState(int x, int y, int width, int height, boolean isAlive, int speed) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.isAlive = isAlive;
            this.speed = speed;
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("無效的 Port 號碼，使用預設 Port: " + DEFAULT_PORT);
            }
        }
        new GameServer(port);
    }
}