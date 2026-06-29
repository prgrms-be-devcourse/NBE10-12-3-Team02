package com.back.domain.venue.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
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

    public static Venue create(String venueName, String location, Long totalSeats) {
        Venue venue = new Venue();
        venue.venueName = venueName;
        venue.location = location;
        venue.totalSeats = totalSeats;
        return venue;
    }
}
