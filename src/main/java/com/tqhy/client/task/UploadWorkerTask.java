package com.tqhy.client.task;

import com.tqhy.client.config.Constants;
import com.tqhy.client.models.msg.local.UploadMsg;
import com.tqhy.client.models.msg.server.ClientMsg;
import com.tqhy.client.network.Network;
import com.tqhy.client.utils.FileUtils;
import com.tqhy.client.utils.GsonUtils;
import com.tqhy.client.utils.NetworkUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import lombok.*;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tqhy.client.utils.FileUtils.isDcmFile;
import static com.tqhy.client.utils.FileUtils.transToJpg;

/**
 * @author Yiheng
 * @create 4/1/2019
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor(staticName = "with")
public class UploadWorkerTask extends Task {

    public static final String PROGRESS_MSG_ERROR = "error";
    public static final String PROGRESS_MSG_COMPLETE = "complete";
    public static final String PROGRESS_MSG_UPLOAD = "upload";
    public static final String PROGRESS_MSG_COLLECT = "collect";

    Logger logger = LoggerFactory.getLogger(UploadWorkerTask.class);
    BooleanProperty jumpToLandFlag = new SimpleBooleanProperty(false);
    BooleanProperty stopUploadFlag = new SimpleBooleanProperty(false);

    @NonNull
    File dirToUpload;
    @NonNull
    UploadMsg uploadMsg;
    @NonNull
    String localDataPath;

    /**
     * 待上传总文件数
     */
    int total;

    /**
     * 上传成功文件数
     */
    AtomicInteger successCount;

    /**
     * 上传失败文件数
     */
    AtomicInteger failCount;

    /**
     * 本次上传任务信息记录文件
     */
    File uploadInfoFile;
    private HashMap<File, String> totalImgFileMap;
    private File jpgDir;

    @Override
    protected Object call() throws Exception {
        logger.info("start upload task...");

        initTaskStatus();

        if (total == 0) {
            logger.info("total file count is 0!");
            updateProgress(100, 100);
            String completeMsg = PROGRESS_MSG_COMPLETE + ";" + successCount.get() + ";" + failCount.get();
            updateMessage(completeMsg);
        }

        logger.info("total file count is: " + total);

        String uploadType = uploadMsg.getUploadType();

        if (UploadMsg.UPLOAD_TYPE_CASE.equals(uploadType)) {
            uploadCase(uploadMsg);
        } else if (UploadMsg.UPLOAD_TYPE_TEST.equals(uploadType)) {
            uploadTest(uploadMsg);
        }
        return null;
    }

    private void initTaskStatus() {
        successCount = new AtomicInteger(0);
        failCount = new AtomicInteger(0);
        jpgDir = new File(dirToUpload, Constants.PATH_TEMP_JPG);
        //批次目录下图片
        HashMap<File, String> directImgFileMap = FileUtils.getFilesMapInDir(dirToUpload, file -> isDcmFile(file) || FileUtils.isJpgFile(file), null);
        HashMap<File, String> subDirImgFileMap = FileUtils.getFilesMapInSubDir(dirToUpload, file -> isDcmFile(file) || FileUtils.isJpgFile(file));
        HashMap<File, String> tempTotalFile = new HashMap<>();
        tempTotalFile.putAll(directImgFileMap);
        tempTotalFile.putAll(subDirImgFileMap);

        total = tempTotalFile.values()
                             .size();
        totalImgFileMap = transAllToJpg(tempTotalFile, jpgDir);

        uploadInfoFile = FileUtils.getLocalFile(localDataPath, uploadMsg.getBatchNumber() + ".txt");
        stopUploadFlag.setValue(false);
    }

    /**
     * 上传测试数据
     *
     * @param uploadMsg
     */
    private void uploadTest(UploadMsg uploadMsg) {

        String token = uploadMsg.getToken();
        String batchNumber = uploadMsg.getBatchNumber();
        String dirPathToUpload = dirToUpload.getAbsolutePath();

        HashMap<String, String> requestParamMap = new HashMap<>();
        requestParamMap.put("token", token);
        requestParamMap.put("batchNumber", batchNumber);
        requestParamMap.put("taskId", uploadMsg.getUploadId());
        requestParamMap.put("name", dirToUpload.getName());

        logger.info("upload token: {}, dirToUpload: {}, batchNumber: {}", token, dirPathToUpload, batchNumber);
        upLoadDir(requestParamMap);
    }

    /**
     * 上传病例数据
     *
     * @param uploadMsg
     */
    private void uploadCase(UploadMsg uploadMsg) {

        String token = uploadMsg.getToken();
        String batchNumber = uploadMsg.getBatchNumber();
        String dirPathToUpload = dirToUpload.getAbsolutePath();

        HashMap<String, String> requestParamMap = new HashMap<>();
        requestParamMap.put("token", uploadMsg.getToken());
        requestParamMap.put("batchNumber", uploadMsg.getBatchNumber());
        requestParamMap.put("projectId", uploadMsg.getUploadId());
        requestParamMap.put("remarks", uploadMsg.getRemarks());
        requestParamMap.put("name", dirToUpload.getName());

        logger.info("upload token: {}, dirToUpload: {}, batchNumber: {}", token, dirPathToUpload, batchNumber);
        upLoadDir(requestParamMap);
    }

    private void upLoadDir(HashMap<String, String> requestParamMap) {
        totalImgFileMap.forEach((file, caseName) -> {
            if (shouldStop()) return;
            requestParamMap.put("caseName", caseName);
            logger.info("case name is: {}", caseName);
            Map<String, RequestBody> requestMap = NetworkUtils.createRequestParamMap(requestParamMap);
            doUpLoad(file, requestMap);
        });
    }

    private void doUpLoad(File fileToUpload, Map<String, RequestBody> requestParamMap) {

        logger.info("start upload file: " + fileToUpload.getAbsolutePath());
        if (shouldStop()) return;
        MultipartBody.Part filePart = NetworkUtils.createFilePart("file", fileToUpload.getAbsolutePath());
        Observable<ResponseBody> responseBodyObservable = null;

        if (UploadMsg.UPLOAD_TYPE_TEST.equals(uploadMsg.getUploadType())) {
            responseBodyObservable = Network.getAicApi().uploadTestFiles(requestParamMap, filePart);
        } else if (UploadMsg.UPLOAD_TYPE_CASE.equals(uploadMsg.getUploadType())) {
            responseBodyObservable = Network.getAicApi().uploadFiles(requestParamMap, filePart);
        }

        responseBodyObservable.observeOn(Schedulers.io())
                              .subscribeOn(Schedulers.trampoline())
                              .blockingSubscribe(new Observer<ResponseBody>() {
                                  @Override
                                  public void onSubscribe(Disposable d) {
                                      logger.info("Disposable: " + d);
                                  }

                                  @Override
                                  public void onNext(ResponseBody responseBody) {
                                      ClientMsg clientMsg = GsonUtils.parseResponseToObj(responseBody);
                                      Integer flag = clientMsg.getFlag();
                                      if (203 == flag) {
                                          jumpToLandFlag.set(true);
                                      }
                                      if (2 == flag) {
                                          logger.info("server get file fail...{}", fileToUpload.getAbsolutePath());
                                          failCount.incrementAndGet();
                                          successCount.decrementAndGet();
                                          FileUtils.appendFile(uploadInfoFile, fileToUpload.getAbsolutePath(), builder -> builder.append(Constants.NEW_LINE), true);
                                      }
                                  }

                                  @Override
                                  public void onError(Throwable e) {
                                      logger.error("upload " + fileToUpload.getAbsolutePath() + " failed", e);
                                      failCount.incrementAndGet();
                                      updateUploadStatus();
                                      FileUtils.appendFile(uploadInfoFile, fileToUpload.getAbsolutePath(), builder -> builder.append(Constants.NEW_LINE), true);
                                  }

                                  @Override
                                  public void onComplete() {
                                      successCount.incrementAndGet();
                                      updateUploadStatus();
                                  }

                              });
    }

    /**
     * 更新上传状态
     */
    private void updateUploadStatus() {
        int completeCount = successCount.get() + failCount.get();
        double progress = (completeCount + 0D) / total * 100;
        logger.info("complete count is: " + completeCount + ", progress is: " + progress);
        updateProgress(completeCount, total);

        String completeMsg = PROGRESS_MSG_COMPLETE + ";" + successCount.get() + ";" + failCount.get();
        String uploadMsg = PROGRESS_MSG_UPLOAD + ";" + progress;
        updateMessage(progress == 100.0D ? completeMsg : uploadMsg);
        deleteTempFiles(completeCount);
    }

    /**
     * 删除生成的临时jpg文件
     */
    private void deleteTempFiles(int completeCount) {

        if (completeCount == total) {
            File temp = new File(dirToUpload, Constants.PATH_TEMP_JPG);
            FileUtils.deleteDir(temp);
        }
    }

    private AtomicInteger fakeUpload(File caseDir) {
        String libPath = System.getProperty("java.library.path");
        logger.info("lib path: is: " + libPath);

        HashMap<File, String> filesMapInDir = FileUtils.getFilesMapInDir(caseDir, file -> FileUtils.isJpgFile(file) || isDcmFile(file), null);
        HashMap<File, String> transformedFilesMap = transAllToJpg(filesMapInDir, jpgDir);
        logger.info("into fakeUpload...");
        AtomicInteger completeCount = new AtomicInteger(0);
        int total = transformedFilesMap.values().size();

        transformedFilesMap.forEach((file, caseName) ->
                                            Observable.create((ObservableOnSubscribe<File>) emitter -> {
                                                                  emitter.onNext(file);
                                                                  emitter.onComplete();
                                                              }
                                            ).observeOn(Schedulers.io())
                                                      .subscribeOn(Schedulers.single())
                                                      .blockingSubscribe(new Observer<File>() {
                                                          @Override
                                                          public void onSubscribe(Disposable d) {
                                                              logger.info("Disposable: " + d);
                                                          }

                                                          @Override
                                                          public void onNext(File file) {
                                                              try {
                                                                  Thread.sleep(2000);
                                                                  logger.info(file.getAbsolutePath() + " uploading...");
                                                              } catch (InterruptedException e) {
                                                                  e.printStackTrace();
                                                              }
                                                          }

                                                          @Override
                                                          public void onError(Throwable e) {
                                                              failCount.incrementAndGet();
                                                              e.printStackTrace();
                                                          }

                                                          @Override
                                                          public void onComplete() {
                                                              completeCount.incrementAndGet();
                                                              updateProgress(completeCount.get(), total);
                                                              double progress = (completeCount.get() + failCount.get() + 0D) / total * 100;
                                                              logger.info("complete count is: " + completeCount.get() + ", progress is: " + progress);
                                                              updateMessage(progress == 100.0D ? PROGRESS_MSG_COMPLETE : DecimalFormat.getInstance().format(progress));
                                                          }
                                                      }));

        return completeCount;
    }

    /**
     * @param originFiles
     * @return
     */
    private HashMap<File, String> transAllToJpg(HashMap<File, String> originFiles, File jpgDir) {
        AtomicInteger completeCount = new AtomicInteger(0);
        HashMap<File, String> jpgFileMap = originFiles.entrySet()
                                                      .stream()
                                                      .collect(HashMap::new,
                                                               (map, entry) -> {
                                                                   if (shouldStop()) return;

                                                                   File file = entry.getKey();
                                                                   String caseName = entry.getValue();
                                                                   if (isDcmFile(file)) {
                                                                       File jpgCaseDir = new File(jpgDir, caseName);

                                                                       File jpgFile = transToJpg(file, jpgCaseDir);
                                                                       map.put(jpgFile, caseName);
                                                                   } else {
                                                                       map.put(file, caseName);
                                                                   }
                                                                   double progress = (completeCount.incrementAndGet() + 0D) / total * 100;
                                                                   updateProgress(completeCount.get(), total);
                                                                   String uploadMsg = PROGRESS_MSG_COLLECT + ";" + progress;
                                                                   updateMessage(uploadMsg);
                                                               },
                                                               HashMap::putAll);

        return jpgFileMap;
    }

    private boolean shouldStop() {
        if (stopUploadFlag.get() || jumpToLandFlag.get()) {
            File temp = new File(dirToUpload, Constants.PATH_TEMP_JPG);
            FileUtils.deleteDir(temp);
            return true;
        }
        return false;
    }
}
