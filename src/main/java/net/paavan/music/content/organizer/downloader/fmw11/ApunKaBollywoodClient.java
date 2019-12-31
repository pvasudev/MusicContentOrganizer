package net.paavan.music.content.organizer.downloader.fmw11;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.paavan.music.content.organizer.downloader.beans.AlbumSong;
import net.paavan.music.content.organizer.downloader.beans.AvailableAlbum;
import net.paavan.music.content.organizer.downloader.beans.DownloadableAlbum;
import net.paavan.music.content.organizer.downloader.fmw11.beans.DownloadUrl;
import net.paavan.music.content.organizer.downloader.fmw11.beans.File;
import net.paavan.music.content.organizer.downloader.fmw11.beans.Folder;
import net.paavan.music.content.organizer.downloader.fmw11.beans.ListS3Response;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ApunKaBollywoodClient implements Fmw11Client {
    private static final int MAX_RETRIES = 5;
    private static final ObjectReader LIST_S3_RESPONSE_OBJECT_READER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .readerFor(ListS3Response.class)
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final ObjectReader DOWNLOAD_URL_OBJECT_READER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .readerFor(DownloadUrl.class)
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final String LIST_S3_API_PATH = "https://api.gigahost123.com/api/listS3?bucket=mp3.gigahost123.com&path=";
    private static final String GET_DOWNLOAD_URL_API_PATH = "https://api.gigahost123.com/api/getDownloadUrl?bucket=mp3.gigahost123.com&key=";

    @Override
    public List<AvailableAlbum> getAlbumsOnPage(final String pageUrl) {
        ListS3Response listS3Response = getListS3ResponseWithRetry(pageUrl);

        return listS3Response.getFolders().stream()
                .map(Folder::getPrefix)
                .map(prefix -> {
                    String lastPrefixPart = getLastDelimitedPart(prefix);
                    Optional<Integer> year = getYearFromHtml(lastPrefixPart);
                    String displayTitle = unescapeHtml(lastPrefixPart.trim());
                    // Commented this out because the new webpage format brings up too many options to download. By not
                    // adding the year, it is relying on current year comparison to filter out old files. The downside
                    // is that new albums without the year will not be handled.
                    // TODO: Fix and re-enable the year assignment logic.
//                    if (!year.isPresent()) {
//                        log.info("Assuming current year for " + prefix);
//                        year = Optional.of(Calendar.getInstance().get(Calendar.YEAR));
//                        displayTitle = String.format("%s (%d)", displayTitle, year.get());
//                    }
                    return AvailableAlbum.builder()
                            .title(getNameFromHtml(lastPrefixPart))
                            .year(year.orElse(0))
                            .displayTitle(displayTitle)
                            .url(createEncodedUrl(LIST_S3_API_PATH, prefix))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public DownloadableAlbum getDownloadableAlbum(final AvailableAlbum availableAlbum) {
        ListS3Response listS3Response = getListS3ResponseWithRetry(availableAlbum.getUrl());

        // TODO: Replace with CollectionUtil
        if (listS3Response.getFolders() != null && !listS3Response.getFolders().isEmpty()) {
            if (listS3Response.getFolders().size() > 1) {
                throw new RuntimeException("Multiple sub-albums found for " + availableAlbum);
            }

            listS3Response = getListS3ResponseWithRetry(createEncodedUrl(LIST_S3_API_PATH, listS3Response.getFolders().get(0).getPrefix()));
        }

        List<AlbumSong> albumSongs = listS3Response.getFiles().stream()
                .map(File::getKey)
                .map(key -> {
                    String lastKeyPart = getLastDelimitedPart(key);
                    return AlbumSong.builder()
                            .title(lastKeyPart)
                            .downloadUrl(Optional.of(key)
                                    .map(key2 -> createEncodedUrl(GET_DOWNLOAD_URL_API_PATH, key2))
                                    .map(this::getDownloadUrl)
                                    .map(DownloadUrl::getUrl)
                                    .get())
                            .build();
                })
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

    private ListS3Response getListS3ResponseWithRetry(final String pageUrl) {
        return Failsafe
                .with(new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofSeconds(2L))
                        .withMaxAttempts(MAX_RETRIES))
                .get(() -> {
                    try (InputStream inputStream = new URL(pageUrl).openStream()) {
                        String listOfAlbums = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                        return LIST_S3_RESPONSE_OBJECT_READER.readValue(listOfAlbums);
                    }
                });
    }

    private String getLastDelimitedPart(final String prefix) {
        String[] prefixParts = prefix.split("/");
        if (prefixParts.length < 1) {
            throw new IllegalStateException(String.format("String is not delimited as expected %s", prefix));
        }
        return prefixParts[prefixParts.length - 1];
    }

    private DownloadUrl getDownloadUrl(final String songDownloadPageUrl) {
        return Failsafe
                .with(new RetryPolicy<>()
                        .handle(IOException.class)
                        .withDelay(Duration.ofSeconds(2L))
                        .withMaxAttempts(MAX_RETRIES))
                .get(() -> {
                    try (InputStream inputStream = new URL(songDownloadPageUrl).openStream()) {
                        String listOfAlbums = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                        return DOWNLOAD_URL_OBJECT_READER.readValue(listOfAlbums);
                    }
                });
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

    private String createEncodedUrl(final String baseUrl, final String pathToEncode) {
        return baseUrl + URLEncoder.encode(pathToEncode, StandardCharsets.UTF_8);
    }
}
