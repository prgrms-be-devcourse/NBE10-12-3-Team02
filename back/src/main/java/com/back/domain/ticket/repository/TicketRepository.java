package com.back.domain.ticket.repository;

import com.back.domain.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketIdAndUser_UserId(Long ticketId, Long userId);
}
