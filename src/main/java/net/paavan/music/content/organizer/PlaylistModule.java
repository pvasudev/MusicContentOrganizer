package net.paavan.music.content.organizer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import net.paavan.music.content.organizer.playlist.creator.CokeStudioMtvPlaylistCreator;
import net.paavan.music.content.organizer.playlist.creator.CokeStudioPlaylistCreator;
import net.paavan.music.content.organizer.playlist.creator.NewSongsAlbumPlaylistCreator;
import net.paavan.music.content.organizer.playlist.creator.PlaylistCreator;
import net.paavan.music.content.organizer.playlist.creator.SelectedAllPlaylistCreator;
import net.paavan.music.content.organizer.playlist.creator.SelectedLastNPlaylistCreator;
import net.paavan.music.content.organizer.playlist.creator.SelectedReverse100PlaylistCreator;

public class PlaylistModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<PlaylistCreator> uriBinder = Multibinder.newSetBinder(binder(), PlaylistCreator.class);
//        uriBinder.addBinding().to(NewSongsAlbumCollectionPlaylistCreator.class); // Disabling this playlist creator
        uriBinder.addBinding().to(NewSongsAlbumPlaylistCreator.class);
//        uriBinder.addBinding().to(SelectedAlbumPlaylistCreator.class); // Disabling this playlist creator
        uriBinder.addBinding().to(SelectedAllPlaylistCreator.class);
        uriBinder.addBinding().to(SelectedLastNPlaylistCreator.class);
        uriBinder.addBinding().to(SelectedReverse100PlaylistCreator.class);
        uriBinder.addBinding().to(CokeStudioPlaylistCreator.class);
        uriBinder.addBinding().to(CokeStudioMtvPlaylistCreator.class);
    }
}
