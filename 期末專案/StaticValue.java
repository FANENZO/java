import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class StaticValue {
    
    //背景
    public static BufferedImage bg = null;
    public static BufferedImage bg2 = null;

    //跳躍動作
    public static BufferedImage jump_L = null;
    public static BufferedImage jump_R = null;
    public static BufferedImage bigjump_R = null;
    public static BufferedImage superjump_R = null;
    public static BufferedImage stand_L = null;
    public static BufferedImage stand_R = null;
    public static BufferedImage bigstand_R = null;
    public static BufferedImage superstand_R = null;

    //奔跑動作
    public static List<BufferedImage> run_L = new ArrayList<>();
    public static List<BufferedImage> run_R = new ArrayList<>();
    public static List<BufferedImage> bigrun_R = new ArrayList<>();
    public static List<BufferedImage> superrun_R = new ArrayList<>();

    //城堡
    public static BufferedImage tower = null;
    //旗桿
    public static BufferedImage flag = null;
    public static List<BufferedImage> obstacle = new ArrayList<>();

    //栗寶寶
    public static List<BufferedImage> Goomba = new ArrayList<>();
    //食人花
    public static List<BufferedImage> flower = new ArrayList<>();

    //照片路徑，方便調用
    public static String path = "C:/Users/nt226/Desktop/java/test/src/images/";


    //初始化
    public static void init(){
        
        try {
            //背景
            bg = ImageIO.read(new File(path + "background.png"));
            bg2 = ImageIO.read(new File(path + "background2.png"));

            //站立
            stand_R = ImageIO.read(new File(path + "mario1.png"));
            bigstand_R = ImageIO.read(new File(path + "bigmario1.png"));
            superstand_R = ImageIO.read(new File(path + "firemario1.png"));


        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
