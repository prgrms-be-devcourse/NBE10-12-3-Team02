package com.back.domain.ticket.entity;

import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.user.entity.User;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Ticket extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_seat_id", nullable = false)
    private ScheduleSeat scheduleSeat;

    @Column(nullable = false, unique = true)
    private String ticketNumber;

    @Column(nullable = false)
    private int ticketPrice;

    @Column(nullable = false)
    private boolean isValid = true;

    public static Ticket create(
            User user,
            Schedule schedule,
            ScheduleSeat scheduleSeat,
            String ticketNumber,
            int ticketPrice
    ) {
        Ticket ticket = new Ticket();
        ticket.user = user;
        ticket.schedule = schedule;
        ticket.scheduleSeat = scheduleSeat;
        ticket.ticketNumber = ticketNumber;
        ticket.ticketPrice = ticketPrice;
        ticket.isValid = true;
        return ticket;
    }
    public void updateIsValid(boolean isValid) {
        this.isValid = isValid;
    }
}
