package net.paavan.music.content.organizer.downloader.fmw11;

import net.paavan.music.content.organizer.downloader.beans.AlbumSong;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadableAlbum;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class Fmw11Client {
    private static final String MOVIES_PAGE_URL = "http://www.apunkabollywood.com/browser/category/view/347/movies";

    public List<AvailableAlbum> getAvailableAlbums() {
        Document doc = null;
        try {
            doc = Jsoup.connect(MOVIES_PAGE_URL).get();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        Element movieTable = doc.selectFirst("#categories > table");
        Elements movies = movieTable.select("a");

        return movies.stream()
                .map(element -> AvailableAlbum.builder()
                        .title(getNameFromHtml(element.html()))
                        .year(getYearFromHtml(element.html()))
                        .displayTitle(element.html().trim())
                        .url(element.attr("href"))
                        .build())
                .collect(Collectors.toList());
    }

    public DownloadableAlbum getDownloadableAlbum(final AvailableAlbum availableAlbum) {
        Document doc = null;
        try {
            doc = Jsoup.connect(availableAlbum.getUrl()).get();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        Element movieTable = doc.selectFirst("#entries > table");
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

    private String getSongDownloadUrl(final String songDownloadPageUrl) {
        Document doc = null;
        try {
            doc = Jsoup.connect(songDownloadPageUrl).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element downloadBox = doc.selectFirst("#DownloadBox");
        Element songLink = downloadBox.selectFirst("a");
        return songLink.attr("href").replace(" ", "%20");
    }

    private String getNameFromHtml(final String html) {
        if (html.contains("(")) {
            return html.substring(0, html.indexOf("(")).trim();
        }
        return html;
    }

    private Integer getYearFromHtml(final String html) {
        if (html.contains("(")) {
            String stringYear = html.substring(html.indexOf("(") + 1, html.length() - 1).trim();
            try {
                return Integer.valueOf(stringYear);
            } catch (final NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
