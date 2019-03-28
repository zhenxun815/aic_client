package com.tqhy.client.task;



import lombok.*;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.image.BufferedImageUtils;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;

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
    public File convert(File dicomFile) {
        initImageWriter();
        if (null == dicomFile) {
            return null;
        }
        if (dicomFile.exists()) {

            try (ImageInputStream iis = ImageIO.createImageInputStream(dicomFile)) {
                File jpgDir = new File(dicomFile.getParent() + "/jpg");
                if (!jpgDir.exists()) {
                    jpgDir.mkdir();
                }
                File dest = new File(jpgDir, dicomFile.getName() + ".jpg");
                imageReader = ImageIO.getImageReadersByFormatName("DICOM").next();
                imageReader.setInput(iis);
                BufferedImage bi = imageReader.read(0, readParam());
                ColorModel cm = bi.getColorModel();
                if (cm.getNumComponents() == 3) {
                    bi = BufferedImageUtils.convertToIntRGB(bi);
                }

                if (dest.exists()) {
                    dest.delete();
                }
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

        return  convert(dicomFile);
    }
}
