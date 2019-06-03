import com.tqhy.client.models.msg.server.ClientMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.task.Dcm2JpgTask;
import com.tqhy.client.utils.FileUtils;
import com.tqhy.client.utils.GsonUtils;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Yiheng
 * @create 1/29/2019
 * @since 1.0.0
 */

public class UnitTests {

    Logger logger = LoggerFactory.getLogger(UnitTests.class);

    @Test
    public void testParseDcm() {

        String libPath = System.getProperty("java.library.path");
        logger.info("lib path: is: " + libPath);

        File dcmDir = new File("F:\\dicom\\4321\\case1\\");
        File[] dcmFiles = dcmDir.listFiles(File::isFile);
        logger.info("dcm count is: " + dcmFiles.length);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Arrays.stream(dcmFiles)
              .collect(ArrayList<File>::new, (list, dicomFile) -> {
                  Future<File> fileFuture = executor.submit(Dcm2JpgTask.of(dicomFile, dcmDir));
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
        List<File> invalidDirs = Arrays.stream(files)
                                       .filter(caseDir -> {
                                           File[] caseSubDirs = caseDir.listFiles(File::isDirectory);
                                           return null != caseSubDirs && caseSubDirs.length > 0;
                                       }).collect(Collectors.toList());
        logger.info("files {}", invalidDirs.size());
    }

    @Test
    public void testIp() {
        File dir = new File("C:\\Users\\qing\\Pictures\\shadow\\jpg\\test2");
        HashMap<File, String> filesMapInDir = getFilesMapInDir(dir, file -> FileUtils.isDcmFile(file) || FileUtils.isJpgFile(file));
        filesMapInDir.forEach((k, v) -> logger.info("k is {},v is {}", k, v));
    }

    @Test
    public void testSys() {
        File source = new File("D:\\tq_workspace\\client3\\out\\artifacts\\client3\\bundles\\client3\\app", "opencv_java_64bit.dll");
        File dest = new File("D:\\tq_workspace\\client3\\out\\artifacts\\client3\\bundles\\client3\\app\\dll", "opencv_java.dll");
        boolean copyFile = FileUtils.copyFile(source, dest);
        logger.info("copy: " + copyFile);
        //System.out.println("arch: " + SystemUtils.getArc());
    }

    public HashMap<File, String> getFilesMapInDir(File dir, Predicate<File> filter) {

        File[] files = dir.listFiles();
        String dirName = dir.getName();
        logger.info("dir name is: {}", dirName);
        HashMap<File, String> fileMap = Arrays.stream(files)
                                              .filter(File::isFile)
                                              .collect(HashMap::new, (map, file) -> {
                                                  if (filter.test(file)) {
                                                      map.put(file, dirName);
                                                  }
                                              }, HashMap::putAll);
        return fileMap;
    }

    @Test
    public void testNet() {
        try {
            ResponseBody responseBody = Network.getAicApi()
                                               .pingServer()
                                               .execute()
                                               .body();
            ClientMsg clientMsg = GsonUtils.parseResponseToObj(responseBody);
            logger.info("client msg {}", clientMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDownLoad() {
        String downloadUrl = "/home/tqhy/aic/file/15cc97aaa6964fc5b6caf00020d575a4/20190517174054644/b798abe6e1b1318ee36b0dcb3fb9e4d3/fd9c69eb905feede1a7fc2fdfcf0fbdb.jpg";

        Network.getAicApi()
               .download(downloadUrl)
               //.observeOn(Schedulers.io())
               //.subscribeOn(Schedulers.io())
               .subscribe(response -> {
                   String header = response.headers().get("Content-Disposition");
                   logger.info("header is {}", header);
                   String[] split = header.split("filename=");
                   String fileName = split[1];
                   File file = new File("C:\\Users\\qing\\Pictures\\shadow", fileName);

                   BufferedSink sink = null;
                   try {
                       sink = Okio.buffer(Okio.sink(file));
                       sink.writeAll(response.body().source());
                       sink.close();
                   } catch (FileNotFoundException e) {
                       e.printStackTrace();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               });
        //logger.info("response is: {}", response.string());
    }
}
