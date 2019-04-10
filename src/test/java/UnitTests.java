import com.tqhy.client.task.Dcm2JpgTask;
import com.tqhy.client.utils.FileUtils;
import com.tqhy.client.utils.NetworkUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Yiheng
 * @create 1/29/2019
 * @since 1.0.0
 */

public class UnitTests {

    Logger logger = LoggerFactory.getLogger(UnitTests.class);

    @Test
    public void testParseDicom() {
        File dicomDir = new File("F:\\dicom\\12345\\");
        File[] dicomFiles = dicomDir.listFiles(File::isFile);

        Arrays.stream(dicomFiles)
              .collect(ArrayList<File>::new, (list, dicomFile) -> {
                  ExecutorService executor = Executors.newSingleThreadExecutor();
                  Future<File> fileFuture = executor.submit(Dcm2JpgTask.of(dicomFile));
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
        // String batchNumber = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        //logger.info("batchNumber is: "+batchNumber);
        String dirToDeletePath = "F:\\dicom\\1234\\case3\\TQHY_TEMP";
        File dirToDelete = new File(dirToDeletePath);
        boolean deleteDir = FileUtils.deleteDir(dirToDelete);

        logger.info("delete success: " + deleteDir);
    }

    @Test
    public void testIp() {
        boolean ip = NetworkUtils.isIP("123");
        logger.info("is ip: " + ip);
    }
}
