package net.paavan.music.content.organizer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.paavan.music.content.organizer.playlist.PlaylistManager;

public class Main {
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new PlaylistModule(), new PropertiesModule());
        PlaylistManager playlistManager = injector.getInstance(PlaylistManager.class);

        playlistManager.createPlaylists();
    }
}
