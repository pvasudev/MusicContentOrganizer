package net.paavan.music.content.organizer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import net.paavan.music.content.organizer.downloader.AlbumDownloadTaskProvider;
import net.paavan.music.content.organizer.downloader.fmw11.Fmw11CurrentYearAlbumDownloadTaskProvider;
import net.paavan.music.content.organizer.downloader.pagalsong.PagalSongDownloadTaskProvider;

public class DownloadModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<AlbumDownloadTaskProvider> uriBinder = Multibinder.newSetBinder(binder(), AlbumDownloadTaskProvider.class);
        uriBinder.addBinding().to(Fmw11CurrentYearAlbumDownloadTaskProvider.class);
        uriBinder.addBinding().to(PagalSongDownloadTaskProvider.class);
    }
}
