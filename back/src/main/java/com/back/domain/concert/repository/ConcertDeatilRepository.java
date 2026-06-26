package com.back.domain.concert.repository;

import com.back.domain.concert.entity.ConcertDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertDeatilRepository extends JpaRepository<ConcertDetail, Long> {
}
