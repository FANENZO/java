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

    private Map<String, PlayerState> playerStates = new ConcurrentHashMap<>();
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    private List<BlockState> blocks = Collections.synchronizedList(new ArrayList<>());
    private List<MushroomState> mushrooms = Collections.synchronizedList(new ArrayList<>());
    private List<GoombaState> goombas = Collections.synchronizedList(new ArrayList<>());
    private List<FireballState> fireballs = Collections.synchronizedList(new ArrayList<>());


    public GameServer(int port) {
        // 初始化遊戲物件
        blocks.add(new ItemBlockState(400, 350, 50, 50, false));

        for (int i = 0; i < 5; i++) {
            blocks.add(new BlockState(50 + i * 100, GROUND_LEVEL - 100, 50, 50));
        }

        mushrooms.add(new MushroomState(0, 0, 30, 30, false)); // 初始化為不可見

        //goombas.add(new GoombaState(600, GROUND_LEVEL - 40, 40, 40, true, -2));
        mushrooms.add(new MushroomState(600, GROUND_LEVEL - 40, 40, 40, true));


        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("遊戲伺服器已啟動，監聽 Port: " + port);

            startGameLoop(); 

            while (true) {
                Socket clientSocket = serverSocket.accept();
                
                if (clients.size() < MAX_PLAYERS) { 
                    System.out.println("新玩家連線: " + clientSocket.getInetAddress().getHostAddress());
                    
                    String newPlayerName;
                    int playerNumForPos;

                    synchronized (playerStates) { 
                        int nextPlayerNum = 1;
                        while(playerStates.containsKey("P" + nextPlayerNum)) {
                            nextPlayerNum++;
                        }
                        newPlayerName = "P" + nextPlayerNum;
                        playerNumForPos = nextPlayerNum; 
                    }

                    ClientHandler clientThread = new ClientHandler(clientSocket, newPlayerName);
                    
                    synchronized (clients) { 
                        clients.add(clientThread);
                    }
                    
                    playerStates.put(newPlayerName, new PlayerState(newPlayerName, 50 + (playerNumForPos - 1) * 100, GROUND_LEVEL - 50));
                    
                    new Thread(clientThread).start();
                    System.out.println("成功為新玩家分配名稱: " + newPlayerName); 
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
            final double TIME_PER_UPDATE = 1_000_000_000 / GAME_FPS; 

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
                            player.velocityY = 0; 
                            player.isOnGround = true; 
                        } else {
                            player.isOnGround = false; 
                        }

                        // 5. 確保玩家不會跑出左右邊界
                        if (player.playerX < 0) player.playerX = 0;
                        if (player.playerX + player.playerWidth > SCREEN_WIDTH) player.playerX = SCREEN_WIDTH - player.playerWidth;

                        // 6. 更新玩家大小 (如果吃到了蘑菇或受傷)
                        if (player.isbigmario && player.playerWidth == 50) { // 變大
                            player.playerWidth = 60;
                            player.playerHeight = 60;
                            player.playerY -= 10; // 向上微調以保持底部對齊
                        } else if (!player.isbigmario && player.playerWidth == 60) { // 變回小瑪利歐
                            player.playerWidth = 50;
                            player.playerHeight = 50;
                            player.playerY += 10; // 向下微調以保持底部對齊
                        }
                    });

                    mushrooms.forEach(mushroom -> {
                        if (mushroom.isVisible) {
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
                            // 簡易邊界反彈
                            if (goomba.x < 0 || goomba.x + goomba.width > SCREEN_WIDTH) {
                                goomba.speed *= -1; 
                            }
                            goomba.y = GROUND_LEVEL - goomba.height; // 始終保持在地面上
                        }
                    });

                    fireballs.removeIf(fireball -> {
                        if (!fireball.isAlive) return true; // 如果火球已經標記為不活躍，則移除
                        
                        fireball.x += fireball.speedX; // 更新火球水平位置

                        // 檢查火球是否出界
                        if (fireball.x < -fireball.width || fireball.x > SCREEN_WIDTH) {
                            System.out.println("伺服器：火球出界，移除。");
                            return true; // 出界則移除
                        }
                        return false; // 否則保留
                    });


                    handleCollisions(); 

                    broadcastGameState(); 
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
                // 檢查玩家底部是否與磚塊頂部碰撞 (用於站立)
                if (player.velocityY >= 0 && 
                    player.playerX < block.x + block.width &&
                    player.playerX + player.playerWidth > block.x &&
                    player.playerY + player.playerHeight <= block.y + player.velocityY && // 確保從上方接觸
                    player.playerY + player.playerHeight + player.velocityY >= block.y) {
                    
                    player.playerY = block.y - player.playerHeight; // 落在磚塊上
                    player.velocityY = 0;
                    player.isOnGround = true;
                }
                // 檢查玩家頭部是否與磚塊底部碰撞 (用於頂磚塊)
                else if (player.velocityY < 0 && 
                         player.playerX < block.x + block.width &&
                         player.playerX + player.playerWidth > block.x &&
                         player.playerY >= block.y + block.height + player.velocityY && // 確保從下方接觸
                         player.playerY + player.velocityY <= block.y + block.height) {
                    
                    player.playerY = block.y + block.height; // 撞到磚塊底部
                    player.velocityY = 0; // 反彈
                    
                    if (block instanceof ItemBlockState) {
                        ItemBlockState itemBlock = (ItemBlockState) block;
                        if (!itemBlock.isHit) {
                            itemBlock.isHit = true;
                            // 觸發蘑菇出現邏輯
                            mushrooms.forEach(mushroom -> {
                                if (!mushroom.isVisible) { // 找到第一個不可見的蘑菇
                                    mushroom.x = block.x;
                                    mushroom.y = block.y - mushroom.height; // 出現在磚塊上方
                                    mushroom.isVisible = true;
                                    System.out.println("伺服器：蘑菇出現於 (" + mushroom.x + ", " + mushroom.y + ")");
                                }
                            });
                        }
                    }
                }
            });

            // 玩家與蘑菇碰撞
            mushrooms.forEach(mushroom -> {
                if (mushroom.isVisible &&
                    player.playerX < mushroom.x + mushroom.width &&
                    player.playerX + player.playerWidth > mushroom.x &&
                    player.playerY < mushroom.y + mushroom.height &&
                    player.playerY + player.playerHeight > mushroom.y) {
                    
                    player.isbigmario = true;
                    mushroom.isVisible = false; // 蘑菇消失
                    System.out.println("伺服器：玩家 " + playerName + " 吃到蘑菇，變大瑪利歐。");
                }
            });

            // 玩家與 Goomba 碰撞
            goombas.forEach(goomba -> {
                if (goomba.isAlive &&
                    player.playerX < goomba.x + goomba.width &&
                    player.playerX + player.playerWidth > goomba.x &&
                    player.playerY < goomba.y + goomba.height &&
                    player.playerY + player.playerHeight > goomba.y) {

                    // 判斷是否踩到 Goomba (玩家從上方落下，Goomba 死亡)
                    if (player.velocityY > 0 && player.playerY + player.playerHeight - player.velocityY < goomba.y) {
                        goomba.isAlive = false;
                        player.velocityY = -player.initialJumpVelocity / 2;// 踩死後小跳一下
                        System.out.println("伺服器：玩家 " + playerName + " 踩死了 Goomba。");
                    } else { // 被 Goomba 撞到
                        if (player.isbigmario) {
                            player.isbigmario = false; // 變回小瑪利歐
                            System.out.println("伺服器：玩家 " + playerName + " 被 Goomba 撞到，變回小瑪利歐。");
                        } else {
                            player.gameOver = true; // 遊戲結束
                            System.out.println("伺服器：玩家 " + playerName + " 遊戲結束。");
                        }
                    }
                }
            });
        });

        // 火球與磚塊、Goomba 碰撞
        fireballs.forEach(fireball -> {
            if (!fireball.isAlive) return;

            // 火球與磚塊碰撞
            blocks.forEach(block -> {
                if (fireball.collidesWith(block.x, block.y, block.width, block.height)) {
                    fireball.isAlive = false; // 火球消失
                    System.out.println("伺服器：火球撞到磚塊，消失。");
                    // 可以添加磚塊被火球打爆的邏輯
                }
            });

            // 火球與 Goomba 碰撞
            goombas.forEach(goomba -> {
                if (goomba.isAlive && fireball.collidesWith(goomba.x, goomba.y, goomba.width, goomba.height)) {
                    fireball.isAlive = false; // 火球消失
                    goomba.isAlive = false; // Goomba 死亡
                    System.out.println("伺服器：火球擊中 Goomba，Goomba 死亡。");
                }
            });
        });
    }


    private void broadcastGameState() {
        Map<String, Object> gameState = new HashMap<>();
        
        Map<String, int[]> currentPlayers = new HashMap<>();
        playerStates.forEach((name, player) -> {
            currentPlayers.put(name, new int[]{player.playerX, player.playerY, player.playerWidth, player.playerHeight, player.isbigmario ? 1 : 0, player.gameOver ? 1 : 0});
        });
        gameState.put("players", currentPlayers);

        List<Map<String, Object>> currentBlocksState = new ArrayList<>();
        blocks.forEach(block -> {
            Map<String, Object> blockMap = new HashMap<>();
            blockMap.put("x", block.x);
            blockMap.put("y", block.y);
            blockMap.put("width", block.width);
            blockMap.put("height", block.height);
            if (block instanceof ItemBlockState) {
                blockMap.put("type", "item");
                blockMap.put("isHit", ((ItemBlockState) block).isHit);
            } else {
                blockMap.put("type", "normal");
            }
            currentBlocksState.add(blockMap);
        });
        gameState.put("blocks", currentBlocksState);

        List<Map<String, Object>> currentMushroomsState = new ArrayList<>();
        mushrooms.forEach(mushroom -> {
            Map<String, Object> mushroomMap = new HashMap<>();
            mushroomMap.put("x", mushroom.x);
            mushroomMap.put("y", mushroom.y);
            mushroomMap.put("width", mushroom.width);
            mushroomMap.put("height", mushroom.height);
            mushroomMap.put("isVisible", mushroom.isVisible);
            currentMushroomsState.add(mushroomMap);
        });
        gameState.put("mushrooms", currentMushroomsState);

        List<Map<String, Object>> currentGoombasState = new ArrayList<>();
        goombas.forEach(goomba -> {
            Map<String, Object> goombaMap = new HashMap<>();
            goombaMap.put("x", goomba.x);
            goombaMap.put("y", goomba.y);
            goombaMap.put("width", goomba.width);
            goombaMap.put("height", goomba.height);
            goombaMap.put("isAlive", goomba.isAlive);
            currentGoombasState.add(goombaMap);
        });
        gameState.put("goombas", currentGoombasState);

        List<Map<String, Object>> currentFireballsState = new ArrayList<>();
        fireballs.forEach(fireball -> {
            Map<String, Object> fireballMap = new HashMap<>();
            fireballMap.put("x", fireball.x);
            fireballMap.put("y", fireball.y);
            fireballMap.put("width", fireball.width);
            fireballMap.put("height", fireball.height);
            fireballMap.put("isAlive", fireball.isAlive);
            currentFireballsState.add(fireballMap);
        });
        gameState.put("fireballs", currentFireballsState);


        gameState.put("groundLevel", GROUND_LEVEL);

        synchronized (clients) {
            clients.forEach(clientHandler -> {
                try {
                    clientHandler.out.reset(); // 清除已發送過的對象緩存，確保每次都發送最新的狀態
                    clientHandler.out.writeObject(gameState);
                    clientHandler.out.flush();
                } catch (IOException e) {
                    System.err.println("向客戶端 " + clientHandler.playerName + " 發送遊戲狀態失敗: " + e.getMessage());
                    // 如果發送失敗，考慮將此客戶端移除
                    clientHandler.closeClientResources();
                }
            });
        }
    }


    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String playerName;

        public ClientHandler(Socket socket, String playerName) {
            this.clientSocket = socket;
            this.playerName = playerName;
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                out.writeObject(playerName); // 將分配的玩家名稱發送給客戶端
                out.flush();
            } catch (IOException e) {
                System.err.println("為客戶端 " + playerName + " 建立串流失敗: " + e.getMessage());
                closeClientResources();
            }
        }

        @Override
        public void run() {
            try {
                while (clientSocket.isConnected()) {
                    Object clientInput = in.readObject(); 
                    //System.out.println("伺服器收到來自 " + playerName + " 的客戶端輸入: " + clientInput); // <<=== 關鍵調試訊息

                    PlayerState currentPlayerState = playerStates.get(playerName);

                    if (currentPlayerState == null || currentPlayerState.gameOver) {
                        continue; 
                    }

                    if (clientInput instanceof Map) {
                        Map<String, Boolean> keyStates = (Map<String, Boolean>) clientInput;

                        currentPlayerState.movingLeft = keyStates.getOrDefault("MOVE_LEFT", false);
                        currentPlayerState.movingRight = keyStates.getOrDefault("MOVE_RIGHT", false);

                        if (keyStates.getOrDefault("JUMP", false)) {
                            if (currentPlayerState.isOnGround) {
                                currentPlayerState.velocityY = currentPlayerState.initialJumpVelocity; 
                                currentPlayerState.isOnGround = false; 
                            }
                        }
                        
                        if (keyStates.getOrDefault("FIREBALL_REQUESTED", false)) {
                            System.out.println("伺服器：收到火球請求，檢查大瑪利歐狀態... (玩家: " + playerName + ", isbigmario: " + currentPlayerState.isbigmario + ")"); 

                            if (currentPlayerState.isbigmario) { 
                                int fireballSpeed = 10; 
                                int fireballWidth = 20;
                                int fireballHeight = 20;
                                
                                // 火球發射位置調整，使其在玩家前方
                                int fireballX;
                                if (currentPlayerState.movingRight || (!currentPlayerState.movingLeft && !currentPlayerState.movingRight)) { // 靜止或向右，向右發射
                                    fireballX = currentPlayerState.playerX + currentPlayerState.playerWidth;
                                } else { // 向左，向左發射
                                    fireballX = currentPlayerState.playerX - fireballWidth;
                                }

                                int fireballY = currentPlayerState.playerY + currentPlayerState.playerHeight / 2 - fireballHeight / 2;
                                
                                int initialFireballSpeedX = currentPlayerState.movingRight ? fireballSpeed : 
                                                            (currentPlayerState.movingLeft ? -fireballSpeed : fireballSpeed); 
                                // 如果玩家靜止，可以預設方向，或者根據最後移動方向。這裡維持預設向右
                                if (!currentPlayerState.movingLeft && !currentPlayerState.movingRight) {
                                    initialFireballSpeedX = fireballSpeed; 
                                }

                                FireballState newFireball = new FireballState(fireballX, fireballY, fireballWidth, fireballHeight, initialFireballSpeedX, playerName);
                                fireballs.add(newFireball);
                                System.out.println("伺服器：玩家 " + playerName + " 發射火球成功！火球初始位置: (" + fireballX + ", " + fireballY + ") 速度: " + initialFireballSpeedX); 
                            } else {
                                System.out.println("伺服器：玩家 " + playerName + " 嘗試發射火球但不是大瑪利歐。"); 
                            }
                        }
                    } 
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("客戶端 " + playerName + " 連線斷開或讀取錯誤: " + e.getMessage());
            } finally {
                closeClientResources();
                System.out.println("客戶端 " + playerName + " 已斷開連線。");
                
                synchronized (clients) { 
                    clients.remove(this);
                }
                playerStates.remove(this.playerName); 
            }
        }

        private void closeClientResources() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException ex) {
                System.err.println("關閉客戶端資源時發生錯誤: " + ex.getMessage());
            }
        }
    }

    // 遊戲狀態類別 (PlayerState, BlockState, ItemBlockState, MushroomState, GoombaState, FireballState)
    // 保持不變，但為確保完整性，我再次包含它們。
    // ... (以下為 PlayerState, BlockState 等類別定義) ...

    private static class PlayerState implements Serializable {
        public String playerName;
        public int playerX, playerY;
        public int playerWidth, playerHeight;
        public int playerSpeed = 5;
        public float velocityY = 0;
        public float gravity = 0.5f; 
        public float initialJumpVelocity = -12f; 
        public boolean isOnGround = false;
        public boolean movingLeft = false;
        public boolean movingRight = false;
        public boolean isbigmario = false; // 是否是大瑪利歐
        public boolean gameOver = false; 

        public PlayerState(String name, int x, int y) {
            this.playerName = name;
            this.playerX = x;
            this.playerY = y;
            this.playerWidth = 50;
            this.playerHeight = 50;
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

    private static class ItemBlockState extends BlockState {
        public boolean isHit = false; 
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

    private static class FireballState implements Serializable {
        public int x, y, width, height;
        public int speedX; 
        public boolean isAlive;
        public String ownerPlayerName; 

        public FireballState(int x, int y, int width, int height, int speedX, String ownerPlayerName) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speedX = speedX;
            this.isAlive = true;
            this.ownerPlayerName = ownerPlayerName;
        }

        public boolean collidesWith(int otherX, int otherY, int otherWidth, int otherHeight) {
            return x < otherX + otherWidth &&
                   x + width > otherX &&
                   y < otherY + otherHeight &&
                   y + height > otherY;
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