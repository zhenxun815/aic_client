import com.tqhy.client.ClientApplication;
import com.tqhy.client.task.Dcm2JpgTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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

public class Tests {

    @Test
    public void test() {
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
}
