package com.tqhy.client.utils;

import com.tqhy.client.task.Dcm2JpgTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

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
                                                 } else {
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

    /**
     * 删除文件夹
     *
     * @param temp
     * @return
     */
    public static boolean deleteDir(File temp) {
        logger.info("into delete");
        if (temp.exists()) {
            File[] subFiles = temp.listFiles();
            Arrays.stream(subFiles)
                  .forEach(subFile -> {
                      if (subFile.isDirectory()) {
                          deleteDir(subFile);
                      } else {
                          subFile.delete();
                      }
                  });
            return temp.delete();
        }
        return false;
    }

    /**
     * 按行读取文件,返回一个由每行处理结果对象组成的集合
     *
     * @param file     待读行文件
     * @param function 对每一行内容处理的{@link Function <String,T> Function}
     * @param <T>      返回集合泛型
     * @return
     */
    public static <T> List<T> readLine(File file, Function<String, T> function) {
        ArrayList<T> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                T apply = function.apply(line);
                if (null != apply) {
                    list.add(apply);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }


    /**
     * 写文件
     *
     * @param file
     * @param info     待写入信息
     * @param function 处理写入内容,为null则不做任何处理
     * @param create   当文件不存在时是否创建新文件
     */
    public static void writeFile(File file, String info, @Nullable Function<StringBuilder, StringBuilder> function, boolean create) {

        if (create && !file.exists()) {
            createNewFile(file);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            if (null == function) {
                writer.write(info);
            } else {
                String apply = function.apply(new StringBuilder(info))
                                       .toString();
                writer.write(apply);
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建新文件
     *
     * @param file
     * @return
     */
    public static boolean createNewFile(File file) {
        if (file.exists()) {
            file.delete();
        }

        try {
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            boolean newFile = file.createNewFile();
            return newFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 获取项目所在路径
     *
     * @return
     */
    public static String getAppPath() {
        String jarPath = FileUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        int end = jarPath.lastIndexOf("/");
        String appPath = jarPath.substring(1, end);
        //logger.info("appPath is: "+appPath);
        return appPath;
    }

    /**
     * 获取相对jar包所在文件夹路径的{@link File File}对象
     *
     * @param relativePath 相对jar包所在文件夹相对路径.以<em>"/"<em/>开头
     * @param fileName     文件名
     * @return
     */
    public static File getLocalFile(String relativePath, String fileName) {
        String rootPath = getAppPath();
        String localFilePath = rootPath + relativePath;
        logger.info("localFilePath is: " + localFilePath);
        File localFile = new File(localFilePath, fileName);
        return localFile;
    }

    /**
     * 如果目标文件存在则替换之
     *
     * @param sourceFile
     * @param destFile
     * @return
     */
    public static boolean copyFile(File sourceFile, File destFile) {

        logger.info("source file: " + sourceFile.getAbsolutePath() + ", dest file: " + destFile.getAbsolutePath());
        if (!sourceFile.exists()) {
            return false;
        }
        if (destFile.exists()) {
            boolean delete = destFile.delete();
            if (!delete) {
                return false;
            }
        }

        if (createNewFile(destFile)) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile))) {
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile))) {
                    byte[] buffer = new byte[1024 * 8];
                    int i = 0;
                    while ((i = bis.read(buffer)) != -1) {
                        bos.write(buffer);
                    }
                    bos.flush();
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 复制jar包内资源文件到jar包外
     *
     * @param resourceName
     * @param destination
     * @return
     */
    public static boolean copyResource(String resourceName, String destination) {
        boolean succeess = true;
        InputStream resourceStream = FileUtils.class.getResourceAsStream(resourceName);
        logger.info("Copying ->" + resourceName + " to ->" + destination);

        try {
            Files.copy(resourceStream, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.error("copy resource error!!", ex);
            succeess = false;
        }

        return succeess;

    }
}
