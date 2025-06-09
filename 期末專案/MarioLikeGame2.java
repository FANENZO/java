import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MarioLikeGame2 extends JPanel implements KeyListener, Runnable {
    // 主角屬性
    private int playerX = 50, playerY; // 主角初始位置
    private int playerWidth = 50, playerHeight = 50;
    private int playerSpeed = 5;
    private Image playerImage; // 角色圖片
    private Image playerjump;
    private ImageIcon playerGif; // 角色 GIF
    private boolean isMoving = false; // 判斷角色是否在移動
    private boolean jump = false;

    // 背景屬性
    private Image[] backgrounds = new Image[2];
    private int currentBackgroundIndex = 0; // 當前背景索引
    private int screenWidth = 800; // 視窗寬度
    private int screenHeight = 600; // 視窗高度
    private int groundHeight = 120; // 地面高度
    private int groundLevel; // 地面頂部位置

    // 移動邏輯
    private boolean isJumping = false;
    private boolean isFalling = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;

    private int jumpHeight = 180; // 跳躍高度
    private int jumpSpeed = 5; // 跳躍速度

    public MarioLikeGame2() {
        setFocusable(true);
        addKeyListener(this);

        // 設定地面頂部位置
        groundLevel = screenHeight - groundHeight; // 地面位於畫布底部以上 200px
        playerY = groundLevel - playerHeight; // 將角色初始化在地面上

        // 載入背景圖片
        backgrounds[0] = new ImageIcon("images/background.png").getImage();
        backgrounds[1] = new ImageIcon("images/background2.png").getImage();
        
        //角色
        playerImage = new ImageIcon("images/mario1.png").getImage();
        playerGif = new ImageIcon("images/output-onlinegiftools.gif");
        playerjump = new ImageIcon("images/mariojmup.png").getImage();
        new Thread(this).start(); // 啟動遊戲循環
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 繪製背景
        g.drawImage(backgrounds[currentBackgroundIndex], 0, 0, screenWidth, getHeight(), null);

        // 繪製主角
        if (jump == true) {
            g.drawImage(playerjump, playerX, playerY, playerWidth, playerHeight, null);
        }
        else {
            if (isMoving == true) {
                g.drawImage(playerGif.getImage(), playerX, playerY,playerWidth, playerHeight, null);
            }
            else {
                g.drawImage(playerImage, playerX, playerY, playerWidth, playerHeight, null);
            }
        }
        
        
        // g.setColor(Color.RED);
        // g.fillRect(playerX, playerY, playerWidth, playerHeight);

    }

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

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(16); // 遊戲循環 (約60FPS)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 左右移動邏輯
            if (movingLeft && playerX > 0) {
                playerX -= playerSpeed;
            }
            if (movingRight && playerX + playerWidth < screenWidth) {
                playerX += playerSpeed;
            }

            // 背景切換邏輯
            if (playerX <= 0) { // 左邊界
                currentBackgroundIndex = (currentBackgroundIndex - 1 + backgrounds.length) % backgrounds.length;
                playerX = screenWidth - playerWidth; // 角色移到右邊界
            } else if (playerX + playerWidth >= screenWidth) { // 右邊界
                currentBackgroundIndex = (currentBackgroundIndex + 1) % backgrounds.length;
                playerX = 0; // 角色移到左邊界
            }

            // 處理跳躍邏輯
            if (isJumping) {
               // jump = true;
                playerY -= jumpSpeed; // 向上跳躍
                if (groundLevel - playerY >= jumpHeight) {
                    
                    isJumping = false;
                    isFalling = true;
                }
            } else if (isFalling) {
                playerY += jumpSpeed; // 自由落體
                if (playerY >= groundLevel - playerHeight) {
                    playerY = groundLevel - playerHeight; // 停止下落
                    isFalling = false;
                    jump = false;
                }
            }

            repaint(); // 更新畫面
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("馬力歐遊戲雛形");
        MarioLikeGame2 game = new MarioLikeGame2();
        frame.add(game);
        frame.setSize(800, 600); // 視窗大小
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
