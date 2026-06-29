package com.back.domain.schedule.entity;

import com.back.domain.concert.entity.Concert;
import com.back.global.jpa.entity.BaseEntity;
import com.back.domain.venue.entity.Venue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
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

    public static Schedule create(Concert concert, Venue venue, LocalDateTime scheduleDate, int round) {
        Schedule schedule = new Schedule();
        schedule.concert = concert;
        schedule.venue = venue;
        schedule.scheduleDate = scheduleDate;
        schedule.round = round;
        return schedule;
    }
}
