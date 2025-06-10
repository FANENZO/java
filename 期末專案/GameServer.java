import java.awt.Rectangle;
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
    private static final int GROUND_LEVEL = SCREEN_HEIGHT - 85;

    private Map<String, PlayerState> playerStates = new ConcurrentHashMap<>();
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    private List<BlockState> blocks = Collections.synchronizedList(new ArrayList<>());
    private List<MushroomState> mushrooms = Collections.synchronizedList(new ArrayList<>());
    private List<GoombaState> goombas = Collections.synchronizedList(new ArrayList<>());
    private List<FireballState> fireballs = Collections.synchronizedList(new ArrayList<>());

    public GameServer(int port) {
        // 初始化遊戲物件
        blocks.add(new ItemBlockState(410, 380, 50, 50, false));
        for (int i = 0; i < 5; i++) {
            blocks.add(new BlockState(60 + i * 200, 380, 50, 50));
        }
        mushrooms.add(new MushroomState(0, 0, 30, 30, false));
        goombas.add(new GoombaState(600, GROUND_LEVEL - 40, 40, 40, true, -2));
        mushrooms.add(new MushroomState(0, GROUND_LEVEL - 40, 40, 40, true));

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
                        while (playerStates.containsKey("P" + nextPlayerNum)) {
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

                    playerStates.forEach((name, player) -> {
                        if (player.gameOver) return;

                        if (player.movingLeft) {
                            player.playerX -= player.playerSpeed;
                        }
                        if (player.movingRight) {
                            player.playerX += player.playerSpeed;
                        }

                        if (!player.isOnGround) {
                            player.velocityY += player.gravity;
                        }
                        player.playerY += player.velocityY;

                        if (player.playerX < 0) player.playerX = 0;
                        if (player.playerX + player.playerWidth > SCREEN_WIDTH) player.playerX = SCREEN_WIDTH - player.playerWidth;

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
            if (player.gameOver)
                return;

            System.out.printf("START_TICK[%s]: y=%d, vy=%.2f, onGround(in)=%b%n",
                playerName, player.playerY, player.velocityY, player.isOnGround);

            java.util.concurrent.atomic.AtomicBoolean isGroundedThisFrame = new java.util.concurrent.atomic.AtomicBoolean(false);

            if (player.playerY + player.playerHeight >= GROUND_LEVEL) {
                player.playerY = GROUND_LEVEL - player.playerHeight;
                if (player.velocityY > 0) {
                    player.velocityY = 0;
                }
                isGroundedThisFrame.set(true);
            }

            blocks.forEach(block -> {
                Rectangle blockBounds = new Rectangle(block.x, block.y, block.width, block.height);
                Rectangle playerBounds = new Rectangle(player.playerX, player.playerY, player.playerWidth, player.playerHeight);
                Rectangle nextPlayerBoundsV = new Rectangle(player.playerX, (int)(player.playerY + player.velocityY), player.playerWidth, player.playerHeight);

                if (player.velocityY >= 0 && nextPlayerBoundsV.intersects(blockBounds) && player.playerY + player.playerHeight <= block.y + 5) {
                    player.playerY = block.y - player.playerHeight;
                    player.velocityY = 0;
                    isGroundedThisFrame.set(true);
                } else if (player.velocityY < 0 && nextPlayerBoundsV.intersects(blockBounds) && player.playerY >= block.y + block.height - 1) {
                    player.playerY = block.y + block.height;
                    player.velocityY = 0;

                    if (block instanceof ItemBlockState) {
                        ItemBlockState itemBlock = (ItemBlockState) block;
                        if (!itemBlock.isHit) {
                            itemBlock.isHit = true;
                            mushrooms.stream().filter(m -> !m.isVisible).findFirst().ifPresent(mushroom -> {
                                mushroom.x = block.x;
                                mushroom.y = block.y - mushroom.height;
                                mushroom.isVisible = true;
                            });
                        }
                    }
                } else if (playerBounds.intersects(blockBounds)) {
                    if (player.movingRight) {
                        player.playerX = block.x - player.playerWidth;
                    } else if (player.movingLeft) {
                        player.playerX = block.x + block.width;
                    }
                }
            });

            if (Math.abs(player.playerY + player.playerHeight - GROUND_LEVEL) < 1) {
                isGroundedThisFrame.set(true);
            } else {
                for (BlockState block : blocks) {
                    if (Math.abs(player.playerY + player.playerHeight - block.y) < 1 &&
                        player.playerX + player.playerWidth > block.x &&
                        player.playerX < block.x + block.width) {
                        isGroundedThisFrame.set(true);
                        break;
                    }
                }
            }

            player.isOnGround = isGroundedThisFrame.get();

            System.out.printf("END_TICK[%s]:   y=%d, vy=%.2f, onGround(out)=%b%n%n",
                playerName, player.playerY, player.velocityY, player.isOnGround);

            mushrooms.forEach(mushroom -> {
                if (mushroom.isVisible &&
                    new Rectangle(player.playerX, player.playerY, player.playerWidth, player.playerHeight)
                    .intersects(new Rectangle(mushroom.x, mushroom.y, mushroom.width, mushroom.height))) {
                    player.isbigmario = true;
                    mushroom.isVisible = false;
                }
            });

            goombas.forEach(goomba -> {
                if (goomba.isAlive &&
                    new Rectangle(player.playerX, player.playerY, player.playerWidth, player.playerHeight)
                    .intersects(new Rectangle(goomba.x, goomba.y, goomba.width, goomba.height))) {
                    if (player.velocityY > 0 && (player.playerY + player.playerHeight - player.velocityY) <= goomba.y) {
                        goomba.isAlive = false;
                        player.velocityY = -6f;
                    } else {
                        if (player.isbigmario) {
                            player.isbigmario = false;
                        } else {
                            player.gameOver = true;
                        }
                    }
                }
            });

            fireballs.forEach(fireball -> {
                if (!fireball.isAlive) return;
                Rectangle fireballBounds = new Rectangle(fireball.x, fireball.y, fireball.width, fireball.height);

                blocks.forEach(block -> {
                    if (fireballBounds.intersects(new Rectangle(block.x, block.y, block.width, block.height))) {
                        fireball.isAlive = false;
                    }
                });

                goombas.forEach(goomba -> {
                    if (goomba.isAlive && fireballBounds.intersects(new Rectangle(goomba.x, goomba.y, goomba.width, goomba.height))) {
                        fireball.isAlive = false;
                        goomba.isAlive = false;
                    }
                });
            });
        });
    }

    private void broadcastGameState() {
        Map<String, Object> gameState = new HashMap<>();
        
        Map<String, int[]> currentPlayers = new HashMap<>();
        playerStates.forEach((name, player) -> {
            currentPlayers.put(name, new int[]{
                player.playerX,
                player.playerY,
                player.playerWidth,
                player.playerHeight,
                player.isbigmario ? 1 : 0,
                player.gameOver ? 1 : 0,
                player.isOnGround ? 1 : 0,
                (player.movingLeft || player.movingRight) ? 1 : 0 // 傳送 isMoving 狀態
            });
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
            for (ClientHandler clientHandler : new ArrayList<>(clients)) {
                try {
                    clientHandler.out.reset();
                    clientHandler.out.writeObject(gameState);
                    clientHandler.out.flush();
                } catch (IOException e) {
                    System.err.println("向客戶端 " + clientHandler.playerName + " 發送遊戲狀態失敗: " + e.getMessage());
                    clientHandler.closeClientResources();
                    clients.remove(clientHandler);
                    playerStates.remove(clientHandler.playerName);
                }
            }
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
                out.writeObject(playerName);
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

                    PlayerState currentPlayerState = playerStates.get(playerName);

                    if (currentPlayerState == null || currentPlayerState.gameOver) {
                        continue;
                    }

                    if (clientInput instanceof Map) {
                        Map<String, Boolean> keyStates = (Map<String, Boolean>) clientInput;

                        currentPlayerState.movingLeft = keyStates.getOrDefault("MOVE_LEFT", false);
                        currentPlayerState.movingRight = keyStates.getOrDefault("MOVE_RIGHT", false);

                        boolean isJumping = keyStates.getOrDefault("JUMP", false);

                        if (isJumping && currentPlayerState.isOnGround && currentPlayerState.canJump) {
                            currentPlayerState.velocityY = currentPlayerState.initialJumpVelocity;
                            currentPlayerState.isOnGround = false;
                            currentPlayerState.canJump = false;
                        }

                        if (!isJumping) {
                            currentPlayerState.canJump = true;
                        }

                        if (keyStates.getOrDefault("FIREBALL_REQUESTED", false) && currentPlayerState.isbigmario) {
                            int fireballSpeed = 10;
                            int fireballWidth = 20;
                            int fireballHeight = 20;
                            
                            int fireballX;
                            int initialFireballSpeedX;

                            if (currentPlayerState.movingRight || (!currentPlayerState.movingLeft && !currentPlayerState.movingRight)) {
                                fireballX = currentPlayerState.playerX + currentPlayerState.playerWidth;
                                initialFireballSpeedX = fireballSpeed;
                            } else {
                                fireballX = currentPlayerState.playerX - fireballWidth;
                                initialFireballSpeedX = -fireballSpeed;
                            }

                            int fireballY = currentPlayerState.playerY + currentPlayerState.playerHeight / 2 - fireballHeight / 2;
                            
                            FireballState newFireball = new FireballState(fireballX, fireballY, fireballWidth, fireballHeight, initialFireballSpeedX, playerName);
                            fireballs.add(newFireball);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("客戶端 " + playerName + " 連線斷開或讀取錯誤: " + e.getMessage());
            } finally {
                closeClientResources();
                System.out.println("客戶端 " + playerName + " 已斷開連線。");
                clients.remove(this);
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
        public boolean isbigmario = false;
        public boolean gameOver = false;
        public boolean canJump = true;

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