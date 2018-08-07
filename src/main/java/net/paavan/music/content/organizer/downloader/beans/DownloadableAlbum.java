package net.paavan.music.content.organizer.downloader.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public class DownloadableAlbum {
    private final String title;
    private final Integer year;
    private final String displayTitle;
    private final String url;
    private final List<AlbumSong> songs;
}
