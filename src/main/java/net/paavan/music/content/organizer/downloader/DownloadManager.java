package net.paavan.music.content.organizer.downloader;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.paavan.music.content.organizer.downloader.beans.DownloadExecutionResult;
import net.paavan.music.content.organizer.downloader.beans.DownloadTask;
import net.paavan.music.content.organizer.downloader.executor.DownloadExecutor;
import org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.paavan.music.content.organizer.downloader.beans.DownloadExecutionResult.DownloadStatus.FAILURE;

@Slf4j
public class DownloadManager {
    private final boolean executeDownloader;
    private final String allSongsDirectory;
    private final String newSongsDirectory;
    private final String transferSongsDirectory;

    private final Set<AlbumDownloadTaskProvider> albumDownloadTaskProviders;
    private final DownloadExecutor downloadExecutor;

    @Inject
    public DownloadManager(@Named("execute.downloader") final boolean executeDownloader,
                           @Named("all.songs.directory") String allSongsDirectory,
                           @Named("new.songs.directory") String newSongsDirectory,
                           @Named("transfer.songs.directory") String transferSongsDirectory,
                           final Set<AlbumDownloadTaskProvider> albumDownloadTaskProviders,
                           final DownloadExecutor downloadExecutor) {
        this.executeDownloader = executeDownloader;
        this.allSongsDirectory = allSongsDirectory;
        this.newSongsDirectory = newSongsDirectory;
        this.transferSongsDirectory = transferSongsDirectory;
        this.albumDownloadTaskProviders = albumDownloadTaskProviders;
        this.downloadExecutor = downloadExecutor;
    }

    public void download() {
        if (executeDownloader) {
            executeDownloads();
        }
    }

    // --------------
    // Helper Methods

    private void executeDownloads() {
        Path destinationNewSongsCollectionPath = getDestinationNewSongsCollectionPath();
        log.info("Download directory: " + destinationNewSongsCollectionPath);

        List<DownloadTask> downloadTasks = albumDownloadTaskProviders.stream()
                .map(albumDownloadTaskProvider -> albumDownloadTaskProvider.getDownloadTasks(destinationNewSongsCollectionPath))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<DownloadExecutionResult> results = downloadExecutor.execute(downloadTasks);
        handleErrorsIfAny(results, destinationNewSongsCollectionPath);

        copyFiles(destinationNewSongsCollectionPath, Paths.get(transferSongsDirectory));
        copyFiles(destinationNewSongsCollectionPath, Paths.get(allSongsDirectory));
    }

    private void handleErrorsIfAny(List<DownloadExecutionResult> results, Path destinationNewSongsCollectionPath) {
        Map<String, List<DownloadTask>> failedTasksByAlbumName = results.stream()
                .filter(downloadExecutionResult -> downloadExecutionResult.getDownloadStatus() == FAILURE ||
                        downloadExecutionResult.getDownloadStatus() == null)
                .map(DownloadExecutionResult::getDownloadTask)
                .collect(Collectors.groupingBy(DownloadTask::getAlbumName));

        if (!failedTasksByAlbumName.isEmpty()) {
            log.error("There were " + failedTasksByAlbumName.size() + " failed download tasks");
        }

        Set<String> allAlbums = results.stream()
                .map(DownloadExecutionResult::getDownloadTask)
                .map(DownloadTask::getAlbumName)
                .collect(Collectors.toSet());

        if (failedTasksByAlbumName.keySet().containsAll(allAlbums)) {
            log.error("No download albums were successful.");
            deleteDirectory(destinationNewSongsCollectionPath);
            return;
        }

        failedTasksByAlbumName.forEach((key, value) -> log.error(String.format(
                "The following download tasks from album %s failed. The album will be deleted.: \n%s", key,
                value.stream()
                        .map(DownloadTask::getSourceUrl)
                        .collect(Collectors.joining("\n\t")))));
        failedTasksByAlbumName.keySet()
                .forEach(albumName -> deleteDirectory(Paths.get(destinationNewSongsCollectionPath.toString(), albumName)));
    }


    private void deleteDirectory(final Path directoryPath) {
        log.info("Deleting directory " + directoryPath);
        Failsafe
                .with(new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofSeconds(2L))
                        .withMaxAttempts(5))
                .run(() -> FileUtils.deleteDirectory(new File(directoryPath.toString())));
    }

    private Path getDestinationNewSongsCollectionPath() {
        List<String> albumCollectionDirectoryNames;
        try (Stream<Path> paths = Files.list(Paths.get(newSongsDirectory))) {
            albumCollectionDirectoryNames = paths.filter(Files::isDirectory)
                    .filter(path -> !path.endsWith("NewSongs"))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Unable to read newSongsDirectory", e);
            throw new RuntimeException(e);
        }

        int[] integerDirectoryNames = albumCollectionDirectoryNames.stream()
                .map(s -> {
                    try {
                        return Integer.valueOf(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .toArray();

        Arrays.sort(integerDirectoryNames);
        int nextDirectoryIndex = integerDirectoryNames[integerDirectoryNames.length - 1] + 1;
        return Paths.get(newSongsDirectory + "/" + nextDirectoryIndex);
    }

    private void copyFiles(final Path sourcePath, final Path destinationPath) {
        if (!Files.exists(sourcePath)) {
            log.info("Source path " + sourcePath + " does not exist. Skipping copy.");
            return;
        }
        log.info("Coping files from " + sourcePath + " to " + destinationPath);
        try {
            FileUtils.copyDirectory(new File(sourcePath.toString()), new File(destinationPath.toString()));
        } catch (IOException e) {
            log.error("Unable to copy files from " + sourcePath + " to " + destinationPath, e);
            throw new RuntimeException(e);
        }
    }
}
