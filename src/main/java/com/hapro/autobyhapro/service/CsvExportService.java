package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.CsvExportResult;
import com.hapro.autobyhapro.entity.PageSimpleStats;
import com.hapro.autobyhapro.entity.VideoExportRow;
import com.hapro.autobyhapro.repository.CsvExportRepository;
import com.hapro.autobyhapro.repository.SimpleStatsRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvExportService {

    private static final DateTimeFormatter FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final SimpleStatsRepository simpleStatsRepository = new SimpleStatsRepository();
    private final CsvExportRepository csvExportRepository = new CsvExportRepository();

    public CsvExportResult exportPageStatsCsv() {
        List<PageSimpleStats> statsList = simpleStatsRepository.findPageSimpleStats();

        String fileName = "page_stats_"
                + LocalDateTime.now().format(FILE_TIME_FORMAT)
                + ".csv";

        Path outputFile = AppPaths.EXPORTS_DIR.resolve(fileName);

        StringBuilder builder = new StringBuilder();

        builder.append('\uFEFF');
        builder.append("STT,Page Code,Page Name,Source Code,Source Name,Source Type,Start Date,Total Downloaded,Ready Upload,Uploaded Marked,Uploaded Deleted,Uploaded Total,Working Days")
                .append(System.lineSeparator());

        for (int index = 0; index < statsList.size(); index++) {
            PageSimpleStats stats = statsList.get(index);

            builder.append(index + 1).append(",");
            builder.append(csv(stats.getPageCode())).append(",");
            builder.append(csv(stats.getPageName())).append(",");
            builder.append(csv(stats.getSourceCode())).append(",");
            builder.append(csv(stats.getSourceName())).append(",");
            builder.append(csv(stats.getSourceType())).append(",");
            builder.append(csv(stats.getStartDate())).append(",");
            builder.append(stats.getTotalDownloadedCount()).append(",");
            builder.append(stats.getReadyUploadCount()).append(",");
            builder.append(stats.getUploadedMarkedCount()).append(",");
            builder.append(stats.getUploadedDeletedCount()).append(",");
            builder.append(stats.getUploadedTotalCount()).append(",");
            builder.append(stats.getWorkingDays());
            builder.append(System.lineSeparator());
        }

        writeCsv(outputFile, builder.toString());

        return new CsvExportResult(
                outputFile.toAbsolutePath().toString(),
                statsList.size(),
                "Đã xuất thống kê fanpage ra CSV."
        );
    }

    public CsvExportResult exportVideosCsv() {
        List<VideoExportRow> videos = csvExportRepository.findAllVideosForExport();

        String fileName = "videos_"
                + LocalDateTime.now().format(FILE_TIME_FORMAT)
                + ".csv";

        Path outputFile = AppPaths.EXPORTS_DIR.resolve(fileName);

        StringBuilder builder = new StringBuilder();

        builder.append('\uFEFF');
        builder.append("STT,Video DB ID,Page Code,Page Name,Source Code,Source Name,Source Type,Platform Video ID,Title,Status,Downloaded Time,Original URL")
                .append(System.lineSeparator());

        for (int index = 0; index < videos.size(); index++) {
            VideoExportRow video = videos.get(index);

            builder.append(index + 1).append(",");
            builder.append(video.getVideoId()).append(",");
            builder.append(csv(video.getPageCode())).append(",");
            builder.append(csv(video.getPageName())).append(",");
            builder.append(csv(video.getSourceCode())).append(",");
            builder.append(csv(video.getSourceName())).append(",");
            builder.append(csv(video.getSourceType())).append(",");
            builder.append(csv(video.getPlatformVideoId())).append(",");
            builder.append(csv(video.getTitle())).append(",");
            builder.append(csv(video.getStatus())).append(",");
            builder.append(csv(video.getDownloadedTime())).append(",");
            builder.append(csv(video.getOriginalUrl()));
            builder.append(System.lineSeparator());
        }

        writeCsv(outputFile, builder.toString());

        return new CsvExportResult(
                outputFile.toAbsolutePath().toString(),
                videos.size(),
                "Đã xuất danh sách video ID ra CSV."
        );
    }

    private void writeCsv(Path outputFile, String content) {
        try {
            Files.createDirectories(AppPaths.EXPORTS_DIR);
            Files.writeString(outputFile, content, StandardCharsets.UTF_8);

        } catch (IOException exception) {
            throw new RuntimeException("Không thể ghi file CSV.", exception);
        }
    }

    private String csv(String text) {
        if (text == null) {
            text = "";
        }

        String clean = text.replace("\"", "\"\"");

        return "\"" + clean + "\"";
    }
}