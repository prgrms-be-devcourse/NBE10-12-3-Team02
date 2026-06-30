package com.back.domain.concert.dto;

import com.back.domain.schedule.entity.SeatStatus;

public record SeatOccupyResponse (
        String occupyToken,
        long expireInSeconds,
        SeatStatus seatStatus
){
}
