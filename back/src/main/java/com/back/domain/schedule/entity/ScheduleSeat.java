package com.back.domain.schedule.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleSeat extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long concertSeatPriceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(nullable = false)
    private String gradeName;

    @Column(nullable = false)
    private String seatNumber;

    @Column(nullable = false)
    private Integer seatPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus seatStatus;

    private ScheduleSeat(Schedule schedule, String gradeName, String seatNumber, Integer seatPrice, SeatStatus seatStatus) {
        this.schedule = schedule;
        this.gradeName = gradeName;
        this.seatNumber = seatNumber;
        this.seatPrice = seatPrice;
        this.seatStatus = seatStatus;
    }

    public static ScheduleSeat create(Schedule schedule, String gradeName, String seatNumber, Integer seatPrice, SeatStatus seatStatus) {
        return new ScheduleSeat(schedule, gradeName, seatNumber, seatPrice, seatStatus);
    }

    public void updateSeatStatus(SeatStatus SeatStatus) {
        this.seatStatus = SeatStatus;
    }
}
