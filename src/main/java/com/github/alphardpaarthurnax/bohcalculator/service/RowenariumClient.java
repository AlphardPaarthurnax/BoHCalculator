package com.github.alphardpaarthurnax.bohcalculator.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public class RowenariumClient {
    public static final String BASE_URL = "https://uadaf.theevilroot.xyz/rowenarium";

    private final int timeoutMillis;
    private final int attempts;

    public RowenariumClient() {
        this(30_000, 3);
    }

    RowenariumClient(int timeoutMillis, int attempts) {
        this.timeoutMillis = timeoutMillis;
        this.attempts = attempts;
    }

    public Document fetch(String kind, String id) throws IOException {
        String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8).replace("+", "%20");
        String url = BASE_URL + "/" + kind + "/" + encodedId;
        IOException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Connection.Response response = Jsoup.connect(url)
                        .userAgent("BoHCalculator/1.0 (+local SDE generator)")
                        .timeout(timeoutMillis)
                        .maxBodySize(0)
                        .followRedirects(true)
                        .execute();
                return response.parse();
            } catch (IOException exception) {
                last = exception;
                if (attempt < attempts) {
                    try {
                        Thread.sleep(250L * attempt);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while fetching " + url, interrupted);
                    }
                }
            }
        }
        throw last != null ? last : new IOException("Unable to fetch " + url);
    }

    public byte[] downloadImage(String sourcePath) throws IOException {
        String encodedPath = Arrays.stream(sourcePath.split("/", -1))
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
        String url = sourcePath.startsWith("http://") || sourcePath.startsWith("https://")
                ? sourcePath : "https://uadaf.theevilroot.xyz" + encodedPath;
        IOException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Connection.Response response = Jsoup.connect(url)
                        .userAgent("BoHCalculator/1.0 (+local image downloader)")
                        .timeout(timeoutMillis)
                        .maxBodySize(0)
                        .ignoreContentType(true)
                        .followRedirects(true)
                        .execute();
                String contentType = response.contentType();
                if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                    throw new IOException("Not an image: " + contentType + " from " + url);
                }
                return response.bodyAsBytes();
            } catch (IOException exception) {
                if (exception instanceof HttpStatusException status && status.getStatusCode() == 404) {
                    throw new ImageUnavailableException(url, exception);
                }
                last = exception;
            }
        }
        throw last != null ? last : new IOException("Unable to download " + url);
    }

    public static final class ImageUnavailableException extends IOException {
        ImageUnavailableException(String url, Throwable cause) {
            super("Rowenarium image resolves to _x.png: " + url, cause);
        }
    }
}
