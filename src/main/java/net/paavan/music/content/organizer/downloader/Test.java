package net.paavan.music.content.organizer.downloader;

import net.paavan.music.content.organizer.downloader.beans.AlbumSong;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.fmw11.Fmw11Client;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class Test {
    public static void main(String[] args) throws IOException {
        List<AvailableAlbum> albums = new Fmw11Client().getAvailableAlbums()
                .stream()
                .filter(availableAlbum -> availableAlbum.getYear()!= null && availableAlbum.getYear().equals(Calendar.getInstance().get(Calendar.YEAR)))
                .collect(Collectors.toList());

        System.out.println(albums.stream().map(AvailableAlbum::toString).collect(Collectors.joining("\n")));
        List<AlbumSong> albumSongs = getAlbumSongs(albums.get(0));
        System.out.println(albumSongs.stream().map(AlbumSong::toString).collect(Collectors.joining("\n")));
//        List<String> songDownloadUrls = albumSongs.stream().map(Test::getSongDownloadUrl).collect(Collectors.toList());
//        System.out.println(songDownloadUrls.stream().collect(Collectors.joining("\n")));
//        download(songDownloadUrls.get(0), albumSongs.get(0).getTitle());
    }

    private static List<AlbumSong> getAlbumSongs(final AvailableAlbum availableAlbum) throws IOException {
        Document doc = Jsoup.connect(availableAlbum.getUrl()).get();
        System.out.println(doc.title());
        Element movieTable = doc.selectFirst("#entries > table");
        Elements songs = movieTable.select("tr > td:nth-child(2) > a");

        return songs.stream()
                .map(element -> AlbumSong.builder()
                        .title(element.html())
                        .downloadUrl(element.attr("href"))
                        .build())
                .collect(Collectors.toList());
    }

    private static String getSongDownloadUrl(final String downloadPageUrl) {
        Document doc = null;
        try {
            doc = Jsoup.connect(downloadPageUrl).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element downloadBox = doc.selectFirst("#DownloadBox");
        Element songLink = downloadBox.selectFirst("a");
        return songLink.attr("href");
    }


    private static void download(final String downloadUrl, final String title) throws IOException {
        FileUtils.copyURLToFile(new URL(downloadUrl.replace(" ", "%20")), new File("/home/paavan/" + title));
    }

    private static void search(final String title, final String year) throws IOException {
        Document doc = Jsoup.connect(String.format("https://www.imdb.com/find?ref_=nv_sr_fn&q=%s+%s&ttype=ft&s=all", title, year)).get();
        Element element = doc.selectFirst(".result_text > a");
        if (element != null)
            System.out.println(title + " " + year + " https://www.imdb.com" + element.attr("href"));

    }
}
