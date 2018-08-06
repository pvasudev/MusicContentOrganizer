package net.paavan.music.content.organizer.downloader.executor;

import net.paavan.music.content.organizer.downloader.beans.DownloadTask;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DownloadExecutor {
    private static final int THREAD_COUNT = 10;
    private static final long TIMEOUT_PER_DOWNLOAD_TASK_SECONDS = 5;

    private final ExecutorService executorService;

    public DownloadExecutor() {
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    public void execute(final List<DownloadTask> downloadTasks) {
        long startTime = System.currentTimeMillis();
        downloadTasks.forEach(downloadTask -> executorService.submit(new DownloadRunner(downloadTask)));

        System.out.println("Current time: " + System.currentTimeMillis());

        try {
            executorService.shutdown();
            long timeout = TIMEOUT_PER_DOWNLOAD_TASK_SECONDS * downloadTasks.size();
            System.out.println("Will wait for time (seconds): " + timeout);
            executorService.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (!executorService.isTerminated()) {
                System.err.println("Canceling unfinished tasks");
                executorService.shutdownNow();
                System.out.println("Executor terminated");
            }
        }

        System.out.println("End Current time: " + System.currentTimeMillis());
        System.out.println("Diff time: " + (System.currentTimeMillis() - startTime));
    }

    private class DownloadRunner implements Runnable {
        private DownloadTask downloadTask;

        DownloadRunner(DownloadTask downloadTask) {
            this.downloadTask = downloadTask;
        }

        @Override
        public void run() {
            long taskStartTime = System.currentTimeMillis();
            if (Files.notExists(downloadTask.getDestinationCollectionPath())) {
                try {
                    Files.createDirectory(downloadTask.getDestinationCollectionPath());
                } catch (final FileAlreadyExistsException e) {
                    // do nothing
                } catch (final IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            Path albumPath = Paths.get(downloadTask.getDestinationCollectionPath().toString(), downloadTask.getAlbumName());

            if (Files.notExists(albumPath)) {
                try {
                    Files.createDirectory(albumPath);
                } catch (final FileAlreadyExistsException e) {
                    // do nothing
                } catch (final IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            try {
                System.out.println("Downloading file: " + downloadTask.getFileName() + " from album " + downloadTask.getAlbumName());
                FileUtils.copyURLToFile(new URL(downloadTask.getSourceUrl()), new File(albumPath.toString() + "/" + downloadTask.getFileName()));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            System.out.println("Task total time: " + (System.currentTimeMillis() - taskStartTime));
        }
    }
}
