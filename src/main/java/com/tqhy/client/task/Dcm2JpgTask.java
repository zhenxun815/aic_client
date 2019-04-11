package com.tqhy.client.task;


import com.tqhy.client.config.Constants;
import lombok.*;
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
import java.util.concurrent.Callable;

/**
 * 将Dicom文件转换为Jpg文件任务
 *
 * @author Yiheng
 * @create 2018/5/18
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor(staticName = "of")
public class Dcm2JpgTask implements Callable<File> {


    static Logger logger = LoggerFactory.getLogger(Dcm2JpgTask.class);
    private boolean autoWindowing = true;
    private boolean preferWindow = true;
    private int overlayGrayscaleValue = 0xffff;
    private int overlayActivationMask = 0xffff;
    private ImageWriteParam imageWriteParam;
    private ImageWriter imageWriter;
    private ImageReader imageReader;

    @NonNull
    private File dicomFile;

    /**
     * 初始化ImageWriter
     */
    public void initImageWriter() {
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
    public File convert(File dcmFile) {
        logger.info("start convert dicom to jpg: " + dcmFile.getAbsolutePath());

        initImageWriter();
        if (null == dcmFile) {
            return null;
        }
        if (dcmFile.exists()) {

            try (ImageInputStream iis = ImageIO.createImageInputStream(dcmFile)) {
                File jpgDir = new File(dcmFile.getParent(), Constants.PATH_TEMP_JPG);
                if (!jpgDir.exists()) {
                    jpgDir.mkdir();
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
    private File genJpgFile(File dcmFile, File jpgDir) {
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
    private ImageReadParam readParam() {
        DicomImageReadParam param = (DicomImageReadParam) imageReader.getDefaultReadParam();
        param.setAutoWindowing(autoWindowing);
        param.setPreferWindow(preferWindow);
        param.setOverlayActivationMask(overlayActivationMask);
        param.setOverlayGrayscaleValue(overlayGrayscaleValue);
        return param;
    }


    @Override
    public File call() throws Exception {

        return convert(dicomFile);
    }
}
