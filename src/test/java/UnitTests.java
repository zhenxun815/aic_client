import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.tqhy.client.models.entity.DownloadInfo;
import com.tqhy.client.models.msg.server.ClientMsg;
import com.tqhy.client.network.Network;
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
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    public void testJudgeDcm() {
        String dirPath = "C:\\Users\\qing\\Documents\\WeChat Files\\shamaohengheng\\FileStorage\\File\\2019-07\\a735aadbe0f9fa1605520f641ccbb978";
        String dcmName = "c4ca4238a0b923820dcc509a6f75849b.3.6.1.4.1.25403.220295149980554.168.20190703101815.4.dcm";
        boolean isDcm = FileUtils.isDcmFile(new File(dirPath, dcmName));
        logger.info("is dcm {}", isDcm);
    }

    @Test
    public void testParseDcm() {

        String libPath = System.getProperty("java.library.path");
        logger.info("lib path: is: " + libPath);

        File dcmDir = new File("C:\\Users\\qing\\Documents\\WeChat Files\\shamaohengheng\\FileStorage\\File\\2019-07\\a735aadbe0f9fa1605520f641ccbb978");
        File[] dcmFiles = dcmDir.listFiles(FileUtils::isDcmFile);
        logger.info("dcm count is: " + dcmFiles.length);
        Arrays.stream(dcmFiles)
              .forEach(dcmFile -> {
                  File jpgFile = FileUtils.transToJpg(dcmFile, dcmDir).get();
                  System.out.println("trans jpg file finish..." + jpgFile.getAbsolutePath());
              });

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

    @Test
    public void testJson() {
        String str = "download;{\"fileName\":\"猫猫狗狗\",\"imgUrlString\":\"/home/tqhy/tf/train/dd6e323d1eee4487be6034ea383e053c/validate_img_result/1000/1679091c5a880faf6fb5e6087eb1b2dc.jpg;/home/tqhy/tf/train/dd6e323d1eee4487be6034ea383e053c/validate_img_result/1000/8e296a067a37563370ded05f5a3bf3ec.jpg\"}";
        String replace = str.replace("download;", "");
        logger.info("replace is {}", replace);
        JsonReader jsonReader = new JsonReader(new StringReader(replace));
        jsonReader.setLenient(true);
        DownloadInfo downloadInfo = new Gson().fromJson(jsonReader, DownloadInfo.class);
        logger.info("download info is: {}", downloadInfo);
    }
}
