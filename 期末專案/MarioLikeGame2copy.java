import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MarioLikeGame2copy extends JPanel implements KeyListener, Runnable {
    // 主角屬性
    private int playerX = 50, playerY; // 主角初始位置
    private int playerWidth = 50, playerHeight = 50;
    private int playerSpeed = 5;
    private Image playerImage,bigplayerImage; // 角色圖片
    private Image playerjump,bigplayerjump;
    private Image bigplayerrun1,bigplayerrun2,bigplayerrun3;
    private ImageIcon playerGif,bigplayerGif; // 角色 GIF
    private boolean isMoving = false; // 判斷角色是否在移動
    private boolean jump = false;
    private boolean gameOver = false; // 是否顯示Game Over
    private boolean isbigmario = false;

    // 背景屬性
    private Image[] backgrounds = new Image[4];
    private Image gameover;
    private int currentBackgroundIndex = 0; // 當前背景索引
    private int screenWidth = 800; // 視窗寬度
    private int screenHeight = 600; // 視窗高度
    private int groundHeight = 120; // 地面高度
    private int groundLevel; // 地面頂部位置
    private int currentImageIndex = 0;

    // 移動邏輯
    private boolean isJumping = false;
    private boolean isFalling = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private int jumpHeight = 160; // 跳躍高度
    private int jumpSpeed = 5; // 跳躍速度

    // 磚塊與道具方塊屬性
    private int blockX, blockY; // 磚塊位置
    private int blockWidth = 50, blockHeight = 50; // 磚塊大小
    private int itemBlockX, itemBlockY; // 道具磚塊位置
    //private boolean itemused = false;
    private boolean mushroomVisible = false; // 蘑菇是否可見
    private int mushroomX, mushroomY;
    private Image normalBlockImage, itemBlockImage; // 道具磚塊圖片
    // 場景二方塊位置和尺寸
    private int block21X, block21Y, block22X, block23X, block24X, block25X, block26X;
    private int block31X,block31Y, block32X, block33X, block34X,block3Y;
    
    //蘑菇
    private Image mushrooImage;
    private int mushroomSpeed = 2; // 蘑菇移動速度
    private boolean mushroomMovingLeft = true; // 蘑菇初始往左移動
    private boolean mushroomFalling = false; // 蘑菇是否正在下落
    private int mushroomFallSpeed = 5; // 蘑菇的墜落速度

    //栗寶寶
    
    private ImageIcon goombaGif;
    private int[] goombaX, goombaY;
    private int goombaWidth = 50, goombaHeight = 50; // 栗寶寶大小
    private int goombaSpeed = 2; //栗寶寶移動速度
    private int goombaFallSpeed = 5; // 栗寶寶的墜落速度
    private boolean[] goombaMovingLeft, goombaVisible, goombaFalling;
    private int numGoombas; // 每個場景的栗寶寶數量
    private boolean isCollisionCooldown = false; // 碰撞冷卻狀態
    private int collisionCooldownTime = 180; // 冷卻持續時間（單位：遊戲迴圈數）
    private int collisionCooldownCounter = 0; // 冷卻計數器

    //終點
    private int flagX = 600;
    private int flagY = 80;
    private int flagHight = 400;
    private int flagWidth = 80;
    private Image flag;


    public MarioLikeGame2copy() {
        setFocusable(true);
        addKeyListener(this);

        // 設定地面頂部位置
        groundLevel = screenHeight - groundHeight; // 地面位於畫布底部以上 120px
        playerY = groundLevel - playerHeight; // 將角色初始化在地面上

        // 載入背景圖片
        backgrounds[0] = new ImageIcon("images/background.png").getImage();
        backgrounds[1] = new ImageIcon("images/background2.png").getImage();
        backgrounds[2] = new ImageIcon("images/background.png").getImage();
        backgrounds[3] = new ImageIcon("images/background2.png").getImage();
        flag = new ImageIcon("images\\flag.png").getImage();
        gameover = new ImageIcon("images\\gameover-removebg-preview.png").getImage();

        // 角色圖片
        //playerdown = new ImageIcon("images/down.png").getImage();
        playerImage = new ImageIcon("images/mario1.png").getImage();
        playerGif = new ImageIcon("images/output-onlinegiftools.gif");
        playerjump = new ImageIcon("images/mariojmup.png").getImage();
        bigplayerImage = new ImageIcon("images\\bigmario1-removebg-preview.png").getImage();
        bigplayerjump = new ImageIcon("images\\bigmariojump-removebg-preview.png").getImage();
        bigplayerGif = new ImageIcon("images\\bigmariorun.gif");

        // 磚塊與道具磚塊初始化
        initializeBlocks();
        goombaGif = new ImageIcon("images/output-goomba.gif");
        mushrooImage = new ImageIcon("images\\mushroom.png").getImage();
        normalBlockImage = new ImageIcon("images\\Brick.png").getImage();
        itemBlockImage = new ImageIcon("images\\Bricks1.png").getImage();
        initializeGoombas();
        new Thread(this).start(); // 啟動遊戲循環
    }
    

    private void initializeBlocks() {
        switch (currentBackgroundIndex) {
            case 0 -> {
                blockX = screenWidth / 3 - blockWidth / 2;
                blockY = groundLevel - playerHeight - blockHeight - 50;
                itemBlockX = screenWidth * 2 / 3 - blockWidth / 2;
                itemBlockY = groundLevel - playerHeight - blockHeight - 50;
            }
            case 1 -> {
                // 场景二
                block21X = screenWidth / 4 - blockWidth / 2; // 修改磚塊位置
                block21Y = groundLevel - playerHeight - blockHeight - 100; // 修改磚塊位置
                block22X = screenWidth / 4 - blockWidth / 2 + 150;
                block23X = screenWidth / 4 - blockWidth / 2 + 200;
                block24X = screenWidth * 3 / 4 - blockWidth / 2 - 50;
                block25X = screenWidth * 3 / 4 - blockWidth / 2 + 50;
                // block26X = 
                itemBlockX = screenWidth * 3 / 4 - blockWidth / 2;
                itemBlockY = groundLevel - playerHeight - blockHeight - 100;
                // 你可以再加入更多的磚塊或道具磚塊
            }
            case 2 -> {
                // 场景三
                block31X = screenWidth / 4 - blockWidth / 2-50; // 修改磚塊位置
                block31Y = groundLevel - playerHeight - blockHeight - 100; // 修改磚塊位置
                block32X = screenWidth / 4 - blockWidth / 2;
                block33X = screenWidth / 4 - blockWidth / 2 + 150;
                block34X = screenWidth / 4 - blockWidth / 2 + 200;
                block3Y = groundLevel - playerHeight - blockHeight - 100 - 50;
                itemBlockX = screenWidth * 3 / 4 - blockWidth / 2;
                itemBlockY = groundLevel - playerHeight - blockHeight - 100;
                // 你可以再加入更多的磚塊或道具磚塊
            }
            default -> {
                blockX = -100; // 隱藏磚塊
                itemBlockX = -100; // 隱藏道具磚塊
                mushroomVisible = false; // 隱藏蘑菇
            }
        }
    }

    private void initializeGoombas() {
        if (currentBackgroundIndex == 0) {
            numGoombas = 2; // 場景 1 有 2 個栗寶寶
            goombaX = new int[] { 600, 300 };
            goombaY = new int[] { groundLevel - goombaHeight, groundLevel - goombaHeight };
        } else if (currentBackgroundIndex == 1) {
            numGoombas = 3; // 場景 2 有 3 個栗寶寶
            goombaX = new int[] { 100, 400, 650 };
            goombaY = new int[] { groundLevel - goombaHeight, groundLevel - goombaHeight - 50,
                    groundLevel - goombaHeight - 200 };
        } else {
            numGoombas = 1; // 其他場景 1 個栗寶寶d
            goombaX = new int[] { 500 };
            goombaY = new int[] { groundLevel - goombaHeight };
        }
        goombaMovingLeft = new boolean[numGoombas];
        goombaVisible = new boolean[numGoombas];
        goombaFalling = new boolean[numGoombas];

        for (int i = 0; i < numGoombas; i++) {
            goombaMovingLeft[i] = true;
            goombaVisible[i] = true;
            goombaFalling[i] = false;
        }
    }
    
    private boolean hasReachedFlag = false;
    private boolean checkCollisionWithFlag() {
        // 簡單的矩形碰撞檢測
        if (hasReachedFlag) {
            return false; // 已經到達終點，跳過檢測
        }
        if (currentBackgroundIndex == 2) {
            boolean collision = playerX + playerWidth > flagX +50 && playerX < flagX + flagWidth &&
                                playerY + playerHeight > flagY && playerY < flagY + flagHight;
            if (collision) {
                hasReachedFlag = true; // 標記已到達終點
                gameOver = true; // 設置遊戲結束
                repaint(); // 觸發畫面重繪，顯示黑色背景
            }
            return collision;
        }
        return false;
    }

    private void hideAllGoombas() {
    for (int i = 0; i < numGoombas; i++) {
        goombaVisible[i] = false; // 將所有栗寶寶設置為不可見
    }
}
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gameOver) {
            // 當遊戲結束時，顯示黑色背景
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, screenWidth, screenHeight);
            hideAllGoombas();;
            currentBackgroundIndex = 4;
        }
        else {
            // 繪製背景
            g.drawImage(backgrounds[currentBackgroundIndex], 0, 0, screenWidth, getHeight(), null);
        }
        // 繪製終點
        if (currentBackgroundIndex == 2) {
            g.drawImage(flag, flagX, flagY, flagWidth, flagHight,null);
        }
        

        // 繪製磚塊
        if (currentBackgroundIndex == 0) {
            g.drawImage(normalBlockImage, blockX, blockY, blockWidth, blockHeight, null);
            g.drawImage(itemBlockImage, itemBlockX, itemBlockY, blockWidth, blockHeight, null);
        } else if (currentBackgroundIndex == 1) {
            g.drawImage(normalBlockImage, block21X, block21Y, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, block22X, blockY, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, block24X, blockY, blockWidth, blockHeight, null);
            g.drawImage(itemBlockImage, itemBlockX, itemBlockY, blockWidth, blockHeight, null);
        } else if (currentBackgroundIndex == 2) {
            g.drawImage(normalBlockImage, block32X, block31Y, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, block31X, block31Y, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, block33X, block3Y, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, block34X, block3Y, blockWidth, blockHeight, null);
        }
        if (gameOver == true) {
            g.drawImage(gameover, blockX , blockY -200, 300, 150, null);
        }


        //繪製栗寶寶
        if (currentBackgroundIndex != -1) {
            // 繪製栗寶寶，如果栗寶寶可見
            for (int i = 0; i < numGoombas; i++) {
                if (goombaVisible[i]) {
                    g.drawImage(goombaGif.getImage(), goombaX[i], goombaY[i], goombaWidth, goombaHeight, null);
                }
            }

        }

        // 繪製蘑菇
        if (mushroomVisible) {
            g.drawImage(mushrooImage, mushroomX, mushroomY, blockWidth, blockHeight,null);
        }

        // 繪製主角
        if (jump && !isbigmario) {
            g.drawImage(playerjump, playerX, playerY, playerWidth, playerHeight, null);
        } else {
            if (isMoving && !isbigmario) {
                g.drawImage(playerGif.getImage(), playerX, playerY, playerWidth, playerHeight, null);
            } else {
                if (!isbigmario){
                    g.drawImage(playerImage, playerX, playerY, playerWidth, playerHeight, null);
                }
            }
        }
        if (jump && isbigmario) {
            g.drawImage(bigplayerjump, playerX, playerY - 40, playerWidth, playerHeight + 40, null);
        } else {
            if (isMoving && isbigmario) {
                g.drawImage(playerGif.getImage(), playerX, playerY - 40, playerWidth, playerHeight + 40, null);
            } else {
                if (isbigmario){
                    g.drawImage(bigplayerImage, playerX, playerY - 40, playerWidth, playerHeight + 40, null);                }
            }
        }
    }

    // 新增變數，記錄跳躍開始的Y座標
    private int jumpStartY; // 角色跳躍的起始位置

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_A) {
            isMoving = true;
            movingLeft = true; // 左移
        } else if (key == KeyEvent.VK_D) {
            isMoving = true;
            movingRight = true; // 右移
        } else if (key == KeyEvent.VK_W && !isJumping && !isFalling) {
            jump = true;
            isJumping = true; // 跳躍
            jumpStartY = playerY;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_A) {
            isMoving = false;
            movingLeft = false; // 停止左移
        } else if (key == KeyEvent.VK_D) {
            isMoving = false;
            movingRight = false; // 停止右移
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void changeBackground(int direction) {
    if (direction > 0 && currentBackgroundIndex < backgrounds.length - 1) {
        currentBackgroundIndex++;
        mushroomVisible = false;
        playerX = 0; // 回到左邊起點
    } else if (direction < 0 && currentBackgroundIndex > 0) {
        currentBackgroundIndex--;
        mushroomVisible = false;
        playerX = screenWidth - playerWidth; // 回到右邊起點
    }
    initializeBlocks(); // 更新磚塊配置
    initializeGoombas();
    
}

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(16); // 遊戲循環 (約60FPS)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (isCollisionCooldown) {
                collisionCooldownCounter--;
                if (collisionCooldownCounter <= 0) {
                    isCollisionCooldown = false;
                }
            }
            // 移動邏輯
            if (movingLeft) {
                if (currentBackgroundIndex == 0 && playerX <= 0) {
                    playerX = 0; // 場景1左邊界限制
                } else if (playerX > 0) {
                    playerX -= playerSpeed;
                } else if (playerX <= 0) {
                    changeBackground(-1);
                }
            }
            if (movingRight) {
                if (playerX + playerWidth < screenWidth) {
                    playerX += playerSpeed;
                } else {
                    changeBackground(1);
                }
            }

            // 跳躍與下落邏輯
            if (isFalling) {
                if (playerY + playerHeight >= groundLevel) {
                    playerY = groundLevel - playerHeight; // 停在地面上
                    isFalling = false;
                    jump = false;
                } else {
                    playerY += jumpSpeed; // 下落
                }
            } else if (isJumping) {
                playerY -= jumpSpeed; // 跳躍
                if (playerY <= jumpStartY - jumpHeight) {
                    isJumping = false;
                    isFalling = true; // 開始下落
                }
            } else {
                // 檢查角色是否站在支撐物上
                if (!isStandingOnBlock()) {
                    isFalling = true; // 沒有支撐物，開始下落
                }
            }

            // 檢查碰撞邏輯
            checkCollisionWithBlocks();
            checkCollisionWithitemBlocks();

            for (int i = 0; i < numGoombas; i++) { // 對每個栗寶寶進行處理
                if (goombaVisible[i]) { // 確保該栗寶寶是可見的
                    if (goombaFalling[i]) {
                        goombaY[i] += goombaFallSpeed; // 栗寶寶下墜
            
                        // 偵測栗寶寶是否落到地面
                        if (goombaY[i] + goombaHeight >= groundLevel) {
                            goombaY[i] = groundLevel - goombaHeight; // 停在地面上
                            goombaFalling[i] = false; // 停止下落
                        }
                        // 偵測栗寶寶是否落到其他支撐物上
                        else if (goombaY[i] + goombaHeight >= blockY &&
                            goombaX[i] + goombaWidth > block24X && goombaX[i] < block24X + blockWidth) {
                            goombaY[i] = blockY - goombaHeight; // 停在磚塊上
                            goombaFalling[i] = false; // 停止下落
                        } else if (goombaY[i] + goombaHeight >= itemBlockY &&
                            goombaX[i] + goombaWidth > itemBlockX && goombaX[i] < itemBlockX + blockWidth) {
                            goombaY[i] = itemBlockY - goombaHeight; // 停在道具磚塊上
                            goombaFalling[i] = false; // 停止下落
                        }
                    } else {
                        // 檢查是否需要讓栗寶寶開始下落
                        if (!isGoombaOnBlock(i) && goombaY[i] + goombaHeight < groundLevel) {
                            goombaFalling[i] = true;
                        }
                    }
            
                    // 更新栗寶寶的座標
                    if (goombaMovingLeft[i]) {
                        goombaX[i] -= goombaSpeed;
                        // 如果栗寶寶碰到左邊界，改變方向
                        if (goombaX[i] <= 0) {
                            goombaMovingLeft[i] = false;
                        }
                    } else {
                        goombaX[i] += goombaSpeed;
                        // 如果栗寶寶碰到右邊界，改變方向
                        if (goombaX[i] + goombaWidth >= screenWidth) {
                            goombaMovingLeft[i] = true;
                        }
                    }
                }
            }
            

            // 更新蘑菇的座標
            if (mushroomVisible) {
                // 碰撞檢測
                if (playerX + playerWidth > mushroomX && playerX < mushroomX + blockWidth &&
                        playerY + playerHeight > mushroomY && playerY < mushroomY + blockHeight) {
                    mushroomVisible = false; // 馬力歐吃到蘑菇
                    isbigmario = true; // 變大
                }

                // 移動蘑菇
                if (mushroomMovingLeft) {
                    mushroomX -= mushroomSpeed;
                    if (mushroomX <= 0) { // 碰到左邊界
                        mushroomMovingLeft = false;
                    }
                } else {
                    mushroomX += mushroomSpeed;
                    if (mushroomX + blockWidth >= screenWidth) { // 碰到右邊界
                        mushroomMovingLeft = true;
                    }
                }

                // 下落邏輯
                if (mushroomFalling) {
                    mushroomY += mushroomFallSpeed;
                    if (mushroomY + blockHeight >= groundLevel) { // 落地
                        mushroomY = groundLevel - blockHeight;
                        mushroomFalling = false;
                    }
                } else if (!isMushroomOnBlock() && mushroomY + blockHeight < groundLevel) {
                    mushroomFalling = true;
                }
            }
            // 檢查角色與每個栗寶寶的碰撞
            for (int i = 0; i < numGoombas; i++) {
                if (goombaVisible[i]) { // 栗寶寶是可見的時候進行碰撞檢查
                    // 偵測角色是否從上方跳到栗寶寶的頭上
                    if (isFalling &&
                            playerX + playerWidth > goombaX[i] && playerX < goombaX[i] + goombaWidth &&
                            playerY <= goombaY[i] + goombaHeight && playerY + playerHeight >= goombaY[i]) {

                        // 角色碰到栗寶寶的頭部，隱藏栗寶寶
                        goombaVisible[i] = false;

                    }

                    // 偵測角色與栗寶寶的側面碰撞
                    else if (goombaVisible[i] && !isCollisionCooldown){if (!gameOver &&
                            playerX + playerWidth > goombaX[i] && playerX < goombaX[i] + goombaWidth &&
                            playerY + playerHeight > goombaY[i] && playerY < goombaY[i] + goombaHeight) {
                        // 如果角色接觸到栗寶寶的側面
                        if (playerY + playerHeight > goombaY[i] + 10 && playerY < goombaY[i] + goombaHeight - 10) {
                            // 側面碰撞，扣血
                            if (isbigmario) {
                                // 大馬力歐變小
                                isbigmario = false;
                            } else {
                                  // 小馬力歐，遊戲結束
                                  gameOver = true;
                              }
                              // 啟動冷卻機制
                            isCollisionCooldown = true;
                            collisionCooldownCounter = collisionCooldownTime;
                        }
                        }
                    }
                }
            }
            if (checkCollisionWithFlag()) {
                System.out.println("到達終點！");
                // 設置遊戲結束並觸發畫面重繪
                gameOver = true;
                repaint(); // 觸發畫面更新，顯示遊戲結束文字
            }
            // 重繪畫面
            repaint();

        }
    }

    private boolean isGoombaOnBlock(int i) {
        return (goombaY[i] + goombaHeight == blockY &&
                goombaX[i] + goombaWidth > blockX && goombaX[i] < blockX + blockWidth);
    }
    
    private boolean isMushroomOnBlock() { // 檢查蘑菇是否站在支撐物上
        if (currentBackgroundIndex == 1) {
            // 檢查是否站在普通磚塊上
        if (mushroomX + blockWidth > block24X && mushroomX < block24X + blockWidth &&
                mushroomY + blockHeight == blockY) {
            return true;
        }

        // 檢查是否站在道具磚塊上
        if (mushroomX + blockWidth > itemBlockX && mushroomX < itemBlockX + blockWidth &&
                mushroomY + blockHeight == itemBlockY) {
            return true;
        }

        // 檢查是否站在地面上
        if (mushroomY + blockHeight == groundLevel) {
            return true;
        }

        return false; // 沒有支撐物
        }
        else{
        // 檢查是否站在普通磚塊上
        if (mushroomX + blockWidth > blockX && mushroomX < blockX + blockWidth &&
                mushroomY + blockHeight == blockY) {
            return true;
        }

        // 檢查是否站在道具磚塊上
        if (mushroomX + blockWidth > itemBlockX && mushroomX < itemBlockX + blockWidth &&
                mushroomY + blockHeight == itemBlockY) {
            return true;
        }

        // 檢查是否站在地面上
        if (mushroomY + blockHeight == groundLevel) {
            return true;
        }

        return false; // 沒有支撐物
    }}

    private void checkCollisionWithBlocks() {   // 一般方塊
        if (currentBackgroundIndex == 1) { // 第2張場景
            checkCollisionWithBlock(block21X, block21Y); // 2-1
            checkCollisionWithBlock(block22X, blockY); // 2-2
            //checkCollisionWithBlock(block23X, blockY); // 2-3
            checkCollisionWithBlock(block24X, blockY); // 2-4
            //checkCollisionWithBlock(block25X, blockY);//2-5
            checkCollisionWithBlock(itemBlockX, itemBlockY);
        } else { // 其他場景
            if (currentBackgroundIndex == 2) { //3
                checkCollisionWithBlock(block32X, block31Y); //3-1
                checkCollisionWithBlock(block31X, block31Y); //3-2
                checkCollisionWithBlock(block33X, block3Y); //3-3
                checkCollisionWithBlock(block34X, block3Y); //3-4
            }
            else if(currentBackgroundIndex==0){
                checkCollisionWithBlock(blockX, blockY);
                checkCollisionWithBlock(itemBlockX, itemBlockY);
            }
            
        }
    }

    private void checkCollisionWithBlock(int blockX, int blockY) {
        if (isbigmario == true) {
            int adjustedHeight = isbigmario ? playerHeight + 40 : playerHeight; // Big 馬力歐高度增加
            int adjustedY = isbigmario ? playerY - 40 : playerY; // Big 馬力歐的 Y 座標上移
            int tolerance = 5; // 容錯範圍，用於避免浮點數誤差導致的跳動

            if (playerX + playerWidth > blockX && playerX < blockX + blockWidth &&
                    adjustedY + adjustedHeight > blockY && adjustedY < blockY + blockHeight) {

                // 頂部碰撞
                if (Math.abs(adjustedY + adjustedHeight - blockY) <= tolerance) {
                    playerY = blockY - adjustedHeight + 40; // 停在磚塊上方
                    isFalling = false;
                    jump = false; // 結束跳躍
                }
                // 底部碰撞
                else if (Math.abs(adjustedY - (blockY + blockHeight)) <= tolerance) {
                    isJumping = false; // 停止向上移動
                    isFalling = true; // 開始下落
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
        else if (playerX + playerWidth > blockX && playerX < blockX + blockWidth &&
                playerY + playerHeight > blockY && playerY < blockY + blockHeight) {

            // 頂部碰撞
            if (playerY + playerHeight > blockY && playerY + playerHeight - jumpSpeed <= blockY) {
                playerY = blockY - playerHeight; // 停在磚塊上方
                isFalling = false;
                jump = false;
            }

            // 底部碰撞
            else if (playerY < blockY + blockHeight && playerY + jumpSpeed >= blockY + blockHeight) {
                isJumping = false;
                isFalling = true; // 開始下落
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
    private void checkCollisionWithitemBlocks() {   //道具方塊
        if (currentBackgroundIndex == 1) { // 第2張場景

            checkCollisionWithitemBlock(itemBlockX, itemBlockY);
        } else if(currentBackgroundIndex == 0){ // 其他場景
            checkCollisionWithitemBlock(itemBlockX, itemBlockY);
        }
    }

    private void checkCollisionWithitemBlock(int blockX, int blockY) {
        if (playerX + playerWidth > blockX && playerX < blockX + blockWidth &&
                playerY + playerHeight > blockY && playerY < blockY + blockHeight) {

            // 頂部碰撞
            if (playerY + playerHeight > blockY && playerY + playerHeight - jumpSpeed <= blockY) {
                playerY = blockY - playerHeight; // 停在磚塊上方
                isFalling = false;
                jump = false;
            }

            // 底部碰撞
            else if (playerY < blockY + blockHeight && playerY + jumpSpeed >= blockY + blockHeight) {
                
                isJumping = false;
                isFalling = true; // 開始下落
                // 如果蘑菇尚未出現，讓蘑菇出現
                    if (!mushroomVisible ) {
                        mushroomVisible = true;
                        
                        mushroomX = itemBlockX;
                        mushroomY = itemBlockY - blockHeight; // 蘑菇出現在道具磚塊上方
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

    

    public static void main(String[] args) {
        JFrame frame = new JFrame("馬力歐遊戲雛形");
        MarioLikeGame2copy game = new MarioLikeGame2copy();
        frame.add(game);
        frame.setSize(800, 600); // 視窗大小
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
