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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static String PROGRESS_MSG_ERROR = "error";
    public static String PROGRESS_MSG_COMPLETE = "complete";

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
        total = FileUtils.getFilesInSubDir(dirToUpload, file -> FileUtils.isDcmFile(file) || FileUtils.isJpgFile(file))
                         .size();
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

        HashMap<String, String> map = new HashMap<>();
        map.put("token", token);
        map.put("batchNumber", batchNumber);
        map.put("taskId", uploadMsg.getUploadId());

        File[] caseDirs = dirToUpload.listFiles(File::isDirectory);
        logger.info("upload token: {}, dirToUpload: {}, batchNumber: {}", token, dirPathToUpload, batchNumber);
        upLoadDirs(caseDirs, map);
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

        HashMap<String, String> map = new HashMap<>();
        map.put("token", uploadMsg.getToken());
        map.put("batchNumber", uploadMsg.getBatchNumber());
        map.put("projectId", uploadMsg.getUploadId());
        map.put("remarks", uploadMsg.getRemarks());

        File[] caseDirs = dirToUpload.listFiles(File::isDirectory);
        logger.info("upload token: {}, dirToUpload: {}, batchNumber: {}", token, dirPathToUpload, batchNumber);
        upLoadDirs(caseDirs, map);
    }

    private void upLoadDirs(File[] caseDirs, HashMap<String, String> map) {

        for (File caseDir : caseDirs) {
            if (stopUploadFlag.get() || jumpToLandFlag.get()) {
                return;
            }
            String caseName = caseDir.getName();
            map.put("caseName", caseName);
            Map<String, RequestBody> requestParamMap = NetworkUtils.createRequestParamMap(map);
            doUpLoad(caseDir, requestParamMap);

            //fakeUpload(dirToUpload);
        }
    }

    private void doUpLoad(File caseDir, Map<String, RequestBody> requestParamMap) {
        logger.info("into upload case: " + caseDir.getAbsolutePath());
        List<File> filesInCaseDir = FileUtils.getFilesInDir(caseDir, file -> FileUtils.isJpgFile(file) || FileUtils.isDcmFile(file));
        Map<String, String> originFilesInfoMap = FileUtils.getFilesInfoMap(filesInCaseDir);
        List<File> transformedFiles = FileUtils.transAllToJpg(filesInCaseDir);
        //Map<String, String> transedFilesInfoMap = FileUtils.getFilesInfoMap(transformedFiles);
        if (transformedFiles.size() > 0) {
            AtomicInteger dirUploadCompleteCount = new AtomicInteger(0);
            for (File file : transformedFiles) {
                logger.info("start upload file: " + file.getAbsolutePath());
                if (stopUploadFlag.get() || jumpToLandFlag.get()) {
                    File temp = new File(caseDir, "TQHY_TEMP");
                    FileUtils.deleteDir(temp);
                    return;
                }
                MultipartBody.Part filePart = NetworkUtils.createFilePart("file", file.getAbsolutePath());
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
                                          }

                                          @Override
                                          public void onError(Throwable e) {
                                              failCount.incrementAndGet();
                                              updateUploadStatus();
                                              deleteTempFiles(dirUploadCompleteCount, filesInCaseDir, caseDir);

                                              String[] fileNameSplit = file.getName().split("\\.");
                                              FileUtils.appendFile(uploadInfoFile, originFilesInfoMap.get(fileNameSplit[0]), builder -> builder.append(Constants.NEW_LINE), true);
                                              e.printStackTrace();
                                          }

                                          @Override
                                          public void onComplete() {
                                              successCount.incrementAndGet();
                                              updateUploadStatus();
                                              deleteTempFiles(dirUploadCompleteCount, filesInCaseDir, caseDir);
                                          }

                                      });
            }
        }
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
        updateMessage(progress == 100.0D ? completeMsg : Double.toString(progress));
    }

    /**
     * 删除生成的临时jpg文件
     *
     * @param dirUploadCompleteCount
     * @param filesInCaseDir
     * @param caseDir
     */
    private void deleteTempFiles(AtomicInteger dirUploadCompleteCount, List<File> filesInCaseDir, File caseDir) {

        if (dirUploadCompleteCount.incrementAndGet() == filesInCaseDir.size()) {
            File temp = new File(caseDir, "TQHY_TEMP");
            FileUtils.deleteDir(temp);
        }
    }

    private AtomicInteger fakeUpload(File caseDir) {
        String libPath = System.getProperty("java.library.path");
        logger.info("lib path: is: " + libPath);

        List<File> filesInCaseDir = FileUtils.getFilesInDir(caseDir, file -> FileUtils.isJpgFile(file) || FileUtils.isDcmFile(file));
        List<File> transformedFiles = FileUtils.transAllToJpg(filesInCaseDir);
        logger.info("into fakeUpload...");
        AtomicInteger completeCount = new AtomicInteger(0);
        int total = transformedFiles.size();
        transformedFiles.forEach(file ->
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
}
