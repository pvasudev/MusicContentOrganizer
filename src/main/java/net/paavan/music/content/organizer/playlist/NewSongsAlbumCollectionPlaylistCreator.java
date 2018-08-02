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

public class NewSongsAlbumCollectionPlaylistCreator implements PlaylistCreator {
    private static final String PLAYLIST_FILE = "NEW %s.m3u";

    private final FilesystemClient filesystemClient;
    private final String newSongsDirectory;

    @Inject
    public NewSongsAlbumCollectionPlaylistCreator(@Named("new.songs.directory") final String newSongsDirectory, final FilesystemClient filesystemClient) {
        this.newSongsDirectory = newSongsDirectory;
        this.filesystemClient = filesystemClient;
    }

    @Override
    public void create() {
        List<Path> albumCollectionDirectories;
        try (Stream<Path> paths = Files.list(Paths.get(newSongsDirectory))) {
            albumCollectionDirectories = paths.filter(Files::isDirectory)
                    .filter(path -> !path.endsWith("NewSongs"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        for (Path albumCollectionDirectory : albumCollectionDirectories) {
            List<Path> mp3FilesInDirectory;
            try {
                mp3FilesInDirectory = filesystemClient.getMp3FilesInDirectory(albumCollectionDirectory.toString());
                Path playlistFile = Paths.get(albumCollectionDirectory.toString() + "/" +
                        String.format(PLAYLIST_FILE, albumCollectionDirectory.getFileName().toString()));
                filesystemClient.writePlaylistFile(playlistFile, mp3FilesInDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
