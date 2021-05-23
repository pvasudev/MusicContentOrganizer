package net.paavan.music.content.organizer.downloader.pagalsong;

import lombok.extern.slf4j.Slf4j;
import net.paavan.music.content.organizer.downloader.AlbumDownloadTaskProvider;
import net.paavan.music.content.organizer.downloader.DownloadProviderUtils;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadTask;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class PagalSongDownloadTaskProvider implements AlbumDownloadTaskProvider {
    private static final String MOVIES_PAGE_URL = "https://pagalsong.in/bollywood-mp3-songs-%d-subcategory.html?page=%d";

    private final String allSongsDirectory;
    private final PagalSongClient pagalSongClient;

    @Inject
    public PagalSongDownloadTaskProvider(@Named("all.songs.directory") final String allSongsDirectory,
                                         final PagalSongClient pagalSongClient) {
        this.allSongsDirectory = allSongsDirectory;
        this.pagalSongClient = pagalSongClient;
    }

    @Override
    public List<DownloadTask> getDownloadTasks(final Path destinationPath) {
        List<AvailableAlbum> availableAlbums = getAvailableAlbumsFromSubpages();
        List<String> existingAlbums = DownloadProviderUtils.getExistingAlbums(allSongsDirectory);

        List<AvailableAlbum> albumsToDownload = availableAlbums.stream()
                // Should not already exist
                .filter(availableAlbum -> !existingAlbums.contains(availableAlbum.getDisplayTitle()))
                .collect(Collectors.toList());

        log.info(String.format("Found %d total albums. After removing existing albums, total is %d",
                availableAlbums.size(), albumsToDownload.size()));

        if (albumsToDownload.isEmpty()) {
            log.info("No new albums");
            return Collections.emptyList();
        }

        Map<String, List<DownloadTask>> downloadTasksByAlbumName = albumsToDownload.stream()
                .map(pagalSongClient::getDownloadableAlbum)
                .map(downloadableAlbum -> DownloadProviderUtils.getDownloadTasksForDownloadableAlbum(downloadableAlbum, destinationPath))
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(DownloadTask::getAlbumName));

        List<DownloadTask> downloadTasks = downloadTasksByAlbumName.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        log.info(String.format("Found %d albums and %d songs on PagalSong: \n\t%s\n",
                albumsToDownload.size(), downloadTasks.size(),
                DownloadProviderUtils.getPrintableAlbumsList(albumsToDownload)));

        return downloadTasks;
    }

    // --------------
    // Helper Methods

    private List<AvailableAlbum> getAvailableAlbumsFromSubpages() {
        List<AvailableAlbum> availableAlbums = new ArrayList<>();

        for (int i = 1; i < 100; i++) {
            String moviesPageUrl = getMoviesPageUrl(Calendar.getInstance().get(Calendar.YEAR), i);
            List<AvailableAlbum> albumsOnPage = pagalSongClient.getAlbumsOnPage(moviesPageUrl);
            log.info(String.format("Found %d albums on page %s", albumsOnPage.size(), moviesPageUrl));
            if (albumsOnPage.isEmpty()) {
                break;
            }
            availableAlbums.addAll(albumsOnPage);
        }

        return availableAlbums;
    }

    private static String getMoviesPageUrl(final int year, final int page) {
        return String.format(MOVIES_PAGE_URL, year, page);
    }
}
