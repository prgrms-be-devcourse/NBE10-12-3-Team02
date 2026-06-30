package com.back.domain.schedule.repository;

import com.back.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findFirstByConcertConcertId(Long concertId);

    List<Schedule> findByConcertConcertId(Long concertId);

    Optional<Schedule> findByScheduleIdAndConcert_ConcertId(Long scheduleId, Long concertId);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.venue WHERE s.concert.concertId IN :concertIds")
    List<Schedule> findAllWithVenueByConcertIds(@Param("concertIds") List<Long> concertIds);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.venue WHERE s.concert.concertId = :concertId ORDER BY s.scheduleId")
    List<Schedule> findWithVenueByConcertId(@Param("concertId") Long concertId);
}

