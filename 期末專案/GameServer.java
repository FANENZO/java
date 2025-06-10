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
            blocks.add(new BlockState(50 + i * 100, GROUND_LEVEL - 130, 50, 50));
        }

        mushrooms.add(new MushroomState(0, 0, 30, 30, false));

        goombas.add(new GoombaState(600, GROUND_LEVEL - 40, 40, 40, true, -2));


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
                        if (player.isbigmario && player.playerWidth == 50) {
                            player.playerWidth = 60;
                            player.playerHeight = 60;
                            player.playerY -= 10; 
                        } else if (!player.isbigmario && player.playerWidth == 60) {
                            player.playerWidth = 50;
                            player.playerHeight = 50;
                            player.playerY += 10; 
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
                            if (goomba.x < 0 || goomba.x + goomba.width > SCREEN_WIDTH) {
                                goomba.speed *= -1; 
                            }
                            goomba.y = GROUND_LEVEL - goomba.height; 
                        }
                    });

                    fireballs.removeIf(fireball -> {
                        if (!fireball.isAlive) return true; 
                        
                        fireball.x += fireball.speedX; 

                        if (fireball.x < -fireball.width || fireball.x > SCREEN_WIDTH) {
                            System.out.println("伺服器：火球出界，移除。");
                            return true; 
                        }
                        return false; 
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
                if (player.collidesWith(block.x, block.y, block.width, block.height)) {
                    if (player.velocityY >= 0 && 
                        player.playerY + player.playerHeight - player.velocityY <= block.y && 
                        player.playerX < block.x + block.width && player.playerX + player.playerWidth > block.x) {

                        player.playerY = block.y - player.playerHeight; 
                        player.velocityY = 0; 
                        player.isOnGround = true; 
                    }
                    else if (player.velocityY < 0 && 
                             player.playerY - player.velocityY >= block.y + block.height && 
                             player.playerX < block.x + block.width && player.playerX + player.playerWidth > block.x) {

                        player.playerY = block.y + block.height; 
                        player.velocityY = 0; 
                        player.isOnGround = false; 

                        if (block instanceof ItemBlockState) {
                            ItemBlockState itemBlock = (ItemBlockState) block;
                            if (!itemBlock.isHit) {
                                itemBlock.isHit = true;
                                System.out.println("伺服器：玩家 " + playerName + " 擊中道具磚塊。");
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
                    }
                    else if (player.playerY < block.y + block.height && player.playerY + player.playerHeight > block.y) {
                        if (player.playerX + player.playerWidth > block.x && player.playerX < block.x) {
                            player.playerX = block.x - player.playerWidth;
                        }
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
                    if (player.velocityY >= 0 && 
                        player.playerY + player.playerHeight - player.velocityY <= goomba.y && player.playerY + player.playerHeight > goomba.y) {
                        goomba.isAlive = false;
                        System.out.println("伺服器：玩家 " + playerName + " 踩死 Goomba。");
                        player.velocityY = player.initialJumpVelocity / 2; 
                        player.isOnGround = false;
                    } else { 
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

        // 火球與磚塊的碰撞
        fireballs.forEach(fireball -> {
            if (!fireball.isAlive) return;

            blocks.forEach(block -> {
                if (fireball.collidesWith(block.x, block.y, block.width, block.height)) {
                    fireball.isAlive = false;
                    System.out.println("伺服器：火球擊中磚塊，消失。");
                }
            });
        });

        // 火球與 Goomba 的碰撞
        fireballs.forEach(fireball -> {
            if (!fireball.isAlive) return; 

            goombas.forEach(goomba -> {
                if (goomba.isAlive && fireball.collidesWith(goomba.x, goomba.y, goomba.width, goomba.height)) {
                    goomba.isAlive = false; 
                    fireball.isAlive = false; 
                    System.out.println("伺服器：火球擊敗 Goomba！");
                }
            });
        });
    }

    private void broadcastGameState() {
        Map<String, Object> fullGameState = new HashMap<>();

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

        List<Map<String, Object>> fireballData = new ArrayList<>();
        fireballs.forEach(fireball -> {
            if (fireball.isAlive) { 
                Map<String, Object> f = new HashMap<>();
                f.put("x", fireball.x);
                f.put("y", fireball.y);
                f.put("width", fireball.width);
                f.put("height", fireball.height);
                f.put("isAlive", fireball.isAlive);
                f.put("owner", fireball.ownerPlayerName);
                fireballData.add(f);
            }
        });
        fullGameState.put("fireballs", fireballData);

        fullGameState.put("groundLevel", GROUND_LEVEL);

        synchronized (clients) { 
            for (ClientHandler client : clients) {
                try {
                    if (client.out != null) {
                        client.sendGameState(fullGameState);
                    }
                } catch (IOException e) {
                    System.err.println("向客戶端 " + client.playerName + " 發送狀態時發生錯誤: " + e.getMessage());
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
                out.writeObject(playerName); 
                out.flush();
            } catch (IOException e) {
                System.err.println("客戶端 " + playerName + " 串流初始化失敗: " + e.getMessage());
                closeClientResources(); 
            }
        }

        public void sendGameState(Map<String, Object> gameState) throws IOException {
            if (out != null) {
                out.reset(); 
                out.writeObject(gameState);
                out.flush();
            }
        }

        @Override
        public void run() {
            try {
                while (clientSocket.isConnected()) {
                    Object clientInput = in.readObject(); 
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
                            if (currentPlayerState.isbigmario) { 
                                int fireballSpeed = 10; 
                                int fireballWidth = 20;
                                int fireballHeight = 20;
                                int fireballX = currentPlayerState.playerX;
                                int fireballY = currentPlayerState.playerY + currentPlayerState.playerHeight / 2 - fireballHeight / 2;
                                
                                int initialFireballSpeedX = currentPlayerState.movingRight ? fireballSpeed : 
                                                            (currentPlayerState.movingLeft ? -fireballSpeed : fireballSpeed); 
                                if (!currentPlayerState.movingLeft && !currentPlayerState.movingRight) {
                                    // 默認向右發射，如果玩家靜止且不是向左移動
                                    // 為了更精確的方向，可能需要儲存玩家最後的移動方向
                                    // 這裡簡化為如果沒有明確的左右移動，就假定向右發射
                                    initialFireballSpeedX = fireballSpeed; 
                                    // 如果玩家從來沒有移動過或靜止很久，這個邏輯會導致問題，
                                    // 通常會儲存一個 `lastDirection` 變數來解決
                                }


                                FireballState newFireball = new FireballState(fireballX, fireballY, fireballWidth, fireballHeight, initialFireballSpeedX, playerName);
                                fireballs.add(newFireball);
                                System.out.println("伺服器：玩家 " + playerName + " 發射火球。");
                            } else {
                                System.out.println("伺服器：玩家 " + playerName + " 嘗試發射火球但不是大瑪利歐。");
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
            } catch (IOException e) {
                System.err.println("關閉客戶端資源時發生錯誤: " + e.getMessage());
            }
        }
    }

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
            if (this.isbigmario) {
                this.playerWidth = 60;
                this.playerHeight = 60;
            } else {
                this.playerWidth = 50;
                this.playerHeight = 50;
            }
        }

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