package com.tqhy.client.utils;


import lombok.NonNull;
import org.dcm4che3.image.BufferedImageUtils;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * 将Dicom文件转换为Jpg文件任务
 *
 * @author Yiheng
 * @create 2018/5/18
 * @since 1.0.0
 */
public class Dcm2JpgUtil {

    static Logger logger = LoggerFactory.getLogger(Dcm2JpgUtil.class);
    private static boolean autoWindowing = true;
    private static boolean preferWindow = true;
    private static int overlayGrayscaleValue = 0xffff;
    private static int overlayActivationMask = 0xffff;
    private static ImageWriteParam imageWriteParam;
    private static ImageWriter imageWriter;
    private static ImageReader imageReader;

    @NonNull
    private File dicomFile;

    @NonNull
    private File jpgDir;

    /**
     * 初始化ImageWriter
     */
    public static void initImageWriter() {
        Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName("JPEG");
        if (!imageWriters.hasNext()) {
            throw new IllegalArgumentException("formatNotSupported");
        }
        imageWriter = imageWriters.next();
        imageWriteParam = imageWriter.getDefaultWriteParam();
    }

    /**
     * 转换DCM文件到同一文件夹下jpg文件夹下
     *
     * @return 转换后的jgp文件File对象
     */
    public static File convert(File dcmFile, File jpgDir) {
        logger.info("start convert dicom to jpg: " + dcmFile.getAbsolutePath());

        initImageWriter();
        if (null == dcmFile) {
            return null;
        }
        if (dcmFile.exists()) {

            try (ImageInputStream iis = ImageIO.createImageInputStream(dcmFile)) {

                if (!jpgDir.exists()) {
                    jpgDir.mkdirs();
                }
                imageReader = ImageIO.getImageReadersByFormatName("DICOM").next();
                imageReader.setInput(iis);
                BufferedImage bi = imageReader.read(0, readParam());
                ColorModel cm = bi.getColorModel();
                if (cm.getNumComponents() == 3) {
                    bi = BufferedImageUtils.convertToIntRGB(bi);
                }

                File dest = genJpgFile(dcmFile, jpgDir);

                ImageOutputStream ios = ImageIO.createImageOutputStream(dest);
                try {
                    imageWriter.setOutput(ios);
                    imageWriter.write(null, new IIOImage(bi, null, null), imageWriteParam);
                    return dest;
                } finally {
                    try {
                        ios.close();
                    } catch (IOException ignore) {
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 生成待写入jpg文件 {@link File File}对象并返回,若已存在则删除已有文件
     *
     * @param dcmFile 原始dcm文件
     * @param jpgDir  生成jpg文件文件夹
     * @return destJpgFile
     */
    private static File genJpgFile(File dcmFile, File jpgDir) {
        String dcmFileName = dcmFile.getName();

        String destFileName =
                dcmFileName.endsWith("dcm") ? dcmFileName.replace("dcm", "jpg") : dcmFileName.concat(".jpg");
        File destJpgFile = new File(jpgDir, destFileName);

        if (destJpgFile.exists()) {
            destJpgFile.delete();
        }
        return destJpgFile;
    }

    /**
     * 设置DicomImageReadParam 数
     *
     * @return
     */
    private static ImageReadParam readParam() {
        DicomImageReadParam param = (DicomImageReadParam) imageReader.getDefaultReadParam();
        param.setAutoWindowing(autoWindowing);
        param.setPreferWindow(preferWindow);
        param.setOverlayActivationMask(overlayActivationMask);
        param.setOverlayGrayscaleValue(overlayGrayscaleValue);
        return param;
    }
}
