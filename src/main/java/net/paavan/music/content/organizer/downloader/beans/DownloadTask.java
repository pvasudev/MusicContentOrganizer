package net.paavan.music.content.organizer.downloader.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;

@Getter
@Builder
@ToString
public class DownloadTask {
    private final String sourceUrl;
    private final Path destinationCollectionPath;
    private final String albumName;
    private final String fileName;
}
