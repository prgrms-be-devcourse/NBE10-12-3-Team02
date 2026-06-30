package com.back.domain.concert.dto;

import com.back.domain.concert.entity.Concert;
import java.time.LocalDateTime;

public record ConcertListResponse(
        Long concertId,
        String concertName,
        String venueName,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String imageUrl,
        String status
) {
    public static ConcertListResponse of(Concert concert, String venueName) {
        String status = concert.getEndDate().isAfter(LocalDateTime.now()) ? "AVAILABLE" : "CLOSED";
        return new ConcertListResponse(
                concert.getConcertId(),
                concert.getConcertName(),
                venueName,
                concert.getStartDate(),
                concert.getEndDate(),
                concert.getUrlPoster(),
                status
        );
    }
}