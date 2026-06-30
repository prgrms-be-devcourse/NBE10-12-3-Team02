package com.back.domain.concert.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concert extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long concertId;

    @Column(nullable = false)
    private String concertName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    private String urlPoster;

    private Concert(String concertName, String description, LocalDateTime startDate, LocalDateTime endDate, String urlPoster) {
        this.concertName = concertName;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.urlPoster = urlPoster;
    }

    public static Concert create(String concertName, String description, LocalDateTime startDate, LocalDateTime endDate, String urlPoster) {
        return new Concert(concertName, description, startDate, endDate, urlPoster);
    }
}
