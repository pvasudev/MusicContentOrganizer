package net.paavan.music.content.organizer.downloader.fmw11;

import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadableAlbum;

import java.util.List;

public interface Fmw11Client {
    List<AvailableAlbum> getAlbumsOnPage(final String pageUrl);
    DownloadableAlbum getDownloadableAlbum(final AvailableAlbum availableAlbum);
}
