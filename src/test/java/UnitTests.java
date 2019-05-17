import com.tqhy.client.task.Dcm2JpgTask;
import com.tqhy.client.utils.FileUtils;
import com.tqhy.client.utils.NetworkUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author Yiheng
 * @create 1/29/2019
 * @since 1.0.0
 */

public class UnitTests {

    Logger logger = LoggerFactory.getLogger(UnitTests.class);

    @Test
    public void testParseDicom() {

        String libPath = System.getProperty("java.library.path");
        logger.info("lib path: is: " + libPath);

        File dicomDir = new File("F:\\dicom\\4321\\case1\\");
        File[] dicomFiles = dicomDir.listFiles(File::isFile);
        logger.info("dicom count is: " + dicomFiles.length);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Arrays.stream(dicomFiles)
              .collect(ArrayList<File>::new, (list, dicomFile) -> {
                  Future<File> fileFuture = executor.submit(Dcm2JpgTask.of(dicomFile, dicomDir));
                  try {
                      File jpgFile = fileFuture.get();
                      System.out.println("trans jpg file finish..." + jpgFile.getAbsolutePath());
                  } catch (InterruptedException e) {
                      e.printStackTrace();
                  } catch (ExecutionException e) {
                      e.printStackTrace();
                  }
              }, ArrayList::addAll)
              .forEach(jpgFile -> System.out.println("trans jpg file finish..." + jpgFile.getAbsolutePath()));

    }

    @Test
    public void testFileUtils() {
       /* String imgPath = "C:\\Users\\qing\\Pictures\\shadow\\error\\test2\\13.jpeg";
        File imgFile = new File(imgPath);
        boolean isJpgFile = FileUtils.isJpgFile(imgFile);
        logger.info("is jpg file {}", isJpgFile);*/

        String dirPath = "C:\\Users\\qing\\Pictures\\shadow\\error";
        File dir = new File(dirPath);
        File[] files = dir.listFiles(File::isFile);
        List<File> unvalidDirs = Arrays.stream(files)
                                       .filter(caseDir -> {
                                           File[] caseSubDirs = caseDir.listFiles(File::isDirectory);
                                           return null != caseSubDirs && caseSubDirs.length > 0;
                                       }).collect(Collectors.toList());
        logger.info("files {}", unvalidDirs.size());
    }

    @Test
    public void testIp() {
        boolean ip = NetworkUtils.isIP("123");
        logger.info("is ip: " + ip);
    }

    @Test
    public void testSys() {
        File source = new File("D:\\tq_workspace\\client3\\out\\artifacts\\client3\\bundles\\client3\\app", "opencv_java_64bit.dll");
        File dest = new File("D:\\tq_workspace\\client3\\out\\artifacts\\client3\\bundles\\client3\\app\\dll", "opencv_java.dll");
        boolean copyFile = FileUtils.copyFile(source, dest);
        logger.info("copy: " + copyFile);
        //System.out.println("arch: " + SystemUtils.getArc());
    }
}
