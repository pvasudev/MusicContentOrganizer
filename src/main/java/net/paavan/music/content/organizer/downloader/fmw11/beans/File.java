package net.paavan.music.content.organizer.downloader.fmw11.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public final class File {
    @JsonProperty("Key")
    private final String key;

    @JsonProperty("LastModified")
    private final String lastModified;

    @JsonProperty("ETag")
    private final String eTag;

    @JsonProperty("Size")
    private final String size;

    @JsonProperty("StorageClass")
    private final String storageClass;
}
