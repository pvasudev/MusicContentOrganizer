package net.paavan.music.content.organizer.downloader;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

public class DownloadManager {
    private final boolean executeDownloader;
    private final Set<AlbumDownloader> albumDownloaders;

    @Inject
    public DownloadManager(@Named("execute.downloader") final boolean executeDownloader, final Set<AlbumDownloader> albumDownloaders) {
        this.executeDownloader = executeDownloader;
        this.albumDownloaders = albumDownloaders;
    }

    public void download() {
        if (executeDownloader) {
            albumDownloaders.forEach(AlbumDownloader::download);
        }
    }
}
