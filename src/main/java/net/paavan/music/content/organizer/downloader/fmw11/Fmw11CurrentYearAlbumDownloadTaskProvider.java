package net.paavan.music.content.organizer.downloader.fmw11;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import net.paavan.music.content.organizer.downloader.AlbumDownloadTaskProvider;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadTask;
import net.paavan.music.content.organizer.downloader.beans.DownloadableAlbum;
import org.apache.commons.io.FilenameUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Fmw11CurrentYearAlbumDownloadTaskProvider implements AlbumDownloadTaskProvider {
    private static final String MOVIES_PAGE_URL = "https://api.gigahost123.com/api/listS3?bucket=mp3.gigahost123.com&path=%2Fsongs%2FAudio%2Findian%2Fmovies%2F";
    private static final String ARCHIVE_URL = "https://web.archive.org/web/20181112083533/http://www.apunkabollywood.com/browser/category/view/347/movies";

    private final String allSongsDirectory;
    private final ApunKaBollywoodClient apunKaBollywoodClient;
    private final Fmw11OldWebpageClient fmw11OldWebpageClient;

    @Inject
    public Fmw11CurrentYearAlbumDownloadTaskProvider(@Named("all.songs.directory") final String allSongsDirectory,
                                                     final ApunKaBollywoodClient apunKaBollywoodClient,
                                                     final Fmw11OldWebpageClient fmw11OldWebpageClient) {
        this.allSongsDirectory = allSongsDirectory;
        this.apunKaBollywoodClient = apunKaBollywoodClient;
        this.fmw11OldWebpageClient = fmw11OldWebpageClient;
    }

    @Override
    public List<DownloadTask> getDownloadTasks(final Path destinationPath) {
        List<AvailableAlbum> albums = apunKaBollywoodClient.getAlbumsOnPage(MOVIES_PAGE_URL).stream()
                .filter(this::isAlbumYearCurrent)
                .collect(Collectors.toList());

        List<AvailableAlbum> oldAlbums = fmw11OldWebpageClient.getAlbumsOnPage(ARCHIVE_URL).stream()
                .filter(this::isAlbumYearCurrent)
                .collect(Collectors.toList());

        List<String> existingAlbums = getExistingAlbums();

        List<AvailableAlbum> albumsToDownload = albums.stream()
                // Only new albums that were not in the archived page
                .filter(availableAlbum -> oldAlbums.stream()
                        .map(AvailableAlbum::getDisplayTitle)
                        .noneMatch(displayTitle -> displayTitle.equalsIgnoreCase(availableAlbum.getDisplayTitle())))
                // Should not already exist
                .filter(availableAlbum -> !existingAlbums.contains(availableAlbum.getDisplayTitle()))
                .collect(Collectors.toList());

        if (albumsToDownload.isEmpty()) {
            log.info("No new albums");
            return Collections.emptyList();
        }

        Map<String, List<DownloadTask>> downloadTasksByAlbumName = albumsToDownload.stream()
                .map(apunKaBollywoodClient::getDownloadableAlbum)
                .map(downloadableAlbum -> getDownloadTasksForDownloadableAlbum(downloadableAlbum, destinationPath))
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(DownloadTask::getAlbumName));

        List<DownloadTask> downloadTasks = downloadTasksByAlbumName.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        log.info(String.format("Found the following new albums: \n\t%s\nStarting download of %d albums and %d songs.",
                getPrintableAlbumsList(albumsToDownload), albumsToDownload.size(), downloadTasks.size()));

        return downloadTasks;
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


    private List<DownloadTask> getDownloadTasksForDownloadableAlbum(final DownloadableAlbum downloadableAlbum,
                                                                    final Path destinationNewSongsCollectionPath) {
        return downloadableAlbum.getSongs().stream()
                .map(albumSong -> DownloadTask.builder()
                        .sourceUrl(albumSong.getDownloadUrl())
                        .destinationCollectionPath(destinationNewSongsCollectionPath)
                        .albumName(downloadableAlbum.getDisplayTitle())
                        .fileName(getFilenameFromDownloadUrl(albumSong.getTitle()))
                        .build())
                .collect(Collectors.toList());
    }

    private String getFilenameFromDownloadUrl(final String downloadUrl) {
        String encodedUrl = URLEncoder.encode(downloadUrl, StandardCharsets.UTF_8);
        String uri = URI.create(encodedUrl).getPath();
        String decodedUrl = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        return FilenameUtils.getName(decodedUrl);
    }

    private String getPrintableAlbumsList(final List<AvailableAlbum> albumsToDownload) {
        AtomicInteger counter = new AtomicInteger();
        return Joiner
                .on("\n\t")
                .withKeyValueSeparator(". ")
                .join(albumsToDownload.stream()
                        .map(AvailableAlbum::getDisplayTitle)
                        .collect(Collectors.toMap(s -> counter.incrementAndGet(), Function.identity())));
    }
}
