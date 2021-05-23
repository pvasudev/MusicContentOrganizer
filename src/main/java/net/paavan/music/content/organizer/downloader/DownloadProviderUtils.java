package net.paavan.music.content.organizer.downloader;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadTask;
import net.paavan.music.content.organizer.downloader.beans.DownloadableAlbum;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
/**
 * @deprecated Refactor responsibility of DownloadTaskProvider to not check for existing albums.
 */
@Deprecated(forRemoval = true)
public class DownloadProviderUtils {
    public static List<String> getExistingAlbums(final String allSongsDirectory) {
        List<String> existingAlbums;
        try (Stream<Path> paths = Files.list(Paths.get(allSongsDirectory))) {
            existingAlbums = paths.filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());

        } catch (final IOException e) {
            log.error("Unable to read allSongsDirectory", e);
            throw new RuntimeException(e);
        }
        return existingAlbums;
    }

    public static String getPrintableAlbumsList(final List<AvailableAlbum> albumsToDownload) {
        AtomicInteger counter = new AtomicInteger();
        return Joiner
                .on("\n\t")
                .withKeyValueSeparator(". ")
                .join(albumsToDownload.stream()
                        .map(AvailableAlbum::getDisplayTitle)
                        .collect(Collectors.toMap(s -> counter.incrementAndGet(), Function.identity())));
    }

    public static List<DownloadTask> getDownloadTasksForDownloadableAlbum(final DownloadableAlbum downloadableAlbum,
                                                                    final Path destinationNewSongsCollectionPath) {
        return downloadableAlbum.getSongs().stream()
                .map(albumSong -> DownloadTask.builder()
                        .sourceUrl(albumSong.getDownloadUrl())
                        .destinationCollectionPath(destinationNewSongsCollectionPath)
                        .albumName(downloadableAlbum.getDisplayTitle())
                        .fileName(getFilenameFromDownloadUrl(albumSong.getTitle()))
                        .build())
                .collect(Collectors.toList());
    }

    private static String getFilenameFromDownloadUrl(final String downloadUrl) {
        String encodedUrl = URLEncoder.encode(downloadUrl, StandardCharsets.UTF_8);
        String uri = URI.create(encodedUrl).getPath();
        String decodedUrl = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        String filename = FilenameUtils.getName(decodedUrl);

        if (!filename.contains(".mp3")) {
            filename = filename + ".mp3";
        }

        return filename;
    }
}
