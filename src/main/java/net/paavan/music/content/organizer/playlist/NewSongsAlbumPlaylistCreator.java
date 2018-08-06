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

public class NewSongsAlbumPlaylistCreator implements PlaylistCreator {
    private static final String PLAYLIST_FILE = "NEW %s.m3u";

    private final String newSongsDirectory;
    private final FilesystemClient filesystemClient;

    @Inject
    public NewSongsAlbumPlaylistCreator(@Named("new.songs.directory") final String newSongsDirectory,
                                        final FilesystemClient filesystemClient) {
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
            List<Path> albumDirectories;
            try (Stream<Path> paths = Files.list(albumCollectionDirectory)) {
                albumDirectories = paths
                        .filter(Files::isDirectory)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            for (Path albumDirectory : albumDirectories) {
                List<Path> mp3FilesInDirectory;
                try {
                    mp3FilesInDirectory = filesystemClient.getMp3FilesInDirectory(albumDirectory.toString());
                    Path playlistFile = Paths.get(albumDirectory.toString() + "/" +
                            String.format(PLAYLIST_FILE, albumDirectory.getFileName().toString()));
                    filesystemClient.writePlaylistFile(playlistFile, mp3FilesInDirectory);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
