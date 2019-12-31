package net.paavan.music.content.organizer.downloader.fmw11.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public final class ListS3Response {
    private final List<File> files;
    private final List<Folder> folders;
}
