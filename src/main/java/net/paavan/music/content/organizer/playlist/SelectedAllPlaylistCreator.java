package net.paavan.music.content.organizer.playlist;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SelectedAllPlaylistCreator implements PlaylistCreator {
    private static final String PLAYLISTS_DIR = "000 Playlists";
    private static final String PLAYLIST_FILE = "SEL 000 ALL.m3u";

    private final String selectedDirectory;
    private final FilesystemClient filesystemClient;

    @Inject
    public SelectedAllPlaylistCreator(@Named("selected.songs.directory") final String selectedDirectory, final FilesystemClient filesystemClient) {
        this.selectedDirectory = selectedDirectory;
        this.filesystemClient = filesystemClient;
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

        Path playlistFile = Paths.get(selectedDirectory + "/" + PLAYLISTS_DIR + "/" + PLAYLIST_FILE);
        try {
            filesystemClient.writePlaylistFile(playlistFile, mp3FilesInDirectory);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
