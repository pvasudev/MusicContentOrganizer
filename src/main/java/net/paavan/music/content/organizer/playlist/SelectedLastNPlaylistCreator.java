package net.paavan.music.content.organizer.playlist;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SelectedLastNPlaylistCreator implements PlaylistCreator {
    private static final String PLAYLISTS_DIR = "000 Playlists";
    private static final String PLAYLIST_FILE = "SEL 000 LAST %d.m3u";

    private final FilesystemClient filesystemClient;
    private final List<Integer> lastNValues;
    private final String selectedDirectory;

    @Inject
    public SelectedLastNPlaylistCreator(@Named("selected.songs.lastNValues") final String lastNValues, @Named("selected.songs.directory") final String selectedDirectory) {
        this.selectedDirectory = selectedDirectory;
        this.filesystemClient = new FilesystemClient();
        this.lastNValues = getLastN(lastNValues);
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

        for (Integer lastN : lastNValues) {
            Path playlistFile = Paths.get(selectedDirectory + "/" + PLAYLISTS_DIR + "/" + String.format(PLAYLIST_FILE, lastN));
            try {
                filesystemClient.writePlaylistFile(playlistFile, mp3FilesInDirectory
                        .subList(Math.max(mp3FilesInDirectory.size() - lastN, 0), mp3FilesInDirectory.size()));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    // --------------
    // Helper Methods

    private static List<Integer> getLastN(final String lastNValues) {
        return Arrays.stream(lastNValues.split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }
}
