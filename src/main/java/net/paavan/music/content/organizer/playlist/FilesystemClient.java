package net.paavan.music.content.organizer.playlist;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class FilesystemClient {
    public List<Path> getMp3FilesInDirectory(final String directory) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".mp3"))
                    .collect(Collectors.toList());
        }
    }

    public void writePlaylistFile(final Path playlistFile, final List<Path> mp3Files) throws IOException {
        if (mp3Files.isEmpty()) {
            log.info("Skipping empty file: " + playlistFile);
            return;
        }

        byte[] bytesToWrite = mp3Files.stream()
                .map(path -> playlistFile.getParent().relativize(path))
                .map(Path::toString)
                .collect(Collectors.joining("\r\n"))
                .getBytes();

        if (Files.exists(playlistFile)) {
            byte[] existingFileBytes = Files.readAllBytes(playlistFile);
            if (Arrays.equals(bytesToWrite, existingFileBytes)) {
//                log.info("Skipping file: " + playlistFile);
                return;
            }
        }

        log.info("Writing file: " + playlistFile);
        Files.write(playlistFile, bytesToWrite);
    }
}
