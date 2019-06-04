package com.tqhy.client.task;

import com.tqhy.client.config.Constants;
import com.tqhy.client.models.entity.Report;
import com.tqhy.client.models.entity.Reports;
import com.tqhy.client.models.entity.Title;
import com.tqhy.client.models.msg.BaseMsg;
import com.tqhy.client.models.msg.local.SaveDataMsg;
import com.tqhy.client.utils.FileUtils;
import com.tqhy.client.utils.GsonUtils;
import io.reactivex.Observable;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * @author Yiheng
 * @create 6/4/2019
 * @since 1.0.0
 */
@Getter
@Setter
@RequiredArgsConstructor(staticName = "of")
public class SaveFileTask implements Callable<Observable<SaveDataMsg>> {

    Logger logger = LoggerFactory.getLogger(SaveFileTask.class);


    @NonNull
    private SaveDataMsg saveDataMsg;

    @Override
    public Observable<SaveDataMsg> call() throws Exception {
        String dataToSave = saveDataMsg.getDataToSave();
        File saveDir = saveDataMsg.getSaveDir();

        switch (saveDataMsg.getSaveType()) {
            case SAVE_REPORT_TO_CSV:
                Optional<Reports> reportsOptional = GsonUtils.parseJsonToObj(dataToSave, Reports.class);

                if (reportsOptional.isPresent()) {
                    Reports reports = reportsOptional.get();
                    String fileName = reports.getFileName();
                    File saveFile = new File(saveDir, fileName + ".csv");
                    String saveDataStr = getReportsSaveString(reports);
                    return Observable.just(saveDataMsg)
                                     .map(msg -> {
                                         FileUtils.writeFile(saveFile, saveDataStr, null, true);
                                         msg.setFlag(BaseMsg.SUCCESS);
                                         return msg;
                                     });
                }
                return genTaskFailObservable();
            default:
                return genTaskFailObservable();
        }
    }

    /**
     * 将{@link Reports}对象转换为保存到csv文件字符串
     *
     * @param reports
     * @return
     */
    private String getReportsSaveString(Reports reports) {
        List<Title> head = reports.getHead();
        List<Report> body = reports.getBody();
        StringBuilder headBuilder = head.stream()
                                        .collect(StringBuilder::new,
                                                 (builder, title) -> builder.append(title.getTitle()).append(Constants.VALUE_SPLITTER),
                                                 StringBuilder::append);

        StringBuilder reportsBuilder = body.stream()
                                           .collect(StringBuilder::new,
                                                    (builder, report) -> {
                                                        builder.append(report.getName())
                                                               .append(Constants.VALUE_SPLITTER)
                                                               .append(report.getValue())
                                                               .append(Constants.VALUE_SPLITTER)
                                                               .append(report.getPer())
                                                               .append(Constants.NEW_LINE);
                                                    },
                                                    StringBuilder::append);

        return headBuilder.append(Constants.NEW_LINE)
                          .append(reportsBuilder)
                          .toString();
    }

    private Observable<SaveDataMsg> genTaskFailObservable() {
        saveDataMsg.setFlag(BaseMsg.FAIL);
        return Observable.just(saveDataMsg);
    }
}
