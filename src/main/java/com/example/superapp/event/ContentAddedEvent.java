package com.example.superapp.event;

public record ContentAddedEvent(
        Long contentId,
        String contentType,
        String contentTitle,
        String posterPath,
        String eventType,
        Long episodeId,
        String episodeName   // null nếu không phải episode event
) {
    // Constructor không có episodeId và episodeName
    public ContentAddedEvent(Long contentId, String contentType,
                             String contentTitle, String posterPath, String eventType) {
        this(contentId, contentType, contentTitle, posterPath, eventType, null, null);
    }

    // Constructor có episodeId nhưng không có episodeName
    public ContentAddedEvent(Long contentId, String contentType,
                             String contentTitle, String posterPath,
                             String eventType, Long episodeId) {
        this(contentId, contentType, contentTitle, posterPath, eventType, episodeId, null);
    }
}