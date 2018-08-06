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
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentLinkedQueue;
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
        ConcurrentLinkedQueue<Long> timePerTask = new ConcurrentLinkedQueue<>();
        downloadTasks.forEach(downloadTask -> executorService.submit(new DownloadRunner(downloadTask, timePerTask)));

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

        OptionalDouble average = timePerTask.stream()
                .mapToLong(value -> value)
                .average();
        System.out.println(String.format("Total download time: %d average download time: %.02f",
                System.currentTimeMillis() - startTime,
                average.isPresent() ? average.getAsDouble() : 0));
    }

    private class DownloadRunner implements Runnable {
        private final DownloadTask downloadTask;
        private final ConcurrentLinkedQueue<Long> timePerTask;

        DownloadRunner(final DownloadTask downloadTask, final ConcurrentLinkedQueue<Long> timePerTask) {
            this.downloadTask = downloadTask;
            this.timePerTask = timePerTask;
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
                System.out.println(String.format("Downloading %s --- %s ", downloadTask.getAlbumName(), downloadTask.getFileName()));
                FileUtils.copyURLToFile(new URL(downloadTask.getSourceUrl()),
                        new File(albumPath.toString() + "/" + downloadTask.getFileName()));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            timePerTask.add(System.currentTimeMillis() - taskStartTime);
        }
    }
}
