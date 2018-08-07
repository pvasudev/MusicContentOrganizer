package net.paavan.music.content.organizer.downloader.fmw11;

import net.paavan.music.content.organizer.downloader.AlbumDownloader;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Fmw11CurrentYearAlbumDownloader implements AlbumDownloader {
    private final String allSongsDirectory;
    private final String newSongsDirectory;
    private final String transferSongsDirectory;
    private final Fmw11Client fmw11Client;
    private final DownloadExecutor downloadExecutor;

    @Inject
    public Fmw11CurrentYearAlbumDownloader(@Named("all.songs.directory") final String allSongsDirectory,
                                           @Named("new.songs.directory") final String newSongsDirectory,
                                           @Named("transfer.songs.directory") final String transferSongsDirectory,
                                           final Fmw11Client fmw11Client, final DownloadExecutor downloadExecutor) {
        this.allSongsDirectory = allSongsDirectory;
        this.newSongsDirectory = newSongsDirectory;
        this.transferSongsDirectory = transferSongsDirectory;
        this.fmw11Client = fmw11Client;
        this.downloadExecutor = downloadExecutor;
    }

    @Override
    public void download() {
        List<AvailableAlbum> availableAlbums = fmw11Client.getAvailableAlbums().stream()
                .filter(this::isAlbumYearCurrent)
                .collect(Collectors.toList());

        List<String> existingAlbums = getExistingAlbums();

        List<AvailableAlbum> albumsToDownload = availableAlbums.stream()
                .filter(availableAlbum -> !existingAlbums.contains(availableAlbum.getDisplayTitle()))
                .collect(Collectors.toList());

        if (albumsToDownload.isEmpty()) {
            System.out.println("No new albums");
            return;
        }

        Path destinationNewSongsCollectionPath = getDestinationNewSongsCollectionPath();
        System.out.println("Download directory: " + destinationNewSongsCollectionPath.toString());

        List<DownloadTask> downloadTasks = albumsToDownload.stream()
                .map(fmw11Client::getDownloadableAlbum)
                .map(downloadableAlbum -> getDownloadTasksForDownloadableAlbum(downloadableAlbum, destinationNewSongsCollectionPath))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        System.out.println(String.format("Found %d albums and %d songs to download", albumsToDownload.size(), downloadTasks.size()));

        downloadExecutor.execute(downloadTasks);

        copyFiles(destinationNewSongsCollectionPath, Paths.get(transferSongsDirectory));
        copyFiles(destinationNewSongsCollectionPath, Paths.get(allSongsDirectory));
    }

    // --------------
    // Helper Methods

    private boolean isAlbumYearCurrent(final AvailableAlbum availableAlbum) {
        return availableAlbum.getYear() != null && availableAlbum.getYear().equals(Calendar.getInstance().get(Calendar.YEAR));
    }

    private List<String> getExistingAlbums() {
        List<String> existingAlbums;
        try (Stream<Path> paths = Files.list(Paths.get(allSongsDirectory))) {
            existingAlbums = paths.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().contains(String.valueOf(Calendar.getInstance().get(Calendar.YEAR))))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());

        } catch (final IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
                        .fileName(FilenameUtils.getName(URI.create(albumSong.getDownloadUrl()).getPath()))
                        .build())
                .collect(Collectors.toList());
    }

    private void copyFiles(Path sourcePath, Path destinationPath) {
        try {
            FileUtils.copyDirectory(new File(sourcePath.toString()), new File(destinationPath.toString()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
