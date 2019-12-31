package net.paavan.music.content.organizer.downloader.fmw11.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class DownloadUrl {
    private final String url;
}
