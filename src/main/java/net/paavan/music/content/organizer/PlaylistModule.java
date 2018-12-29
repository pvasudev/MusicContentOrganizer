package net.paavan.music.content.organizer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import net.paavan.music.content.organizer.playlist.NewSongsAlbumCollectionPlaylistCreator;
import net.paavan.music.content.organizer.playlist.NewSongsAlbumPlaylistCreator;
import net.paavan.music.content.organizer.playlist.PlaylistCreator;
import net.paavan.music.content.organizer.playlist.SelectedAlbumPlaylistCreator;
import net.paavan.music.content.organizer.playlist.SelectedAllPlaylistCreator;
import net.paavan.music.content.organizer.playlist.SelectedLastNPlaylistCreator;
import net.paavan.music.content.organizer.playlist.SelectedReverse100PlaylistCreator;

public class PlaylistModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<PlaylistCreator> uriBinder = Multibinder.newSetBinder(binder(), PlaylistCreator.class);
        uriBinder.addBinding().to(NewSongsAlbumCollectionPlaylistCreator.class);
        uriBinder.addBinding().to(NewSongsAlbumPlaylistCreator.class);
//        uriBinder.addBinding().to(SelectedAlbumPlaylistCreator.class); // Disabling this playlist creator
        uriBinder.addBinding().to(SelectedAllPlaylistCreator.class);
        uriBinder.addBinding().to(SelectedLastNPlaylistCreator.class);
        uriBinder.addBinding().to(SelectedReverse100PlaylistCreator.class);
    }
}
