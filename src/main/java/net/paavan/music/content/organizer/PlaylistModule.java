package net.paavan.music.content.organizer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import net.paavan.music.content.organizer.playlist.*;

public class PlaylistModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<PlaylistCreator> uriBinder = Multibinder.newSetBinder(binder(), PlaylistCreator.class);
        uriBinder.addBinding().to(NewSongsAlbumCollectionPlaylistCreator.class);
        uriBinder.addBinding().to(NewSongsAlbumPlaylistCreator.class);
        uriBinder.addBinding().to(SelectedAlbumPlaylistCreator.class);
        uriBinder.addBinding().to(SelectedAllPlaylistCreator.class);
        uriBinder.addBinding().to(SelectedLastNPlaylistCreator.class);
    }
}
