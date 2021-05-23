package net.paavan.music.content.organizer.downloader.pagalsong;

import lombok.extern.slf4j.Slf4j;
import net.paavan.music.content.organizer.downloader.beans.AlbumSong;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadableAlbum;
import net.paavan.music.content.organizer.downloader.AlbumProviderClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class PagalSongClient implements AlbumProviderClient {
    private static final int MAX_RETRIES = 5;
    private static final Pattern YEAR_PATTERN = Pattern.compile("^(.+)(\\s)([0-9]{4})$");

    @Override
    public List<AvailableAlbum> getAlbumsOnPage(final String pageUrl) {
        Document document = getDocumentFromUrlWithRetry(pageUrl);
        Integer year = getYear(document);

        return document
                .select(".tnned")
                .select("h3")
                .select("a")
                .stream()
                .map(element -> AvailableAlbum.builder()
                        .title(cleanTitle(element.text()))
                        .displayTitle(String.format("%s (%d)", cleanTitle(element.text()), year))
                        .year(year)
                        .url(element.attr("href"))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public DownloadableAlbum getDownloadableAlbum(final AvailableAlbum availableAlbum) {
        Document document = getDocumentFromUrlWithRetry(availableAlbum.getUrl());

        List<AlbumSong> albumSongs = document.select("div.listbox").select("a")
                .stream()
                .map(element -> element.attr("href"))
                .map(this::getDownloadUrlFromSongPage)
                .collect(Collectors.toList());

        return DownloadableAlbum.buildFromAvailableAlbum(availableAlbum)
                .songs(albumSongs)
                .build();
    }

    // --------------
    // Helper Methods

    private Document getDocumentFromUrlWithRetry(final String url) {
        int attempts = 0;
        do {
            try {
                return Jsoup.connect(url).get();
            } catch (final IOException e) {
                attempts++;
                log.error(String.format("Unable to read webpage %s after %d attempt(s). Retrying: %s.", url, attempts,
                        (attempts < MAX_RETRIES ? "Yes" : "No")), e);
            }
        } while (attempts < MAX_RETRIES);

        throw new RuntimeException(String.format("Unable to read webpage after %d attempts", attempts));
    }

    private Integer getYear(final Document document) {
        String title = document.select("h2.title").text();
        Matcher matcher = YEAR_PATTERN.matcher(title);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(3));
        }

        return null;
    }

    private String cleanTitle(final String title) {
        return title.replaceAll(" mp3 songs", "").trim();
    }

    private AlbumSong getDownloadUrlFromSongPage(final String songPageUrl) {
        Document document = getDocumentFromUrlWithRetry(songPageUrl);
        List<String> downloadUrls = document
                .select("div.downloaddiv")
                .select("a")
                .stream()
                .map(element -> element.attr("href"))
                .map(s -> s.replace(" ", "%20"))
                .collect(Collectors.toList());

        return AlbumSong.builder()
                .title(document.select("h2.title").first().text().trim())
                .downloadUrl(selectDownloadUrl(downloadUrls))
                .build();
    }

    private String selectDownloadUrl(final List<String> downloadUrls) {
        Optional<String> highBitrateUrls = downloadUrls.stream()
                .filter(s -> s.contains("320"))
                .findFirst();

        if (highBitrateUrls.isPresent()) {
            return highBitrateUrls.get();
        }

        return downloadUrls.get(0);
    }
}
