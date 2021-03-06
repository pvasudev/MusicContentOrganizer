package net.paavan.music.content.organizer.downloader.fmw11;

import lombok.extern.slf4j.Slf4j;
import net.paavan.music.content.organizer.downloader.AlbumProviderClient;
import net.paavan.music.content.organizer.downloader.beans.AlbumSong;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadableAlbum;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class AlbumProviderOldWebpageClient implements AlbumProviderClient {
    private static final int MAX_RETRIES = 5;

    @Override
    public List<AvailableAlbum> getAlbumsOnPage(final String pageUrl) {
        Document doc = getDocumentFromUrlWithRetry(pageUrl);
        Element movieTable = doc.selectFirst("#categories > table");
        Elements movies = movieTable.select("a");

        return movies.stream()
                .map(element -> {
                    Optional<Integer> year = getYearFromHtml(element.html());
                    String displayTitle = unescapeHtml(element.html().trim());
                    if (!year.isPresent()) {
                        year = Optional.of(Calendar.getInstance().get(Calendar.YEAR));
                        displayTitle = String.format("%s (%d)", displayTitle, year.get());
                    }
                    return AvailableAlbum.builder()
                            .title(getNameFromHtml(element.html()))
                            .year(year.get())
                            .displayTitle(displayTitle)
                            .url(element.attr("href"))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public DownloadableAlbum getDownloadableAlbum(final AvailableAlbum availableAlbum) {
        Document doc = getDocumentFromUrlWithRetry(availableAlbum.getUrl());

        Element movieTable = doc.selectFirst("#entries > table");
        if (movieTable == null) {
            List<AvailableAlbum> availableAlbums = getAlbumsOnPage(availableAlbum.getUrl());
            if (availableAlbums.isEmpty()) {
                throw new RuntimeException("Unable to find album for " + availableAlbum);
            }
            if (availableAlbums.size() > 1) {
                throw new RuntimeException("Multiple sub-albums found for " + availableAlbum);
            }

            // Only retrieving the first nested album
            // TODO: Create sub-albums if size > 1
            doc = getDocumentFromUrlWithRetry(availableAlbums.get(0).getUrl());
            movieTable = doc.selectFirst("#entries > table");
            if (movieTable == null) {
                throw new RuntimeException("Unable to parse album " + availableAlbum);
            }
        }
        Elements songs = movieTable.select("tr > td:nth-child(2) > a");

        List<AlbumSong> albumSongs = songs.stream()
                .map(element -> AlbumSong.builder()
                        .title(element.html())
                        .downloadUrl(getSongDownloadUrl(element.attr("href")))
                        .build())
                .collect(Collectors.toList());

        return DownloadableAlbum.builder()
                .title(availableAlbum.getTitle())
                .year(availableAlbum.getYear())
                .displayTitle(availableAlbum.getDisplayTitle())
                .url(availableAlbum.getUrl())
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

    private String getSongDownloadUrl(final String songDownloadPageUrl) {
        Document doc = getDocumentFromUrlWithRetry(songDownloadPageUrl);
        Element downloadBox = doc.selectFirst("#DownloadBox");
        Element songLink = downloadBox.selectFirst("a");
        return songLink.attr("href").replace(" ", "%20");
    }

    private String getNameFromHtml(final String html) {
        if (html.contains("(")) {
            return html.substring(0, html.indexOf("(")).trim();
        }
        return unescapeHtml(html);
    }

    private Optional<Integer> getYearFromHtml(final String html) {
        if (html.contains("(")) {
            String stringYear = html.substring(html.indexOf("(") + 1, html.length() - 1).trim();
            try {
                return Optional.of(Integer.valueOf(stringYear));
            } catch (final NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private String unescapeHtml(final String html) {
        return Jsoup.parse(html).text();
    }
}
