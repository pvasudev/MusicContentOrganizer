package net.paavan.music.content.organizer.downloader.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class DownloadExecutionResult {
    public enum DownloadStatus {
        SUCCESSFUL, ALREADY_EXISTS, FAILURE
    }

    private final DownloadTask downloadTask;
    private final long startTime;
    private final long endTime;
    private final DownloadStatus downloadStatus;
}
