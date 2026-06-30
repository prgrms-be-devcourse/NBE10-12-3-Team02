package com.back.domain.schedule.entity;

import com.back.domain.concert.entity.Concert;
import com.back.global.jpa.entity.BaseEntity;
import com.back.domain.venue.entity.Venue;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private LocalDateTime scheduleDate;

    @Column(nullable = false)
    private int round;

    private Schedule(Concert concert, Venue venue, LocalDateTime scheduleDate, int round) {
        this.concert = concert;
        this.venue = venue;
        this.scheduleDate = scheduleDate;
        this.round = round;
    }

    public static Schedule create(Concert concert, Venue venue, LocalDateTime scheduleDate, int round) {
        return new Schedule(concert, venue, scheduleDate, round);
    }
}
