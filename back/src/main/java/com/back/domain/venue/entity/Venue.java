package com.back.domain.venue.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long venueId;

    @Column(nullable = false)
    private String venueName;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Long totalSeats;

    private Venue(String venueName, String location, Long totalSeats) {
        this.venueName = venueName;
        this.location = location;
        this.totalSeats = totalSeats;
    }

    public static Venue create(String venueName, String location, Long totalSeats) {
        return new Venue(venueName, location, totalSeats);
    }
}
