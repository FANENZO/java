import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

// public class MarioLikeGame2copy extends JPanel implements KeyListener, Runnable {
//     // 主角屬性
//     private int playerX = 50, playerY; // 主角初始位置
//     private int playerWidth = 50, playerHeight = 50;
//     private int playerSpeed = 5;
//     private Image playerImage,bigplayerImage; // 角色圖片
//     private Image playerjump,bigplayerjump;
//     private Image bigplayerrun1,bigplayerrun2,bigplayerrun3;
//     private ImageIcon playerGif,bigplayerGif; // 角色 GIF
//     private boolean isMoving = false; // 判斷角色是否在移動
//     private boolean jump = false;
//     private int health = 3; // 角色的初始血量
//     private boolean gameOver = false; // 是否顯示Game Over
//     private boolean isbigmario = false;

//     // 背景屬性
//     private Image[] backgrounds = new Image[4];
//     private Image gameover;
//     private int currentBackgroundIndex = 0; // 當前背景索引
//     private int screenWidth = 800; // 視窗寬度
//     private int screenHeight = 600; // 視窗高度
//     private int groundHeight = 120; // 地面高度
//     private int groundLevel; // 地面頂部位置
//     private int currentImageIndex = 0;

//     // 移動邏輯
//     private boolean isJumping = false;
//     private boolean isFalling = false;
//     private boolean movingLeft = false;
//     private boolean movingRight = false;

//     private int jumpHeight = 160; // 跳躍高度
//     private int jumpSpeed = 5; // 跳躍速度

//     // 磚塊與道具方塊屬性
//     private int blockX, blockY; // 磚塊位置
//     private int blockWidth = 50, blockHeight = 50; // 磚塊大小
//     private int itemBlockX, itemBlockY; // 道具磚塊位置
//     private boolean mushroomVisible = false; // 蘑菇是否可見
//     private int mushroomX, mushroomY;
//     private Image normalBlockImage, itemBlockImage; // 道具磚塊圖片
    
//     //蘑菇
//     private Image mushrooImage;
//     private int mushroomSpeed = 2; // 蘑菇移動速度
//     private boolean mushroomMovingLeft = true; // 蘑菇初始往左移動
//     private boolean mushroomFalling = false; // 蘑菇是否正在下落
//     private int mushroomFallSpeed = 5; // 蘑菇的墜落速度

//     //栗寶寶
//     private Image goombaImage;
//     private ImageIcon goombaGif;
//     private int[] goombaX, goombaY;
//     private int goombaWidth = 50, goombaHeight = 50; // 栗寶寶大小
//     private int goombaSpeed = 2; //栗寶寶移動速度
//     private int goombaFallSpeed = 5; // 栗寶寶的墜落速度
//     private boolean[] goombaMovingLeft, goombaVisible, goombaFalling;
//     private int numGoombas; // 每個場景的栗寶寶數量

//     //終點
//     private int flagX = 600;
//     private int flagY = 80;
//     private int flagHight = 400;
//     private int flagWidth = 80;
//     private Image flag;


    // public MarioLikeGame2copy() {
    //     setFocusable(true);
    //     addKeyListener(this);

    //     // 設定地面頂部位置
    //     groundLevel = screenHeight - groundHeight; // 地面位於畫布底部以上 120px
    //     playerY = groundLevel - playerHeight; // 將角色初始化在地面上

    //     // 載入背景圖片
    //     backgrounds[0] = new ImageIcon("images/background.png").getImage();
    //     backgrounds[1] = new ImageIcon("images/background2.png").getImage();
    //     backgrounds[2] = new ImageIcon("images/background.png").getImage();
    //     backgrounds[3] = new ImageIcon("images/background2.png").getImage();
    //     flag = new ImageIcon("images\\flag.png").getImage();
    //     gameover = new ImageIcon("images\\gameover-removebg-preview.png").getImage();

    //     // 角色圖片
    //     playerImage = new ImageIcon("images/mario1.png").getImage();
    //     playerGif = new ImageIcon("images/output-onlinegiftools.gif");
    //     playerjump = new ImageIcon("images/mariojmup.png").getImage();
    //     bigplayerImage = new ImageIcon("images\\bigmario1-removebg-preview.png").getImage();
    //     bigplayerjump = new ImageIcon("images\\bigmariojump-removebg-preview.png").getImage();
    //     bigplayerGif = new ImageIcon("images\\bigmariorun.gif");

    //     // 磚塊與道具磚塊初始化
    //     initializeBlocks();
    //     goombaImage = new ImageIcon("images\\8-bit-goomba-removebg-preview.png").getImage();
    //     goombaGif = new ImageIcon("images/output-goomba.gif");
    //     mushrooImage = new ImageIcon("images\\mushroom.png").getImage();
    //     normalBlockImage = new ImageIcon("images\\Brick.png").getImage();
    //     itemBlockImage = new ImageIcon("images\\Bricks1.png").getImage();
    //     initializeGoombas();
    //     new Thread(this).start(); // 啟動遊戲循環
    // }

    private void initializeBlocks() {
        if (currentBackgroundIndex == 0) {
            blockX = screenWidth / 3 - blockWidth / 2;
            blockY = groundLevel - playerHeight - blockHeight - 50;
            itemBlockX = screenWidth * 2 / 3 - blockWidth / 2;
            itemBlockY = groundLevel - playerHeight - blockHeight - 50;
        } else if (currentBackgroundIndex == 1) {  // 场景二
            blockX = screenWidth / 4 - blockWidth / 2; // 修改磚塊位置
            blockY = groundLevel - playerHeight - blockHeight - 100; // 修改磚塊位置
            itemBlockX = screenWidth * 3 / 4 - blockWidth / 2;
            itemBlockY = groundLevel - playerHeight - blockHeight - 100;
            // 你可以再加入更多的磚塊或道具磚塊
            } else if (currentBackgroundIndex == 2) {  // 场景三
                blockX = screenWidth / 4 - blockWidth / 2; // 修改磚塊位置
                blockY = groundLevel - playerHeight - blockHeight - 100; // 修改磚塊位置
                itemBlockX = screenWidth * 3 / 4 - blockWidth / 2;
                itemBlockY = groundLevel - playerHeight - blockHeight - 100;
                // 你可以再加入更多的磚塊或道具磚塊
                
            } else {
            blockX = -100; // 隱藏磚塊
            itemBlockX = -100; // 隱藏道具磚塊
            mushroomVisible = false; // 隱藏蘑菇
        }
    }

    private void initializeGoombas() {
        if (currentBackgroundIndex == 0) {
            numGoombas = 2; // 場景 1 有 2 個栗寶寶
            goombaX = new int[]{600, 300};
            goombaY = new int[]{groundLevel - goombaHeight, groundLevel - goombaHeight};
        } else if (currentBackgroundIndex == 1) {
            numGoombas = 3; // 場景 2 有 3 個栗寶寶
            goombaX = new int[]{200, 400, 650};
            goombaY = new int[]{groundLevel - goombaHeight, groundLevel - goombaHeight - 50, groundLevel - goombaHeight -200};
        } else {
            numGoombas = 1; // 其他場景 1 個栗寶寶
            goombaX = new int[]{500};
            goombaY = new int[]{groundLevel - goombaHeight};
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
            boolean collision = playerX + playerWidth > flagX && playerX < flagX + flagWidth &&
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
    
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gameOver) {
            // 當遊戲結束時，顯示黑色背景
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, screenWidth, screenHeight);
            currentBackgroundIndex = 4;
        }
        // 繪製背景
        else {
            g.drawImage(backgrounds[currentBackgroundIndex], 0, 0, screenWidth, getHeight(), null);
        }

        // 繪製終點
        if (currentBackgroundIndex == 2) {
            g.drawImage(flag, flagX, flagY, flagWidth, flagHight,null);
        }

        // 繪製磚塊 (咖啡色)
        // if (currentBackgroundIndex == 0) {
        //     g.drawImage(normalBlockImage, blockX, blockY, blockWidth, blockHeight,null);
        } else if (currentBackgroundIndex == 1) {
            g.drawImage(normalBlockImage, blockX, blockY+50, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, blockX +150, blockY, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, blockX +200, blockY, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, itemBlockX-50, blockY, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, itemBlockX+50, blockY, blockWidth, blockHeight, null);
            g.drawImage(itemBlockImage, itemBlockX, itemBlockY, blockWidth, blockHeight, null);
        } else if (currentBackgroundIndex == 2) {
            // g.drawImage(normalBlockImage, screenWidth / 4 - blockWidth / 2-50, groundLevel - playerHeight - blockHeight - 100, blockWidth, blockHeight, null);
            // g.drawImage(normalBlockImage, blockX, blockY, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, blockX +150, blockY -50, blockWidth, blockHeight, null);
            g.drawImage(normalBlockImage, blockX +200, blockY -50, blockWidth, blockHeight, null);
        }
        if (gameOver == true) {
            g.drawImage(gameover, blockX + 100, blockY -200, 300, 150, null);
        }

        // 繪製道具磚塊
        if (currentBackgroundIndex == 0) {
            g.drawImage(itemBlockImage, itemBlockX, itemBlockY, blockWidth, blockHeight, null);
        }

        //繪製栗寶寶
        if (currentBackgroundIndex != -1) {
            // 繪製栗寶寶，如果栗寶寶可見
            for (int i = 0; i < numGoombas; i++) {
                if (goombaVisible[i] && !gameOver) {
                    g.drawImage(goombaImage, goombaX[i], goombaY[i], goombaWidth, goombaHeight, null);
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

    private void moveToPosition(int targetX, int targetY, int duration) {
        int frames = duration / 16; // 計算動畫的總幀數，假設約 60fps（每幀16ms）
        int deltaY = (targetY - playerY) / frames; // 每幀移動的距離
    
        for (int i = 0; i < frames; i++) {
            playerY += deltaY; // 更新馬力歐的Y座標
            repaint(); // 重繪畫面
            try {
                Thread.sleep(16); // 等待 16ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 確保最後的位置精確
        playerY = targetY;
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void changeBackground(int direction) {
        if (direction > 0 && currentBackgroundIndex < backgrounds.length - 1) {
            currentBackgroundIndex++;
            playerX = 0; // 回到左邊起點
        } else if (direction < 0 && currentBackgroundIndex > 0) {
            currentBackgroundIndex--;
            playerX = screenWidth - playerWidth; // 回到右邊起點
        }
        initializeBlocks(); // 更新磚塊配置
        initializeGoombas(); // 更新栗寶寶配置
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(16); // 遊戲循環 (約60FPS)
            } catch (InterruptedException e) {
                e.printStackTrace();
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

            // 生成蘑菇邏輯-->改到從道具磚塊底部碰撞後生成
            // if (!mushroomVisible && playerY + playerHeight <= itemBlockY &&
            //         playerX + playerWidth > itemBlockX && playerX < itemBlockX + blockWidth) {
            //     mushroomVisible = true;
            //     mushroomX = itemBlockX;
            //     mushroomY = itemBlockY - blockHeight; // 蘑菇出現在道具磚塊上方
            // }
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
                            goombaX[i] + goombaWidth > blockX && goombaX[i] < blockX + blockWidth) {
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
            

            if (mushroomVisible) { //蘑菇出現後
                if (playerX + playerWidth > mushroomX && playerX < mushroomX + blockWidth &&
                playerY + playerHeight > mushroomY && playerY < mushroomY + blockHeight) {
                // 當馬力歐碰到蘑菇，將蘑菇設為不可見
                mushroomVisible = false;
                isbigmario = true;
                }
                if (mushroomFalling) {
                    mushroomY += mushroomFallSpeed; // 蘑菇下墜

                    // 偵測蘑菇是否落到地面
                    if (mushroomY + blockHeight >= groundLevel) {
                        mushroomY = groundLevel - blockHeight; // 停在地面上
                        mushroomFalling = false; // 停止下落
                    }
                    // 偵測蘑菇是否落到其他支撐物上
                    else if (mushroomY + blockHeight >= blockY &&
                            mushroomX + blockWidth > blockX && mushroomX < blockX + blockWidth) {
                        mushroomY = blockY - blockHeight; // 停在磚塊上
                        mushroomFalling = false; // 停止下落
                    } else if (mushroomY + blockHeight >= itemBlockY &&
                            mushroomX + blockWidth > itemBlockX && mushroomX < itemBlockX + blockWidth) {
                        mushroomY = itemBlockY - blockHeight; // 停在道具磚塊上
                        mushroomFalling = false; // 停止下落
                    }
                } else {
                    // 檢查是否需要讓蘑菇開始下落
                    if (!isMushroomOnBlock() && mushroomY + blockHeight < groundLevel) {
                        mushroomFalling = true;
                    }
                }
                // 更新蘑菇的座標
                if (mushroomMovingLeft) {
                    mushroomX -= mushroomSpeed;
                    // 如果蘑菇碰到左邊界，改變方向
                    if (mushroomX <= 0) {
                        mushroomMovingLeft = false;
                    }
                } else {
                    mushroomX += mushroomSpeed;
                    // 如果蘑菇碰到右邊界，改變方向
                    if (mushroomX + blockWidth >= screenWidth) {
                        mushroomMovingLeft = true;
                    }
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
        else if (!gameOver &&
                playerX + playerWidth > goombaX[i] && playerX < goombaX[i] + goombaWidth &&
                playerY + playerHeight > goombaY[i] && playerY < goombaY[i] + goombaHeight) {
            // 如果角色接觸到栗寶寶的側面
            if (playerY + playerHeight > goombaY[i] + 10 && playerY < goombaY[i] + goombaHeight - 10) {
                // 側面碰撞，扣血
                // health--;

                // 如果血量歸零，遊戲結束
                if (health <= 0) {
                    gameOver = true;
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

            repaint();
        }
    }

    private boolean isGoombaOnBlock(int i) {
        return (goombaY[i] + goombaHeight == blockY &&
                goombaX[i] + goombaWidth > blockX && goombaX[i] < blockX + blockWidth);
    }
    
    private boolean isMushroomOnBlock() { // 檢查蘑菇是否站在支撐物上
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
    }

    private void checkCollisionWithBlocks() { //偵測碰撞
        // 偵測與普通磚塊的碰撞
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

        // 偵測與道具磚塊的碰撞
        if (playerX + playerWidth > itemBlockX && playerX < itemBlockX + blockWidth &&
                playerY + playerHeight > itemBlockY && playerY < itemBlockY + blockHeight
                && currentBackgroundIndex != 2 && currentBackgroundIndex != 4) {

            // 頂部碰撞
            if (playerY + playerHeight > itemBlockY && playerY + playerHeight - jumpSpeed <= itemBlockY) {
                playerY = itemBlockY - playerHeight; // 停在道具磚塊上方
                isFalling = false;
                jump = false;
            }

            // 底部碰撞
            else if (playerY < itemBlockY + blockHeight && playerY + jumpSpeed >= itemBlockY + blockHeight) {
                isJumping = false;
                isFalling = true;
                // 如果蘑菇尚未出現，讓蘑菇出現
                if (!mushroomVisible) {
                    mushroomVisible = true;
                    mushroomX = itemBlockX;
                    mushroomY = itemBlockY - blockHeight; // 蘑菇出現在道具磚塊上方
                }
            }

            // 左側碰撞
            else if (playerX + playerWidth > itemBlockX && playerX < itemBlockX + blockWidth / 2) {
                playerX = itemBlockX - playerWidth; // 阻止角色穿過道具磚塊左側
            }

            // 右側碰撞
            else if (playerX < itemBlockX + blockWidth && playerX + playerWidth > itemBlockX + blockWidth / 2) {
                playerX = itemBlockX + blockWidth; // 阻止角色穿過道具磚塊右側
            }
        }
    }

    private boolean isStandingOnBlock() { //站在地面或磚塊上
        if (isJumping || isFalling) {
            return false; // 若角色正在跳躍或下落，不進行支撐物判斷
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
