package net.paavan.music.content.organizer.downloader.fmw11;

import net.paavan.music.content.organizer.downloader.AlbumDownloader;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadTask;
import net.paavan.music.content.organizer.downloader.beans.DownloadableAlbum;
import net.paavan.music.content.organizer.playlist.FilesystemClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Fmw11CurrentYearAlbumDownloader implements AlbumDownloader {
    private final String allSongsDirectory;
    private final String newSongsDirectory;
    private final Fmw11Client fmw11Client;
    private final FilesystemClient filesystemClient;

    @Inject
    public Fmw11CurrentYearAlbumDownloader(@Named("all.songs.directory") final String allSongsDirectory,
                                           @Named("new.songs.directory") final String newSongsDirectory,
                                           final Fmw11Client fmw11Client, final FilesystemClient filesystemClient) {
        this.allSongsDirectory = allSongsDirectory;
        this.newSongsDirectory = newSongsDirectory;
        this.fmw11Client = fmw11Client;
        this.filesystemClient = filesystemClient;
    }

    @Override
    public void download() {
        List<AvailableAlbum> availableAlbums = fmw11Client.getAvailableAlbums().stream()
                .filter(this::shouldDownload)
                .collect(Collectors.toList());

        List<String> existingAlbums = getExistingAlbums();
        List<AvailableAlbum> albumsToDownload = availableAlbums.stream()
//                .filter(availableAlbum -> !existingAlbums.stream()
//                        .filter(existingAlbumName -> existingAlbumName.contains(availableAlbum.getTitle()))
//                        .findAny()
//                        .isPresent()
//                )
                .filter(availableAlbum -> !existingAlbums.contains(availableAlbum.getDisplayTitle()))
                .collect(Collectors.toList());
        System.out.println(albumsToDownload.stream().map(AvailableAlbum::toString).collect(Collectors.joining("\n")));
        System.out.println(getDownloadPath().toString());

        albumsToDownload.stream()
                .map(fmw11Client::getDownloadableAlbum)
                ;
    }

    private boolean shouldDownload(final AvailableAlbum availableAlbum) {
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

    private Path getDownloadPath() {
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

    private List<DownloadTask> getDownloadTasksForDownloadableAlbum(final DownloadableAlbum downloadableAlbum) {
        return downloadableAlbum.getSongs().stream()
                .map(albumSong -> DownloadTask.builder()
                        .sourceUrl(albumSong.getDownloadUrl())
                        .destinationDirectory(null)
                        .albumName(downloadableAlbum.getDisplayTitle())
                        .build())
                .collect(Collectors.toList());
    }
}
