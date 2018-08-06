package net.paavan.music.content.organizer.downloader.executor;

import net.paavan.music.content.organizer.downloader.beans.DownloadTask;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadExecutor {
    private final ExecutorService executorService;

    public DownloadExecutor() {
//        executorService = Executors.newFixedThreadPool(5);
        executorService = Executors.newSingleThreadExecutor();
    }

    public void execute(final List<DownloadTask> downloadTasks) {
        downloadTasks.forEach(downloadTask -> executorService.submit(() -> {

        }));
    }
}
