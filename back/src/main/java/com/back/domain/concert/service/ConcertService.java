package com.back.domain.concert.service;

import com.back.domain.concert.dto.ConcertDetailResponse;
import com.back.domain.concert.dto.ConcertListResponse;
import com.back.domain.concert.dto.SeatOccupyResponse;
import com.back.domain.concert.dto.SeatSelectionResponse;
import com.back.domain.concert.dto.SeatSelectionResponse.SeatDetailResponse;
import com.back.domain.concert.entity.Concert;
import com.back.domain.concert.entity.ConcertDetail;
import com.back.domain.concert.enums.ConcertSortType;
import com.back.domain.concert.repository.ConcertDeatilRepository;
import com.back.domain.concert.repository.ConcertRepository;
import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.entity.SeatStatus;
import com.back.domain.schedule.repository.ScheduleRepository;
import com.back.domain.schedule.repository.ScheduleSeatRepository;
import com.back.domain.venue.entity.Venue;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final ScheduleRepository scheduleRepository;
    private final ConcertRepository concertRepository;
    private final ConcertDeatilRepository concertDeatilRepository;
    private final StringRedisTemplate redisTemplate;

    private static final long OCCUPY_TTL_SECONDS = 600;

    private static final RedisScript<Long> OCCUPY_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('EXISTS', KEYS[1]) == 0 then
                      redis.call('HSET', KEYS[1], 'userId', ARGV[1], 'occupyToken', ARGV[2])
                      redis.call('EXPIRE', KEYS[1], ARGV[3])
                      return 1
                    else
                      return 0
                    end
                    """,
            Long.class
    );

    public List<ConcertListResponse> getConcerts(String keyword, ConcertSortType sort) {
        List<Concert> concerts = concertRepository.findByKeyword(keyword);

        List<Long> concertIds = concerts.stream()
                .map(Concert::getConcertId)
                .toList();

        List<Schedule> schedules = scheduleRepository.findAllWithVenueByConcertIds(concertIds);

        Map<Long, String> venueNameMap = schedules.stream()
                .collect(Collectors.toMap(
                        schedule -> schedule.getConcert().getConcertId(),
                        schedule -> schedule.getVenue().getVenueName(),
                        (existing, replacement) -> existing
                ));

        Comparator<Concert> comparator;
        if (sort == ConcertSortType.latest) {
            comparator = Comparator.comparing(Concert::getStartDate).reversed();
        } else {
            comparator = Comparator.comparing(Concert::getEndDate);
        }

        return concerts.stream()
                .sorted(comparator)
                .map(concert -> {
                    String venueName = venueNameMap.getOrDefault(concert.getConcertId(), "");
                    return ConcertListResponse.of(concert, venueName);
                })
                .collect(Collectors.toList());
    }

    public ConcertDetailResponse getConcertDetail(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ServiceException(ErrorCode.CONCERT_NOT_FOUND));

        Schedule schedule = scheduleRepository.findWithVenueByConcertId(concertId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ServiceException(ErrorCode.CONCERT_SCHEDULE_EMPTY));

        Venue venue = schedule.getVenue();

        List<String> detailUrlList = concertDeatilRepository
                .findByConcertConcertId(concertId)
                .stream()
                .map(ConcertDetail::getUrlDetail)
                .collect(Collectors.toList());

        Map<String, Integer> prices = scheduleSeatRepository
                .findByScheduleScheduleId(schedule.getScheduleId())
                .stream()
                .collect(Collectors.toMap(
                        ScheduleSeat::getGradeName,
                        ScheduleSeat::getSeatPrice,
                        (v1, v2) -> v1
                ));

        boolean bookable = concert.getEndDate().isAfter(LocalDateTime.now());

        return ConcertDetailResponse.of(
                concert,
                venue.getVenueName(),
                venue.getLocation(),
                detailUrlList,
                prices,
                bookable
        );
    }

    public SeatSelectionResponse getSeatSelection(Long concertId, Long scheduleId) {
        if (!concertRepository.existsById(concertId)) {
            throw new ServiceException(ErrorCode.CONCERT_NOT_FOUND);
        }
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (!schedule.getConcert().getConcertId().equals(concertId)) {
            throw new ServiceException(ErrorCode.INVALID_CONCERT_SCHEDULE);
        }

        List<ScheduleSeat> scheduleSeats = scheduleSeatRepository.findByScheduleScheduleId(scheduleId);

        Map<String, Integer> pricesMap = scheduleSeats.stream()
                .collect(Collectors.toMap(
                        ScheduleSeat::getGradeName,
                        ScheduleSeat::getSeatPrice,
                        (oldPrice, newPrice) -> oldPrice
                ));

        List<SeatDetailResponse> seatDetailList = scheduleSeats.stream()
                .map(seat -> new SeatDetailResponse(
                        seat.getSeatNumber(),
                        seat.getSeatStatus(),
                        seat.getGradeName()
                ))
                .collect(Collectors.toList());

        return new SeatSelectionResponse(
                concertId,
                scheduleId,
                pricesMap,
                seatDetailList
        );
    }

    @Transactional
    public SeatOccupyResponse seatOccupy(Long concertId, Long scheduleId, String seatNumber, Long userId) {
        ScheduleSeat scheduleSeat = scheduleSeatRepository.findByScheduleScheduleIdAndSeatNumber(scheduleId, seatNumber)
                .orElseThrow(() -> new ServiceException(ErrorCode.SEAT_NOT_FOUND));

        if (scheduleSeat.getSeatStatus() == SeatStatus.SOLD_OUT) {
            throw new ServiceException(ErrorCode.SEAT_ALREADY_SOLD);
        }

        if (scheduleSeat.getSeatStatus() == SeatStatus.HOLD){
            throw new ServiceException(ErrorCode.SEAT_ALREADY_HOLD);
        }

        String redisKey = String.format("seat:occupy:%d:%d:%s", concertId, scheduleId, scheduleSeat.getSeatNumber());
        String occupyToken = UUID.randomUUID().toString();

        Long result = redisTemplate.execute(
                OCCUPY_SCRIPT,
                List.of(redisKey),
                userId.toString(),
                occupyToken,
                String.valueOf(OCCUPY_TTL_SECONDS)
        );

        if (result == null || result == 0L) {
            throw new ServiceException(ErrorCode.SEAT_HELD_BY_OTHER_USER);
        }

        scheduleSeat.updateSeatStatus(SeatStatus.HOLD);

        return new SeatOccupyResponse(occupyToken, OCCUPY_TTL_SECONDS, SeatStatus.HOLD);
    }
}
