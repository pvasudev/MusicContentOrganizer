package net.paavan.music.content.organizer.playlist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilesystemClient {
    public List<Path> getMp3FilesInDirectory(final String directory) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".mp3"))
                    .collect(Collectors.toList());
        }
    }

    public void writePlaylistFile(final Path playlistFile, final List<Path> mp3Files) throws IOException {
        byte[] bytesToWrite = mp3Files.stream()
                .map(path -> playlistFile.getParent().relativize(path))
                .map(Path::toString)
                .collect(Collectors.joining("\r\n"))
                .getBytes();

        if (Files.exists(playlistFile)) {
            byte[] existingFileBytes = Files.readAllBytes(playlistFile);
            if (Arrays.equals(bytesToWrite, existingFileBytes)) {
                System.out.println("Skipping file: " + playlistFile);
                return;
            }
        }

        System.out.println("Writing file: " + playlistFile);
        Files.write(playlistFile, bytesToWrite);
    }
}
