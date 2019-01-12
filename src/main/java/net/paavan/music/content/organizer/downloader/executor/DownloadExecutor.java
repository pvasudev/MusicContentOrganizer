package net.paavan.music.content.organizer.downloader.executor;

import lombok.extern.slf4j.Slf4j;
import net.paavan.music.content.organizer.downloader.beans.DownloadExecutionResult;
import net.paavan.music.content.organizer.downloader.beans.DownloadTask;

import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.paavan.music.content.organizer.downloader.beans.DownloadExecutionResult.DownloadStatus.SUCCESSFUL;

@Slf4j
public class DownloadExecutor {
    private static final int THREAD_COUNT = 10;
    private static final long TIMEOUT_PER_DOWNLOAD_TASK_SECONDS = 10;

    private final ExecutorService executorService;

    public DownloadExecutor() {
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    public List<DownloadExecutionResult> execute(final List<DownloadTask> downloadTasks) {
        long startTime = System.currentTimeMillis();
        List<Future<DownloadExecutionResult>> futures = downloadTasks.stream()
                .map(downloadTask -> new DownloadRunner(downloadTask))
                .map(downloadRunner -> executorService.submit(downloadRunner))
                .collect(Collectors.toList());

        try {
            executorService.shutdown();
            long timeout = TIMEOUT_PER_DOWNLOAD_TASK_SECONDS * downloadTasks.size();
            log.info("Will wait for time (seconds): " + timeout);
            executorService.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            log.error("Interrupted while waiting for download executor", e);
            throw new RuntimeException(e);
        } finally {
            if (!executorService.isTerminated()) {
                log.error("Canceling unfinished tasks");
                executorService.shutdownNow();
                log.info("Executor terminated");
            }
        }

        List<DownloadExecutionResult> results = getDownloadExecutionResults(futures);
        printDownloadStats(results, System.currentTimeMillis() - startTime);

        return results;
    }

    // --------------
    // Helper Methods

    private List<DownloadExecutionResult> getDownloadExecutionResults(final List<Future<DownloadExecutionResult>> futures) {
        return futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("Unable to get Future results", e);
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
    }

    private void printDownloadStats(List<DownloadExecutionResult> results, long totalDownloadTime) {
        List<DownloadExecutionResult> successfulResults = results.stream()
                .filter(downloadExecutionResult -> downloadExecutionResult.getDownloadStatus() == SUCCESSFUL)
                .collect(Collectors.toList());
        OptionalDouble average = successfulResults.stream()
                .map(result -> result.getEndTime() - result.getStartTime())
                .mapToLong(Long::longValue)
                .average();

        log.info(String.format("Items downloaded: %d. Total download time: %d average download time: %.02f.",
                successfulResults.size(),
                totalDownloadTime,
                average.isPresent() ? average.getAsDouble() : 0));
    }
}
