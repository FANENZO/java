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
import java.util.concurrent.ConcurrentHashMap; // ConcurrentHashMap for thread-safe playerStates

public class GameServer {

    private static final int DEFAULT_PORT = 12345; // <-- 這是你需要定義的 DEFAULT_PORT
    private static final int MAX_PLAYERS = 4; // 設定最大玩家數

    private static final int SCREEN_WIDTH = 800; // 遊戲世界的寬度
    private static final int SCREEN_HEIGHT = 600; // 遊戲世界的高度
    private static final int GROUND_HEIGHT = 120; // 地面層的高度
    private static final int GROUND_LEVEL = SCREEN_HEIGHT - GROUND_HEIGHT; // 地面頂部的Y座標

    // 伺服器端維護所有玩家的狀態
    // 使用 ConcurrentHashMap 確保多線程安全
    private Map<String, PlayerState> playerStates = new ConcurrentHashMap<>();
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>()); // 同步列表以確保線程安全

    // 伺服器端維護所有遊戲物件的狀態
    private List<BlockState> blocks = Collections.synchronizedList(new ArrayList<>());
    private List<MushroomState> mushrooms = Collections.synchronizedList(new ArrayList<>());
    private List<GoombaState> goombas = Collections.synchronizedList(new ArrayList<>());


    public GameServer(int port) {
        // 初始化遊戲地圖中的磚塊
        blocks.add(new BlockState(200, 450, 50, 50)); // 普通磚塊
        blocks.add(new ItemBlockState(400, 350, 50, 50, false)); // 道具磚塊 (初始未被擊中)

        // 在地面上方一點建立一排普通磚塊作為平台 (示例)
        for (int i = 0; i < 5; i++) {
            blocks.add(new BlockState(50 + i * 100, GROUND_LEVEL - 100, 50, 50));
        }

        // 初始化蘑菇（初始不可見，從道具磚塊中生成）
        // 假設只有一個蘑菇，它在 ItemBlockState 被擊中後出現
        mushrooms.add(new MushroomState(0, 0, 30, 30, false));

        // 初始化 Goomba
        // Goomba 的 Y 座標應該在地面上
        goombas.add(new GoombaState(600, GROUND_LEVEL - 40, 40, 40, true, -2)); // Goomba 初始位置和速度


        // 啟動伺服器監聽客戶端連線
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("遊戲伺服器已啟動，監聽 Port: " + port);

            // 啟動一個獨立的線程來處理遊戲邏輯更新和狀態廣播
            startGameLoop();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                if (clients.size() < MAX_PLAYERS) {
                    System.out.println("新玩家連線: " + clientSocket.getInetAddress().getHostAddress());
                    ClientHandler clientThread = new ClientHandler(clientSocket, "P" + (clients.size() + 1));
                    clients.add(clientThread);
                    playerStates.put(clientThread.playerName, new PlayerState(clientThread.playerName, 50 + clients.size() * 100, GROUND_LEVEL - 50)); // 為新玩家創建狀態，並給予不同起始X位置
                    new Thread(clientThread).start();
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
            while (true) {
                // 1. 更新所有玩家的狀態 (來自客戶端輸入和伺服器物理)
                playerStates.forEach((name, player) -> {
                    // 應用重力 (如果不在地面上且沒有在跳躍)
                    if (!player.isJumping && player.playerY < GROUND_LEVEL - player.playerHeight) {
                        player.playerY += player.gravity;
                    }
                    // 處理跳躍
                    if (player.isJumping) {
                        player.playerY += player.jumpStrength; // 向上移動
                        player.jumpTicks--;
                        if (player.jumpTicks <= 0) {
                            player.isJumping = false;
                            player.jumpStrength = 0; // 重置跳躍力量
                        }
                    }

                    // 確保玩家不會掉出畫面底部 (或固定在地面上)
                    if (player.playerY > GROUND_LEVEL - player.playerHeight) {
                        player.playerY = GROUND_LEVEL - player.playerHeight;
                        player.isJumping = false; // 落地時重置跳躍狀態
                        player.jumpStrength = -15; // 重置為初始跳躍力量
                    }
                    // 確保玩家不會跑出左右邊界
                    if (player.playerX < 0) player.playerX = 0;
                    if (player.playerX + player.playerWidth > SCREEN_WIDTH) player.playerX = SCREEN_WIDTH - player.playerWidth;

                    // 更新玩家大小 (如果吃到了蘑菇)
                    if (player.isbigmario && player.playerWidth == 50) { // 從小變大
                        player.playerWidth = 60;
                        player.playerHeight = 60;
                        player.playerY -= 10; // 向上微調，防止陷入地面
                    } else if (!player.isbigmario && player.playerWidth == 60) { // 從大變小 (例如受傷後)
                        player.playerWidth = 50;
                        player.playerHeight = 50;
                        player.playerY += 10; // 向下微調
                    }
                });


                // 2. 更新其他遊戲物件的狀態 (例如蘑菇、敵人移動)
                mushrooms.forEach(mushroom -> {
                    if (mushroom.isVisible) {
                        // 蘑菇受重力下落
                        if (mushroom.y < GROUND_LEVEL - mushroom.height) {
                            mushroom.y += 2; // 簡單重力
                        }
                        if (mushroom.y > GROUND_LEVEL - mushroom.height) {
                            mushroom.y = GROUND_LEVEL - mushroom.height; // 落在地面上
                        }
                    }
                });

                goombas.forEach(goomba -> {
                    if (goomba.isAlive) {
                        goomba.x += goomba.speed;
                        // Goomba 碰到邊界反向
                        if (goomba.x < 0 || goomba.x + goomba.width > SCREEN_WIDTH) {
                            goomba.speed *= -1;
                        }
                        // Goomba 也要受重力影響，或者確保它在地面上
                        goomba.y = GROUND_LEVEL - goomba.height;
                    }
                });

                // 3. 處理所有碰撞 (這是核心部分)
                handleCollisions();

                // 4. 將最新的遊戲狀態廣播給所有客戶端
                broadcastGameState();

                try {
                    Thread.sleep(1000 / 60); // Roughly 60 FPS server updates
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private void handleCollisions() {
        playerStates.forEach((playerName, player) -> {
            if (player.gameOver) return; // 如果玩家已經結束遊戲，不處理碰撞

            // 玩家與磚塊碰撞
            blocks.forEach(block -> {
                if (player.collidesWith(block.x, block.y, block.width, block.height)) {
                    // 處理玩家從下方擊中磚塊 (通常觸發道具)
                    if (player.playerY + player.playerHeight > block.y && // 玩家底部在磚塊Y之上
                        player.playerY + player.playerHeight <= block.y + player.gravity + 1 && // 玩家即將落在磚塊上
                        player.playerX < block.x + block.width && player.playerX + player.playerWidth > block.x) { // 水平重疊
                        
                        // 判斷是否從下方擊中 (頭部碰撞)
                        if (player.playerY < block.y + block.height && player.playerY + player.jumpStrength >= block.y + block.height) {
                            // 是從下方擊中
                            if (block instanceof ItemBlockState) {
                                ItemBlockState itemBlock = (ItemBlockState) block;
                                if (!itemBlock.isHit) {
                                    itemBlock.isHit = true; // 標記磚塊已被擊中
                                    System.out.println("伺服器：玩家 " + playerName + " 擊中道具磚塊。");
                                    // 生成蘑菇 (找到第一個不可見的蘑菇並使其可見)
                                    mushrooms.stream()
                                             .filter(m -> !m.isVisible)
                                             .findFirst()
                                             .ifPresent(m -> {
                                                m.x = itemBlock.x + (itemBlock.width - m.width) / 2;
                                                m.y = itemBlock.y - m.height;
                                                m.isVisible = true;
                                                System.out.println("伺服器：蘑菇已生成在 " + m.x + "," + m.y);
                                             });
                                }
                            }
                            // 阻止玩家穿透磚塊（向上移動時撞到）
                            player.playerY = block.y + block.height;
                            player.jumpStrength = 0; // 停止向上移動
                            player.isJumping = false; // 停止跳躍狀態，開始下落
                        }
                    }

                    // 處理玩家落在磚塊上 (站在磚塊上)
                    if (player.playerY + player.playerHeight >= block.y && 
                        player.playerY + player.playerHeight <= block.y + player.gravity + 1 && // 即將或剛好落在磚塊上
                        player.playerX < block.x + block.width && player.playerX + player.playerWidth > block.x) {
                        
                        player.playerY = block.y - player.playerHeight; // 將玩家固定在磚塊頂部
                        player.isJumping = false;
                        player.jumpStrength = -15; // 重置跳躍力量
                    }
                    
                    // 處理玩家從側面撞擊磚塊 (阻止穿透)
                    // 如果玩家不是從上方或下方直接碰撞，則處理水平方向的碰撞
                    if (!(player.playerY + player.playerHeight > block.y && player.playerY < block.y + block.height)) {
                        // 玩家從左邊撞
                        if (player.playerX < block.x + block.width && player.playerX + player.playerWidth > block.x && player.playerX < block.x) {
                            player.playerX = block.x - player.playerWidth;
                        }
                        // 玩家從右邊撞
                        else if (player.playerX < block.x + block.width && player.playerX + player.playerWidth > block.x && player.playerX > block.x) {
                            player.playerX = block.x + block.width;
                        }
                    }
                }
            });

            // 玩家與蘑菇碰撞
            mushrooms.forEach(mushroom -> {
                if (mushroom.isVisible && player.collidesWith(mushroom.x, mushroom.y, mushroom.width, mushroom.height)) {
                    player.isbigmario = true; // 玩家變大
                    mushroom.isVisible = false; // 蘑菇消失
                    System.out.println("伺服器：玩家 " + playerName + " 吃到蘑菇。");
                }
            });

            // 玩家與 Goomba 碰撞
            goombas.forEach(goomba -> {
                if (goomba.isAlive && player.collidesWith(goomba.x, goomba.y, goomba.width, goomba.height)) {
                    // 判斷是否踩到 Goomba (玩家底部在 Goomba 頂部之上)
                    if (player.playerY + player.playerHeight <= goomba.y + player.gravity + 5 && player.playerY + player.playerHeight > goomba.y) {
                        goomba.isAlive = false; // Goomba 死亡
                        System.out.println("伺服器：玩家 " + playerName + " 踩死 Goomba。");
                        // 玩家可以彈跳一下，模擬踩踏
                        player.isJumping = true;
                        player.jumpTicks = 10; // 短暫跳躍
                        player.jumpStrength = -10; // 短暫的向上力量
                    } else { // 側面或下方碰撞，玩家受傷或死亡
                        if (player.isbigmario) {
                            player.isbigmario = false; // 從大瑪利歐變回小瑪利歐
                            System.out.println("伺服器：玩家 " + playerName + " 受到傷害，變回小瑪利歐。");
                        } else {
                            player.gameOver = true; // 玩家遊戲結束
                            System.out.println("伺服器：玩家 " + playerName + " 遊戲結束 (被 Goomba 撞到)。");
                        }
                    }
                }
            });
        });
    }

    private void broadcastGameState() {
        Map<String, Object> fullGameState = new HashMap<>();

        // 玩家狀態
        Map<String, int[]> playerPositions = new HashMap<>();
        playerStates.forEach((name, state) -> {
            playerPositions.put(name, new int[]{
                state.playerX,
                state.playerY,
                state.playerWidth, // 傳輸玩家當前寬度
                state.playerHeight, // 傳輸玩家當前高度
                state.isbigmario ? 1 : 0, // 是否為大瑪利歐
                state.gameOver ? 1 : 0 // 玩家是否遊戲結束
            });
        });
        fullGameState.put("players", playerPositions);

        // 磚塊狀態
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

        // 蘑菇狀態
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

        // Goomba 狀態
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

        // 地面高度
        fullGameState.put("groundLevel", GROUND_LEVEL);

        // 將完整的遊戲狀態發送給每個客戶端
        for (ClientHandler client : clients) {
            try {
                // 只有當客戶端連接時才發送數據
                if (client.out != null) {
                    client.sendGameState(fullGameState);
                }
            } catch (IOException e) {
                System.err.println("向客戶端 " + client.playerName + " 發送狀態時發生錯誤: " + e.getMessage());
                // 客戶端斷線處理: 從列表中移除
                // clients.remove(client); // 不應在迭代中修改列表
                // playerStates.remove(client.playerName);
            }
        }
    }

    // 處理單一客戶端連線的線程
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
                // 首次連線時發送分配的玩家名稱
                out.writeObject(playerName);
            } catch (IOException e) {
                System.err.println("客戶端 " + playerName + " 串流初始化失敗: " + e.getMessage());
                closeClientResources();
            }
        }

        public void sendGameState(Map<String, Object> gameState) throws IOException {
            if (out != null) {
                out.reset(); // 清除緩存，確保發送最新狀態
                out.writeObject(gameState);
                out.flush();
            }
        }

        @Override
        public void run() {
            try {
                while (clientSocket.isConnected()) {
                    Object clientInput = in.readObject();
                    if (clientInput instanceof String) {
                        String command = (String) clientInput;
                        // System.out.println("伺服器收到來自 " + playerName + " 的指令: " + command);

                        // 根據指令更新玩家狀態 (在伺服器端)
                        PlayerState currentPlayerState = playerStates.get(playerName);
                        if (currentPlayerState != null && !currentPlayerState.gameOver) {
                            switch (command) {
                                case "MOVE_LEFT":
                                    currentPlayerState.playerX -= currentPlayerState.playerSpeed;
                                    break;
                                case "MOVE_RIGHT":
                                    currentPlayerState.playerX += currentPlayerState.playerSpeed;
                                    break;
                                case "JUMP":
                                    if (!currentPlayerState.isJumping && currentPlayerState.playerY >= GROUND_LEVEL - currentPlayerState.playerHeight) { // 只有在地面上才能跳
                                        currentPlayerState.isJumping = true;
                                        currentPlayerState.jumpTicks = 30; // 假設跳躍持續的幀數
                                        currentPlayerState.jumpStrength = -15; // 設置跳躍初始力量
                                    }
                                    break;
                                // ... handle other commands
                            }
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("客戶端 " + playerName + " 連線斷開或讀取錯誤: " + e.getMessage());
            } finally {
                closeClientResources();
                System.out.println("客戶端 " + playerName + " 已斷開連線。");
                // 從伺服器端移除斷線玩家的狀態和處理線程
                clients.remove(this); // 'this' 指向當前 ClientHandler 實例
                playerStates.remove(this.playerName);
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

    // 遊戲物件的狀態類別 (需要實現 Serializable 才能通過網路傳輸)
    // PlayerState 類別的完整定義
    private static class PlayerState implements Serializable {
        public String name;
        public int playerX, playerY;
        public int playerWidth = 50;  // 默認小瑪利歐寬度
        public int playerHeight = 50; // 默認小瑪利歐高度
        public int playerSpeed = 5;   // 玩家移動速度
        public boolean isJumping = false;
        public int jumpStrength = -15; // 向上移動的力度，負值表示向上
        public int jumpTicks = 0;      // 跳躍持續時間計數器
        public int gravity = 1;        // 向下重力
        public boolean isbigmario = false; // 是否為大瑪利歐
        public boolean gameOver = false;   // 玩家自己的遊戲結束狀態

        public PlayerState(String name, int playerX, int playerY) {
            this.name = name;
            this.playerX = playerX;
            this.playerY = playerY;
            // 初始尺寸根據 isbigmario 狀態來設定
            if (this.isbigmario) {
                this.playerWidth = 60;
                this.playerHeight = 60;
            } else {
                this.playerWidth = 50;
                this.playerHeight = 50;
            }
        }

        // 輔助方法：用於伺服器端的簡單軸對齊邊界框碰撞檢測 (AABB)
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
        public boolean isHit = false; // 是否已被擊中過
        public ItemBlockState(int x, int y, int width, int height, boolean isHit) {
            super(x, y, width, height);
            this.isHit = isHit;
        }
    }

    private static class MushroomState implements Serializable {
        public int x, y, width, height;
        public boolean isVisible;
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
        public boolean isAlive;
        public int speed;
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
                port = Integer.parseInt(args[0]); // Allow custom port via launch argument
            } catch (NumberFormatException e) {
                System.err.println("無效的 Port 號碼，使用預設 Port: " + DEFAULT_PORT);
            }
        }
        // GameServer server = new GameServer(port); // 不需要再單獨呼叫 start()
        new GameServer(port); // 直接在構造函數中啟動伺服器和遊戲循環
    }
}