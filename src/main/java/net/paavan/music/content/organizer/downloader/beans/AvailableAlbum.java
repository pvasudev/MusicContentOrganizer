package net.paavan.music.content.organizer.downloader.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AvailableAlbum {
    private final String title;
    private final Integer year;
    private final String displayTitle;
    private final String url;
}
