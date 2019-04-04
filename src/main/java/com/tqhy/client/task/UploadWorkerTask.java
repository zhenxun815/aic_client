package com.tqhy.client.task;

import com.google.gson.Gson;
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
import java.io.IOException;
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

    @Override
    protected Object call() throws Exception {

        completeCount = new AtomicInteger(0);
        total = FileUtils.getFilesInDir(dirToUpload).size();

        File[] caseDirs = dirToUpload.listFiles(File::isDirectory);
        for (File caseDir : caseDirs) {
            if (jumpToLandFlag.get()) {
                break;
            }
            HashMap<String, String> map = new HashMap<>();
            map.put("caseName", caseDir.getName());
            map.put("token", "test");
            map.put("projectId", uploadMsg.getProjectId());
            map.put("batchNumber", uploadMsg.getBatchNumber());
            Map<String, RequestBody> requestParamMap = NetworkUtils.createRequestParamMap(map);


            upLoad(caseDir, requestParamMap);
            //fakeUpload(filesToUpload);
        }

        return null;
    }


    private void upLoad(File caseDir, Map<String, RequestBody> requestParamMap) {
        List<File> filesInCaseDir = FileUtils.getFilesInDir(caseDir);
        List<File> transformedFiles = FileUtils.transAllToJpg(filesInCaseDir);
        AtomicInteger dirUploadCompleteCount = new AtomicInteger(0);
        for (File file : transformedFiles) {
            if (jumpToLandFlag.get()) {
                break;
            }
            MultipartBody.Part filePart = NetworkUtils.createFilePart("file", file.getAbsolutePath());
            Network.getAicApi()
                   .uploadFiles(requestParamMap, filePart)
                   .observeOn(Schedulers.io())
                   .subscribeOn(Schedulers.trampoline())
                   .blockingSubscribe(new Observer<ResponseBody>() {
                       @Override
                       public void onSubscribe(Disposable d) {
                           logger.info("Disposable: " + d);
                       }

                       @Override
                       public void onNext(ResponseBody responseBody) {

                           GsonUtils.parseResponseToObj(responseBody, ClientMsg.class)
                                    .ifPresent(clientMsg -> {
                                        Integer flag = clientMsg.getFlag();
                                        if (203 == flag) {
                                            jumpToLandFlag.set(true);
                                        }
                                    });
                       }

                       @Override
                       public void onError(Throwable e) {
                           updateMessage(PROGRESS_MSG_ERROR);
                           e.printStackTrace();
                       }

                       @Override
                       public void onComplete() {
                           completeCount.incrementAndGet();
                           int dirCompleteCount = dirUploadCompleteCount.incrementAndGet();
                           updateProgress(completeCount.get(), total);
                           double progress = (completeCount.get() + 0D) / total * 100;
                           logger.info("complete count is: " + completeCount.get() + ", progress is: " + progress);
                           DecimalFormat decimalFormat = new DecimalFormat("#0.0");
                           String formatProgress = decimalFormat.format(progress);
                           updateMessage(progress == 100.0D ? PROGRESS_MSG_COMPLETE : formatProgress);

                           //上传完毕删除生成的jpg临时文件
                           if (dirCompleteCount == filesInCaseDir.size()) {
                               File temp = new File(caseDir, "TQHY_TEMP");
                               FileUtils.deleteDir(temp);
                           }
                       }
                   });
        }
    }

    private AtomicInteger fakeUpload(List<File> filesToUpload) {
        logger.info("into fakeUpload...");
        AtomicInteger completeCount = new AtomicInteger(0);
        int total = filesToUpload.size();
        filesToUpload.forEach(file ->
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
                                                        updateMessage(PROGRESS_MSG_ERROR);
                                                        e.printStackTrace();
                                                    }

                                                    @Override
                                                    public void onComplete() {
                                                        completeCount.incrementAndGet();
                                                        updateProgress(completeCount.get(), total);
                                                        double progress = (completeCount.get() + 0D) / total * 100;
                                                        logger.info("complete count is: " + completeCount.get() + ", progress is: " + progress);
                                                        updateMessage(progress == 100.0D ? PROGRESS_MSG_COMPLETE : DecimalFormat.getInstance().format(progress));
                                                    }
                                                }));

        return completeCount;
    }
}
