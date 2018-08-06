package net.paavan.music.content.organizer.downloader;

import javax.inject.Inject;
import java.util.Set;

public class DownloadManager {
    private final Set<AlbumDownloader> albumDownloaders;

    @Inject
    public DownloadManager(final Set<AlbumDownloader> albumDownloaders) {
        this.albumDownloaders = albumDownloaders;
    }

    public void download() {
        albumDownloaders.forEach(AlbumDownloader::download);
    }
}
