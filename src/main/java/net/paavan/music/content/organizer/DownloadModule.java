package net.paavan.music.content.organizer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import net.paavan.music.content.organizer.downloader.AlbumDownloader;
import net.paavan.music.content.organizer.downloader.fmw11.Fmw11CurrentYearAlbumDownloader;

public class DownloadModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<AlbumDownloader> uriBinder = Multibinder.newSetBinder(binder(), AlbumDownloader.class);
        uriBinder.addBinding().to(Fmw11CurrentYearAlbumDownloader.class);
    }
}
