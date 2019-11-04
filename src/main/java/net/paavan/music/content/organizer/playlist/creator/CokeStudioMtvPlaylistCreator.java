package net.paavan.music.content.organizer.playlist.creator;

import lombok.extern.slf4j.Slf4j;
import net.paavan.music.content.organizer.playlist.FilesystemClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class CokeStudioMtvPlaylistCreator implements PlaylistCreator {
    private static final String PLAYLIST_FILE = "Coke Studio @ MTV.m3u";

    private final String cokeStudioDirectory;
    private final FilesystemClient filesystemClient;

    @Inject
    public CokeStudioMtvPlaylistCreator(@Named("cokestudiomtv.directory") final String cokeStudioDirectory,
                                        final FilesystemClient filesystemClient) {
        this.cokeStudioDirectory = cokeStudioDirectory;
        this.filesystemClient = filesystemClient;
    }

    @Override
    public void create() {
        List<Path> mp3FilesInDirectory;
        try {
            mp3FilesInDirectory = filesystemClient.getMp3FilesInDirectory(cokeStudioDirectory);
        } catch (IOException e) {
            log.error("Unable to read cokeStudioDirectory", e);
            throw new RuntimeException(e);
        }
        Path playlistDirectoryPath = Paths.get(cokeStudioDirectory);

        if (Files.notExists(playlistDirectoryPath)) {
            try {
                Files.createDirectory(playlistDirectoryPath);
            } catch (IOException e) {
                log.error("Unable to create playlistDirectoryPath " + playlistDirectoryPath, e);
                throw new RuntimeException(e);
            }
        }

        Path playlistFile = Paths.get(cokeStudioDirectory + "/" + PLAYLIST_FILE);
        try {
            filesystemClient.writePlaylistFile(playlistFile, mp3FilesInDirectory);
        } catch (IOException e) {
            log.error("Unable to write playlist file " + playlistFile, e);
            throw new RuntimeException(e);
        }
    }
}
