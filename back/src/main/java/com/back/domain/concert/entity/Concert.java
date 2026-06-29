package com.back.domain.concert.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
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

    public static Concert create(String concertName, String description, LocalDateTime startDate, LocalDateTime endDate, String urlPoster) {
        Concert concert = new Concert();
        concert.concertName = concertName;
        concert.description = description;
        concert.startDate = startDate;
        concert.endDate = endDate;
        concert.urlPoster = urlPoster;
        return concert;
    }
}
