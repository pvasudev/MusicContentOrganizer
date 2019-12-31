package net.paavan.music.content.organizer.downloader.fmw11.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public final class Folder {
    @JsonProperty("Prefix")
    private final String prefix;
}
