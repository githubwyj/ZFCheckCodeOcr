/**
 * Created by Administrator on 2016/4/20.
 */


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final String CHECK_IMAGE = "http://210.44.176.45/CheckCode.aspx";
    private static RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(5000).setConnectTimeout(5000).build();
    private static final String TRAIN_PATH = "./train";

    private static Map<BufferedImage, String> trainMap = null;
    private static int index = 0;


    public static int isBlue(int colorInt) {
        Color color = new Color(colorInt);
        int rgb = color.getRed() + color.getGreen() + color.getBlue();
        if (rgb == 153) {
            return 1;
        }
        return 0;
    }

    public static int isBlack(int colorInt) {
        Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() <= 100) {
            return 1;
        }
        return 0;
    }


    public static BufferedImage removeBackgroud(String picFile)
            throws Exception {
        BufferedImage img = ImageIO.read(new File(picFile));

        //剪切验证码， 便于后面的处理
        img = img.getSubimage(5, 1, img.getWidth() - 5, img.getHeight() - 2);
        img = img.getSubimage(0, 0, 50, img.getHeight());


        int width = img.getWidth();
        int height = img.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (isBlue(img.getRGB(x, y)) == 1) {
                    img.setRGB(x, y, Color.BLACK.getRGB());
                } else {
                    img.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
        }
        return img;
    }

    public static List<BufferedImage> splitImage(BufferedImage img)
            throws Exception {
        List<BufferedImage> subImgs = new ArrayList<BufferedImage>();
        int width = img.getWidth() / 4;
        int height = img.getHeight();
        //前面剪切之后， 验证码的每个数字的位置分布都是有规律的
        subImgs.add(img.getSubimage(0, 0, width, height));
        subImgs.add(img.getSubimage(width, 0, width, height));
        subImgs.add(img.getSubimage(width * 2, 0, width, height));
        subImgs.add(img.getSubimage(width * 3, 0, width, height));
        return subImgs;
    }



    public static Map<BufferedImage, String> loadTrainData() throws Exception {
        if (trainMap == null) {
            Map<BufferedImage, String> map = new HashMap<BufferedImage, String>();
            File dir = new File(TRAIN_PATH);
            File[] files = dir.listFiles();
            for (File file : files) {
                map.put(ImageIO.read(file), file.getName().charAt(0) + "");
            }
            trainMap = map;
        }
        return trainMap;
    }

    public static String getSingleCharOcr(BufferedImage img,
                                          Map<BufferedImage, String> map) {
        String result = "#";
        int width = img.getWidth();
        int height = img.getHeight();
        int min = width * height;
        for (BufferedImage bi : map.keySet()) {
            int count = 0;
            if (Math.abs(bi.getWidth() - width) > 2)
                continue;
            int widthmin = width < bi.getWidth() ? width : bi.getWidth();
            int heightmin = height < bi.getHeight() ? height : bi.getHeight();
            Label1:
            for (int x = 0; x < widthmin; ++x) {
                for (int y = 0; y < heightmin; ++y) {
                    if (isBlack(img.getRGB(x, y)) != isBlack(bi.getRGB(x, y))) {
                        count++;
                        if (count >= min)
                            break Label1;
                    }
                }
            }
            if (count < min) {
                min = count;
                result = map.get(bi);
            }
        }
        return result;
    }

    public static String getAllOcr(String file) throws Exception {
        BufferedImage img = removeBackgroud(file);
        List<BufferedImage> listImg = splitImage(img);
        Map<BufferedImage, String> map = loadTrainData();
        String result = "";
        for (BufferedImage bi : listImg) {
            result += getSingleCharOcr(bi, map);
        }
        return result;
    }



    public static void main(String[] args) throws Exception {

        //下载验证码
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet checkImageRequest = new HttpGet(CHECK_IMAGE);
        checkImageRequest.setConfig(requestConfig);
        HttpResponse imageResponse = null;
        imageResponse = httpClient.execute(checkImageRequest);
        HttpEntity entity = imageResponse.getEntity();
        InputStream imageByte = entity.getContent();
        OutputStream outStream = new FileOutputStream("./image/image"+".jpg");

        byte[] buffer = new byte[1024];
        int len = 0;
        while( ((len = imageByte.read(buffer, 0, 1024)) != -1 )){
            outStream.write(buffer, 0, len);
        }

        outStream.close();
        imageByte.close();


        //进行识别
        System.out.print(getAllOcr("./image/image.jpg"));

    }

}




