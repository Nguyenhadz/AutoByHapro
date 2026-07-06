package com.hapro.autobyhapro.entity;

public class VideoCandidate {

    private final String videoId;
    private final String title;
    private final String url;

    public VideoCandidate(String videoId, String title, String url) {
        this.videoId = videoId;
        this.title = title;
        this.url = url;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}