package net.paavan.music.content.organizer.downloader.executor;

import lombok.extern.slf4j.Slf4j;
import net.paavan.music.content.organizer.downloader.beans.DownloadExecutionResult;
import net.paavan.music.content.organizer.downloader.beans.DownloadTask;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import static net.paavan.music.content.organizer.downloader.beans.DownloadExecutionResult.DownloadStatus.ALREADY_EXISTS;
import static net.paavan.music.content.organizer.downloader.beans.DownloadExecutionResult.DownloadStatus.FAILURE;
import static net.paavan.music.content.organizer.downloader.beans.DownloadExecutionResult.DownloadStatus.SUCCESSFUL;

@Slf4j
class DownloadRunner implements Callable<DownloadExecutionResult> {
    private final DownloadTask downloadTask;

    public DownloadRunner(final DownloadTask downloadTask) {
        this.downloadTask = downloadTask;
    }

    @Override
    public DownloadExecutionResult call() {
        DownloadExecutionResult.DownloadExecutionResultBuilder resultBuilder = DownloadExecutionResult.builder()
                .startTime(System.currentTimeMillis())
                .downloadTask(downloadTask);

        if (Files.notExists(downloadTask.getDestinationCollectionPath())) {
            try {
                Files.createDirectory(downloadTask.getDestinationCollectionPath());
            } catch (final FileAlreadyExistsException e) {
                // do nothing
            } catch (final IOException e) {
                log.error("Unable to create destination collection path at " + downloadTask.getDestinationCollectionPath(), e);
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
                log.error("Unable to create album path at " + albumPath, e);
                return resultBuilder
                        .downloadStatus(FAILURE)
                        .endTime(System.currentTimeMillis())
                        .build();
            }
        }

        Path filePath = Paths.get(albumPath.toString() + "/" + downloadTask.getFileName());

        if (Files.exists(filePath)) {
            log.info(String.format("Skipping %s --- %s ", downloadTask.getAlbumName(), downloadTask.getFileName()));
            return resultBuilder
                    .downloadStatus(ALREADY_EXISTS)
                    .endTime(System.currentTimeMillis())
                    .build();
        }

        try {
            log.info(String.format("Downloading %s --- %s ", downloadTask.getAlbumName(), downloadTask.getFileName()));
            FileUtils.copyURLToFile(new URL(downloadTask.getSourceUrl()), new File(filePath.toString()));
        } catch (IOException e) {
            log.error("Failed to download file from " + downloadTask.getSourceUrl(), e);
            return resultBuilder
                    .downloadStatus(FAILURE)
                    .endTime(System.currentTimeMillis())
                    .build();
        }

        return resultBuilder
                .downloadStatus(SUCCESSFUL)
                .endTime(System.currentTimeMillis())
                .build();
    }
}
