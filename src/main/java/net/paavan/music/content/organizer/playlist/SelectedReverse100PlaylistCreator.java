package net.paavan.music.content.organizer.playlist;

import com.google.common.collect.Lists;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SelectedReverse100PlaylistCreator implements PlaylistCreator {
    private static final String PLAYLISTS_DIR = "000 Playlists";
    private static final String PLAYLIST_FILE = "SEL 000 LAST REV %d.m3u";

    private final FilesystemClient filesystemClient;
    private final int playlistSize;
    private final String selectedDirectory;

    @Inject
    public SelectedReverse100PlaylistCreator(@Named("selected.songs.reverse.size") final int playlistSize, @Named("selected.songs.directory") final String selectedDirectory) {
        this.selectedDirectory = selectedDirectory;
        this.filesystemClient = new FilesystemClient();
        this.playlistSize = playlistSize;
    }

    @Override
    public void create() {
        List<Path> mp3FilesInDirectory;
        try {
            mp3FilesInDirectory = filesystemClient.getMp3FilesInDirectory(selectedDirectory);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        Path playlistDirectoryPath = Paths.get(selectedDirectory + "/" + PLAYLISTS_DIR);
        if (Files.notExists(playlistDirectoryPath)) {
            try {
                Files.createDirectory(playlistDirectoryPath);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        Path playlistFile = Paths.get(selectedDirectory + "/" + PLAYLISTS_DIR + "/" + String.format(PLAYLIST_FILE, playlistSize));
        try {
            filesystemClient.writePlaylistFile(playlistFile, Lists.reverse(mp3FilesInDirectory
                    .subList(Math.max(mp3FilesInDirectory.size() - playlistSize, 0), mp3FilesInDirectory.size())));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
