package com.tqhy.client.task;

import com.tqhy.client.models.msg.BaseMsg;
import com.tqhy.client.models.msg.local.DownloadMsg;
import com.tqhy.client.network.Network;
import io.reactivex.Observable;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import okio.BufferedSink;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Yiheng
 * @create 6/4/2019
 * @since 1.0.0
 */
@Getter
@Setter
@RequiredArgsConstructor(staticName = "of")
public class DownloadTask implements Callable<Observable<DownloadMsg>> {

    Logger logger = LoggerFactory.getLogger(DownloadMsg.class);


    @NonNull
    private DownloadMsg downloadMsg;

    @Override
    public Observable<DownloadMsg> call() throws Exception {
        Map<String, String> requestParamMap = downloadMsg.getRequestParamMap();
        switch (downloadMsg.getDownloadTaskApi()) {
            case DOWNLOAD_PDF:
                return downloadPdf(requestParamMap);
            default:
                return Observable.just(downloadMsg);
        }

    }

    private Observable<DownloadMsg> downloadPdf(Map<String, String> requestParamMap) {
        String imgUrlStr = requestParamMap.get("imgUrlString");
        String saveFileDir = requestParamMap.get("saveFileDir");
        return Network.getAicApi()
                      .download(imgUrlStr)
                      .map(response -> {
                          String header = response.headers().get("Content-Disposition");
                          logger.info("header is {}", header);
                          String[] split = header.split("filename=");
                          String fileName = split[1];
                          File file = new File(saveFileDir, fileName);

                          BufferedSink sink = null;
                          try {
                              sink = Okio.buffer(Okio.sink(file));
                              sink.writeAll(response.body().source());
                              sink.close();
                              downloadMsg.setFlag(BaseMsg.SUCCESS);
                              return downloadMsg;
                          } catch (FileNotFoundException e) {
                              e.printStackTrace();
                          } catch (IOException e) {
                              e.printStackTrace();
                          }
                          downloadMsg.setFlag(BaseMsg.FAIL);
                          return downloadMsg;
                      });

    }
}
