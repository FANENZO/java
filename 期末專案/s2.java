import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class s2 extends JPanel implements KeyListener, Runnable {
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
    private int health = 3; // 角色的初始血量
    private boolean gameOver = false; // 是否顯示Game Over
    private boolean isbigmario = false;

    // 背景屬性
    private Image[] backgrounds = new Image[4];
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
    private boolean itemused = false;
    private boolean mushroomVisible = false; // 蘑菇是否可見
    private int mushroomX, mushroomY;
    private Image normalBlockImage, itemBlockImage; // 道具磚塊圖片
    // 場景二方塊位置和尺寸
    private int block21X, block21Y, block22X, block22Y;
    
    //蘑菇
    private Image mushrooImage;
    private int mushroomSpeed = 2; // 蘑菇移動速度
    private boolean mushroomMovingLeft = true; // 蘑菇初始往左移動
    private boolean mushroomFalling = false; // 蘑菇是否正在下落
    private int mushroomFallSpeed = 5; // 蘑菇的墜落速度

    //栗寶寶
    private Image goombaImage;
    private ImageIcon goombaGif;
    private int[] goombaX, goombaY;
    private int goombaWidth = 50, goombaHeight = 50; // 栗寶寶大小
    private int goombaSpeed = 2; //栗寶寶移動速度
    private int goombaFallSpeed = 5; // 栗寶寶的墜落速度
    private boolean[] goombaMovingLeft, goombaVisible, goombaFalling;
    private int numGoombas; // 每個場景的栗寶寶數量



    public s2() {
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
        
        new Thread(this).start(); // 啟動遊戲循環
    }
    private void switchToSceneTwo() {
    currentBackgroundIndex = 1;
    initializeBlocks(); // 切換場景時初始化方塊
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
                itemBlockX = screenWidth * 3 / 4 - blockWidth / 2;
                itemBlockY = groundLevel - playerHeight - blockHeight - 100;
                // 你可以再加入更多的磚塊或道具磚塊
            }
            case 2 -> {
                // 场景三
                blockX = screenWidth / 4 - blockWidth / 2; // 修改磚塊位置
                blockY = groundLevel - playerHeight - blockHeight - 100; // 修改磚塊位置
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


    @Override
    protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    // 繪製背景
    g.drawImage(backgrounds[currentBackgroundIndex], 0, 0, screenWidth, getHeight(), null);

    // 繪製磚塊
    if (currentBackgroundIndex == 0) {
        g.drawImage(normalBlockImage, blockX, blockY, blockWidth, blockHeight, null);
    } else if (currentBackgroundIndex == 1) {
        g.drawImage(normalBlockImage, block21X, block21Y, blockWidth, blockHeight, null);
    } else if (currentBackgroundIndex == 2) {
        g.drawImage(normalBlockImage, blockX - 50, blockY, blockWidth, blockHeight, null);
        g.drawImage(normalBlockImage, blockX, blockY, blockWidth, blockHeight, null);
        g.drawImage(normalBlockImage, blockX + 150, blockY - 50, blockWidth, blockHeight, null);
        g.drawImage(normalBlockImage, blockX + 200, blockY - 50, blockWidth, blockHeight, null);
    }

    // 繪製道具磚塊
    if (currentBackgroundIndex == 0) {
        g.drawImage(itemBlockImage, itemBlockX, itemBlockY, blockWidth, blockHeight, null);
    }

    // 繪製蘑菇
    if (mushroomVisible) {
        g.drawImage(mushrooImage, mushroomX, mushroomY, blockWidth, blockHeight, null);
    }

    // 繪製主角
    if (jump && !isbigmario) {
        g.drawImage(playerjump, playerX, playerY, playerWidth, playerHeight, null);
    } else {
        if (isMoving && !isbigmario) {
            g.drawImage(playerGif.getImage(), playerX, playerY, playerWidth, playerHeight, null);
        } else {
            if (!isbigmario) {
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
            if (isbigmario) {
                g.drawImage(bigplayerImage, playerX, playerY - 40, playerWidth, playerHeight + 40, null);
            }
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
        switchToSceneTwo();
        playerX = 0; // 回到左邊起點
    } else if (direction < 0 && currentBackgroundIndex > 0) {
        currentBackgroundIndex--;
        playerX = screenWidth - playerWidth; // 回到右邊起點
    }
    initializeBlocks(); // 更新磚塊配置
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
// 重繪畫面
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
        if (currentBackgroundIndex == 0 || currentBackgroundIndex == 2
                || currentBackgroundIndex == 3) {
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
                    playerY + playerHeight > itemBlockY && playerY < itemBlockY + blockHeight) {

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
                    if (!mushroomVisible && itemused == false) {
                        mushroomVisible = true;
                        //itemused = true;
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
            if(currentBackgroundIndex == 1){
        if (playerX + playerWidth > block21X && playerX < block21X + blockWidth &&
                playerY + playerHeight > block21Y && playerY < block21Y + blockHeight) {

            // 頂部碰撞
            if (playerY + playerHeight > block21Y && playerY + playerHeight - jumpSpeed <= block21Y) {
                playerY = block21Y - playerHeight; // 停在磚塊上方
                isFalling = false;
                jump = false;
            }

            // 底部碰撞
            else if (playerY < block21Y + blockHeight && playerY + jumpSpeed >= block21Y + blockHeight) {
                isJumping = false;
                isFalling = true; // 開始下落
            }

            // 左側碰撞
            else if (playerX + playerWidth > block21X && playerX < block21X + blockWidth / 2) {
                playerX = block21X - playerWidth; // 阻止角色穿過磚塊左側
            }

            // 右側碰撞
            else if (playerX < block21X + blockWidth && playerX + playerWidth > block21X + blockWidth / 2) {
                playerX = block21X + blockWidth; // 阻止角色穿過磚塊右側
            }
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
        client game = new client();
        frame.add(game);
        frame.setSize(800, 600); // 視窗大小
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
