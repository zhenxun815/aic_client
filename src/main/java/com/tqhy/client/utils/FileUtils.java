package com.tqhy.client.utils;

import com.tqhy.client.task.Dcm2JpgTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Yiheng
 * @create 4/2/2019
 * @since 1.0.0
 */
public class FileUtils {

    private static final String FILE_TYPE_JPG = "JPG";
    private static final String FILE_TYPE_DCM = "DCM";

    static Logger logger = LoggerFactory.getLogger(FileUtils.class);
    /**
     * 获取文件夹下所有文件
     *
     * @param dir
     * @return
     */
    public static List<File> getFilesInDir(File dir) {

        File[] files = dir.listFiles();
        ArrayList<File> collect = Arrays.stream(files)
                                        .collect(ArrayList::new, (list, file) -> {
                                            if (file.isFile()) {
                                                list.add(file);
                                            } else if (file.isDirectory()) {
                                                List<File> filesInSubDir = getFilesInDir(file);
                                                list.addAll(filesInSubDir);
                                            }
                                        }, ArrayList::addAll);
        return collect;
    }


    public static List<File> transAllToJpg(List<File> originFiles) {
        ArrayList<File> collect = originFiles.stream()
                                             .collect(ArrayList::new, (list, file) -> {
                                                 if (isDcmFile(file)) {
                                                     ExecutorService executor = Executors.newSingleThreadExecutor();
                                                     Future<File> jpgFileFuture = executor.submit(Dcm2JpgTask.of(file));
                                                     try {
                                                         logger.info("transfer dimcom " + file.getName() + " to jpg!");
                                                         File jpgFile = jpgFileFuture.get();
                                                         list.add(jpgFile);
                                                     } catch (InterruptedException e) {
                                                         e.printStackTrace();
                                                     } catch (ExecutionException e) {
                                                         e.printStackTrace();
                                                     }
                                                 }else {
                                                     list.add(file);
                                                 }
                                             }, ArrayList::addAll);
        return collect;
    }

    /**
     * 判断文件是否是DCM文件
     *
     * @param fileToJudge
     * @return
     */
    public static boolean isDcmFile(File fileToJudge) {
        byte[] bytes = new byte[132];
        try (FileInputStream in = new FileInputStream(fileToJudge)) {
            int len = readAvailable(in, bytes, 0, 132);
            return 132 == len && bytes[128] == 'D' && bytes[129] == 'I' && bytes[130] == 'C' && bytes[131] == 'M';
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断是否可读取指定长度信息
     *
     * @param in
     * @param b   要读取的字节数组
     * @param off 开始位置偏移量
     * @param len 读取最大长度
     * @return 读取到长度
     * @throws IOException
     */
    public static int readAvailable(InputStream in, byte b[], int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        int wpos = off;
        while (len > 0) {
            int count = in.read(b, wpos, len);
            if (count < 0) {
                break;
            }
            wpos += count;
            len -= count;
        }
        return wpos - off;
    }

}
