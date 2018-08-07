package net.paavan.music.content.organizer.downloader.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AlbumSong {
    private final String title;
    private final String downloadUrl;
}
