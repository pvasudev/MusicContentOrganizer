package net.paavan.music.content.organizer.downloader.fmw11;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.paavan.music.content.organizer.downloader.AlbumDownloader;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadExecutionResult;
import net.paavan.music.content.organizer.downloader.beans.DownloadTask;
import net.paavan.music.content.organizer.downloader.beans.DownloadableAlbum;
import net.paavan.music.content.organizer.downloader.executor.DownloadExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.paavan.music.content.organizer.downloader.beans.DownloadExecutionResult.DownloadStatus.FAILURE;

@Slf4j
public class Fmw11CurrentYearAlbumDownloader implements AlbumDownloader {
    private static final String MOVIES_PAGE_URL = "http://www.apunkabollywood.com/browser/category/view/347/movies";
    private static final String ARCHIVE_URL = "https://web.archive.org/web/20181112083533/http://www.apunkabollywood.com/browser/category/view/347/movies";

    private final String allSongsDirectory;
    private final String newSongsDirectory;
    private final String transferSongsDirectory;
    private final Fmw11OldWebpageClient fmw11OldWebpageClient;
    private final DownloadExecutor downloadExecutor;

    @Inject
    public Fmw11CurrentYearAlbumDownloader(@Named("all.songs.directory") final String allSongsDirectory,
                                           @Named("new.songs.directory") final String newSongsDirectory,
                                           @Named("transfer.songs.directory") final String transferSongsDirectory,
                                           final Fmw11OldWebpageClient fmw11OldWebpageClient, final DownloadExecutor downloadExecutor) {
        this.allSongsDirectory = allSongsDirectory;
        this.newSongsDirectory = newSongsDirectory;
        this.transferSongsDirectory = transferSongsDirectory;
        this.fmw11OldWebpageClient = fmw11OldWebpageClient;
        this.downloadExecutor = downloadExecutor;
    }

    @Override
    public void download() {
        List<AvailableAlbum> albums = fmw11OldWebpageClient.getAlbumsOnPage(MOVIES_PAGE_URL).stream()
                .filter(this::isAlbumYearCurrent)
                .collect(Collectors.toList());

        List<AvailableAlbum> oldAlbums = fmw11OldWebpageClient.getAlbumsOnPage(ARCHIVE_URL).stream()
                .filter(this::isAlbumYearCurrent)
                .collect(Collectors.toList());

        List<String> existingAlbums = getExistingAlbums();

        List<AvailableAlbum> albumsToDownload = albums.stream()
                // Only new albums that were not in the archived page
                .filter(availableAlbum -> !oldAlbums.stream()
                        .map(AvailableAlbum::getDisplayTitle)
                        .anyMatch(displayTitle -> displayTitle.equals(availableAlbum.getDisplayTitle())))
                // Should not already exist
                .filter(availableAlbum -> !existingAlbums.contains(availableAlbum.getDisplayTitle()))
                .collect(Collectors.toList());

        if (albumsToDownload.isEmpty()) {
            log.info("No new albums");
            return;
        }

        Path destinationNewSongsCollectionPath = getDestinationNewSongsCollectionPath();
        log.info("Download directory: " + destinationNewSongsCollectionPath.toString());

        Map<String, List<DownloadTask>> downloadTasksByAlbumName = albumsToDownload.stream()
                .map(fmw11OldWebpageClient::getDownloadableAlbum)
                .map(downloadableAlbum -> getDownloadTasksForDownloadableAlbum(downloadableAlbum, destinationNewSongsCollectionPath))
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(DownloadTask::getAlbumName));

        List<DownloadTask> downloadTasks = downloadTasksByAlbumName.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        log.info(String.format("Found %d albums and %d songs to download", albumsToDownload.size(), downloadTasks.size()));

        List<DownloadExecutionResult> results = downloadExecutor.execute(downloadTasks);
        handleErrorsIfAny(results, destinationNewSongsCollectionPath);

        copyFiles(destinationNewSongsCollectionPath, Paths.get(transferSongsDirectory));
        copyFiles(destinationNewSongsCollectionPath, Paths.get(allSongsDirectory));
    }

    // --------------
    // Helper Methods

    private boolean isAlbumYearCurrent(final AvailableAlbum availableAlbum) {
        return availableAlbum.getYear().equals(Calendar.getInstance().get(Calendar.YEAR));
    }

    private List<String> getExistingAlbums() {
        List<String> existingAlbums;
        try (Stream<Path> paths = Files.list(Paths.get(allSongsDirectory))) {
            existingAlbums = paths.filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());

        } catch (final IOException e) {
            log.error("Unable to read allSongsDirectory", e);
            throw new RuntimeException(e);
        }
        return existingAlbums;
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

    private List<DownloadTask> getDownloadTasksForDownloadableAlbum(final DownloadableAlbum downloadableAlbum,
                                                                    final Path destinationNewSongsCollectionPath) {
        return downloadableAlbum.getSongs().stream()
                .map(albumSong -> DownloadTask.builder()
                        .sourceUrl(albumSong.getDownloadUrl())
                        .destinationCollectionPath(destinationNewSongsCollectionPath)
                        .albumName(downloadableAlbum.getDisplayTitle())
                        .fileName(getFilenameFromDownloadUrl(albumSong.getDownloadUrl()))
                        .build())
                .collect(Collectors.toList());
    }

    private String getFilenameFromDownloadUrl(final String downloadUrl) {
        String encodedUrl = URLEncoder.encode(downloadUrl, StandardCharsets.UTF_8);
        String uri = URI.create(encodedUrl).getPath();
        String decodedUrl = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        return FilenameUtils.getName(decodedUrl);
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

        failedTasksByAlbumName.entrySet().stream()
                .forEach(entry -> log.error(String.format(
                        "The following download tasks from album %s failed. The album will be deleted.: \n%s",
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(DownloadTask::getSourceUrl)
                                .collect(Collectors.joining("\n\t")))));
        failedTasksByAlbumName.keySet().stream()
                .forEach(albumName -> deleteDirectory(Paths.get(destinationNewSongsCollectionPath.toString(), albumName)));
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

    private void deleteDirectory(final Path directoryPath) {
        log.info("Deleting directory " + directoryPath);
        Failsafe
                .with(new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofSeconds(2l))
                        .withMaxAttempts(5))
                .run(() -> FileUtils.deleteDirectory(new File(directoryPath.toString())));
    }
}
