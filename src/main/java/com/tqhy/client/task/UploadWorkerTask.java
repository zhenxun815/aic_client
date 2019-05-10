package com.tqhy.client.task;

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

    @NonNull
    File dirToUpload;
    @NonNull
    UploadMsg uploadMsg;

    int total;
    AtomicInteger completeCount;
    AtomicInteger errorCount;

    @Override
    protected Object call() throws Exception {
        logger.info("start upload task...");
        completeCount = new AtomicInteger(0);
        errorCount = new AtomicInteger(0);
        total = FileUtils.getFilesInSubDir(dirToUpload).size();
        if (total == 0) {
            logger.info("total file count is 0!");
            updateProgress(100, 100);
            String completeMsg = PROGRESS_MSG_COMPLETE + ";" + completeCount.get() + ";" + errorCount.get();
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
            if (jumpToLandFlag.get()) {
                break;
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
        List<File> filesInCaseDir = FileUtils.getFilesInDir(caseDir);
        List<File> transformedFiles = FileUtils.transAllToJpg(filesInCaseDir);

        if (transformedFiles.size() > 0) {
            AtomicInteger dirUploadCompleteCount = new AtomicInteger(0);
            for (File file : transformedFiles) {
                logger.info("start upload file: " + file.getAbsolutePath());
                if (jumpToLandFlag.get()) {
                    break;
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
                                              dirUploadCompleteCount.incrementAndGet();
                                              errorCount.incrementAndGet();
                                              e.printStackTrace();
                                          }

                                          @Override
                                          public void onComplete() {
                                              completeCount.incrementAndGet();
                                              int dirCompleteCount = dirUploadCompleteCount.incrementAndGet();
                                              updateProgress(completeCount.get(), total);

                                              double progress = (completeCount.get() + errorCount.get() + 0D) / total * 100;
                                              logger.info("complete count is: " + completeCount.get() + ", progress is: " + progress);
                                              DecimalFormat decimalFormat = new DecimalFormat("#0.0");
                                              String formatProgress = decimalFormat.format(progress);
                                              String completeMsg = PROGRESS_MSG_COMPLETE + ";" + completeCount.get() + ";" + errorCount.get();
                                              updateMessage(progress == 100.0D ? completeMsg : formatProgress);

                                              //上传完毕删除生成的jpg临时文件
                                              if (dirCompleteCount == filesInCaseDir.size()) {
                                                  File temp = new File(caseDir, "TQHY_TEMP");
                                                  FileUtils.deleteDir(temp);
                                              }
                                          }

                                      });
            }
        }
    }

    private AtomicInteger fakeUpload(File caseDir) {
        String libPath = System.getProperty("java.library.path");
        logger.info("lib path: is: " + libPath);

        List<File> filesInCaseDir = FileUtils.getFilesInDir(caseDir);
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
                                                           errorCount.incrementAndGet();
                                                           e.printStackTrace();
                                                       }

                                                       @Override
                                                       public void onComplete() {
                                                           completeCount.incrementAndGet();
                                                           updateProgress(completeCount.get(), total);
                                                           double progress = (completeCount.get() + errorCount.get() + 0D) / total * 100;
                                                           logger.info("complete count is: " + completeCount.get() + ", progress is: " + progress);
                                                           updateMessage(progress == 100.0D ? PROGRESS_MSG_COMPLETE : DecimalFormat.getInstance().format(progress));
                                                       }
                                                   }));

        return completeCount;
    }
}
