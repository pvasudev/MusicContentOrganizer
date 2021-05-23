package net.paavan.music.content.organizer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import net.paavan.music.content.organizer.downloader.AlbumDownloadTaskProvider;
import net.paavan.music.content.organizer.downloader.fmw11.Fmw11CurrentYearAlbumDownloadTaskProvider;

public class DownloadModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<AlbumDownloadTaskProvider> uriBinder = Multibinder.newSetBinder(binder(), AlbumDownloadTaskProvider.class);
        uriBinder.addBinding().to(Fmw11CurrentYearAlbumDownloadTaskProvider.class);
    }
}
