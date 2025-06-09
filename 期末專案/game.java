import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;

public class game extends JFrame implements KeyListener {

    //遊戲視窗設定
    public void launch() {

        this.setVisible(true);

        //視窗大小
        this.setSize(800,600);

        //視窗位置和標題
        this.setLocationRelativeTo(null);
        this.setTitle("遊戲專案");

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.addKeyListener(this);

    }

    
    @Override
    public void keyPressed(KeyEvent e) {
        // 當鍵盤按鍵被按下時調用
        System.out.println("鍵被按下: " + e.getKeyChar());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // 當鍵盤按鍵被鬆開時調用
        System.out.println("鍵被鬆開: " + e.getKeyChar());
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // 當鍵盤按鍵被輸入時調用
        System.out.println("鍵被輸入: " + e.getKeyChar());
    }

    
    public static void main(String[] args) {
        game game = new game();
        game.launch();
    }


}