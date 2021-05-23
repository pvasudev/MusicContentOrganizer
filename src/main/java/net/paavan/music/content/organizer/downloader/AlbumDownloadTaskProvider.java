package net.paavan.music.content.organizer.downloader;

import net.paavan.music.content.organizer.downloader.beans.DownloadTask;

import java.nio.file.Path;
import java.util.List;

public interface AlbumDownloadTaskProvider {
    List<DownloadTask> getDownloadTasks(final Path destinationPath);
}
