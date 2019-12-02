package com.tqhy.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Yiheng
 * @create 2018/7/12
 * @since 1.0.0
 */
public class ImgUtils {

    private static Logger logger = LoggerFactory.getLogger(ImgUtils.class);

    public static String cutImg(String originImgPath, String cuttedImgPath, int x, int y, int width, int height) {
        try {
            File originImg = new File(originImgPath);
            File cuttedImmg = new File(cuttedImgPath);
            if (originImg.exists()) {
                BufferedImage bufferedImage = ImageIO.read(originImg);
                BufferedImage bufferedSubImage = bufferedImage.getSubimage(x, y, width, height);
                boolean cutted = ImageIO.write(bufferedSubImage, "jpg", cuttedImmg);
                if (cutted) {
                    return zoomImage(cuttedImmg, 100);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 截取屏幕
     *
     * @param screenImgPath
     * @return
     * @throws Exception
     */
    public static String captureScreen(String screenImgPath) {
        logger.info("start cap screen to {}", screenImgPath);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screenDevices = ge.getScreenDevices();
        int screenIndex = 0;
        GraphicsDevice secondDevice = screenDevices[screenIndex];
        Rectangle secondBounds = secondDevice.getDefaultConfiguration().getBounds();
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        BufferedImage image = robot.createScreenCapture(secondBounds);
        // 截图保存的路径
        File screenImg = new File(screenImgPath);
        // 如果路径不存在,则创建
        if (!screenImg.getParentFile().exists()) {
            screenImg.getParentFile().mkdirs();
        }
        //判断文件是否存在，不存在就创建文件
        if (!screenImg.exists() && !screenImg.isDirectory()) {
            screenImg.mkdir();
        }

        try {
            ImageIO.write(image, "jpg", screenImg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //自动打开
        /*if (Desktop.isDesktopSupported()
                 && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                    Desktop.getDesktop().open(f);*/
        return screenImg.getAbsolutePath();
    }

    /**
     * 判断图片文件是否大于指定大小,如果大于则进行宽高等比例压缩到指定大小,并返回压缩后图片绝对路径.如果未压缩,则返回原图
     * 绝对路径.
     *
     * @param originImgFile 原图绝对路径
     * @param newWidth      压缩后图片宽度
     * @return 压缩后图片文件绝对路径, 如果未压缩, 则为原图路径
     * @throws Exception
     */
    public static String zoomImage(File originImgFile, int newWidth) {
        long length = originImgFile.length();
        logger.info("originImgFile.length() is: " + length);

        BufferedImage bufImgOld = null;
        BufferedImage bufImgNew = null;
        try {
            bufImgOld = ImageIO.read(originImgFile);
            int originHeight = bufImgOld.getHeight();
            int originWidth = bufImgOld.getWidth();
            int newHeight = Integer.parseInt(
                    new java.text.DecimalFormat("0").format(originHeight * newWidth / (originWidth * 1.0)));
            logger.debug("change image's height, width:{}, height:{}.", newWidth, newHeight);

            bufImgNew = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics g = bufImgNew.getGraphics();
            g.drawImage(bufImgOld, 0, 0, newWidth, newHeight, Color.LIGHT_GRAY, null);
            g.dispose();
            // 刷新此 Image 对象正在使用的所有可重构的资源.
            bufImgOld.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File tempFile = new File(FileUtils.getAppPath() + "/temp_img.jpg");
        if (!tempFile.exists() && !tempFile.isDirectory()) {
            tempFile.mkdir();
        }
        try {
            ImageIO.write(bufImgNew, "jpg", tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile.getAbsolutePath();
    }

}
