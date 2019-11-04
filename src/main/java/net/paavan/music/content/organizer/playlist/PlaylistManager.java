package net.paavan.music.content.organizer.playlist;

import net.paavan.music.content.organizer.playlist.creator.PlaylistCreator;

import javax.inject.Inject;
import java.util.Set;

public class PlaylistManager {
    private final Set<PlaylistCreator> playlistCreators;

    @Inject
    public PlaylistManager(final Set<PlaylistCreator> playlistCreators) {
        this.playlistCreators = playlistCreators;
    }

    public void createPlaylists() {
        playlistCreators.forEach(PlaylistCreator::create);
    }
}
