package net.paavan.music.content.organizer.playlist;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SelectedAlbumPlaylistCreator implements PlaylistCreator {
    private static final String PLAYLISTS_DIR = "000 Playlists";
    private static final String PLAYLIST_FILE = "SEL %s.m3u";

    private final String selectedDirectory;
    private final FilesystemClient filesystemClient;

    @Inject
    public SelectedAlbumPlaylistCreator(@Named("selected.songs.directory") final String selectedDirectory, final FilesystemClient filesystemClient) {
        this.selectedDirectory = selectedDirectory;
        this.filesystemClient = filesystemClient;
    }

    @Override
    public void create() {
        List<Path> directoryPaths;
        try (Stream<Path> paths = Files.walk(Paths.get(selectedDirectory))) {
            directoryPaths = paths.filter(Files::isDirectory)
                    .filter(path -> !path.endsWith("[SELECTED]"))
                    .filter(path -> !path.endsWith("000 Playlists"))
                    .filter(path -> !path.endsWith(PLAYLISTS_DIR))
                    .collect(Collectors.toList());
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

        for (Path directoryPath : directoryPaths) {
            List<Path> mp3FilesInDirectory;
            try {
                mp3FilesInDirectory = filesystemClient.getMp3FilesInDirectory(directoryPath.toString());
                Path playlistFile = Paths.get(selectedDirectory + "/" + PLAYLISTS_DIR + "/" +
                        String.format(PLAYLIST_FILE, directoryPath.getFileName().toString()));
                filesystemClient.writePlaylistFile(playlistFile, mp3FilesInDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
