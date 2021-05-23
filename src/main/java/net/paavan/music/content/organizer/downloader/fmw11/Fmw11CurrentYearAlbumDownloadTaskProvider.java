package net.paavan.music.content.organizer.downloader.fmw11;

import lombok.extern.slf4j.Slf4j;
import net.paavan.music.content.organizer.downloader.AlbumDownloadTaskProvider;
import net.paavan.music.content.organizer.downloader.DownloadProviderUtils;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadTask;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class Fmw11CurrentYearAlbumDownloadTaskProvider implements AlbumDownloadTaskProvider {
    private static final String MOVIES_PAGE_URL = "https://api.gigahost123.com/api/listS3?bucket=mp3.gigahost123.com&path=%2Fsongs%2FAudio%2Findian%2Fmovies%2F";
    private static final String ARCHIVE_URL = "https://web.archive.org/web/20181112083533/http://www.apunkabollywood.com/browser/category/view/347/movies";

    private final String allSongsDirectory;
    private final ApunKaBollywoodClient apunKaBollywoodClient;
    private final AlbumProviderOldWebpageClient albumProviderOldWebpageClient;

    @Inject
    public Fmw11CurrentYearAlbumDownloadTaskProvider(@Named("all.songs.directory") final String allSongsDirectory,
                                                     final ApunKaBollywoodClient apunKaBollywoodClient,
                                                     final AlbumProviderOldWebpageClient albumProviderOldWebpageClient) {
        this.allSongsDirectory = allSongsDirectory;
        this.apunKaBollywoodClient = apunKaBollywoodClient;
        this.albumProviderOldWebpageClient = albumProviderOldWebpageClient;
    }

    @Override
    public List<DownloadTask> getDownloadTasks(final Path destinationPath) {
        List<AvailableAlbum> albums = apunKaBollywoodClient.getAlbumsOnPage(MOVIES_PAGE_URL).stream()
                .filter(this::isAlbumYearCurrent)
                .collect(Collectors.toList());

        List<AvailableAlbum> oldAlbums = albumProviderOldWebpageClient.getAlbumsOnPage(ARCHIVE_URL).stream()
                .filter(this::isAlbumYearCurrent)
                .collect(Collectors.toList());

        List<String> existingAlbums = DownloadProviderUtils.getExistingAlbums(allSongsDirectory);

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
                .parallel()
                .map(downloadableAlbum -> DownloadProviderUtils.getDownloadTasksForDownloadableAlbum(downloadableAlbum, destinationPath))
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(DownloadTask::getAlbumName));

        List<DownloadTask> downloadTasks = downloadTasksByAlbumName.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        log.info(String.format("Found %d albums and %d songs on Fmw11: \n\t%s\n",
                albumsToDownload.size(), downloadTasks.size(),
                DownloadProviderUtils.getPrintableAlbumsList(albumsToDownload)));

        return downloadTasks;
    }

    // --------------
    // Helper Methods

    private boolean isAlbumYearCurrent(final AvailableAlbum availableAlbum) {
        return availableAlbum.getYear().equals(Calendar.getInstance().get(Calendar.YEAR));
    }
}
