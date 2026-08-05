package com.back.domain.ticket.service

import com.back.domain.concert.entity.Concert
import com.back.domain.concert.repository.ConcertRepository
import com.back.domain.concert.service.SeatOccupyManager
import com.back.domain.schedule.constant.SeatStatus
import com.back.domain.schedule.entity.Schedule
import com.back.domain.schedule.entity.ScheduleSeat
import com.back.domain.schedule.repository.ScheduleRepository
import com.back.domain.schedule.repository.ScheduleSeatRepository
import com.back.domain.ticket.dto.PaymentTicketRequest
import com.back.domain.ticket.dto.SeatHoldInfo
import com.back.domain.ticket.repository.TicketRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.domain.venue.entity.Venue
import com.back.domain.venue.repository.VenueRepository
import com.back.global.RedisTestConfig
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisTestConfig::class)
class TicketServiceTest {

    @Autowired
    private lateinit var ticketService: TicketService

    @Autowired
    private lateinit var seatOccupyManager: SeatOccupyManager

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var concertRepository: ConcertRepository

    @Autowired
    private lateinit var venueRepository: VenueRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var scheduleSeatRepository: ScheduleSeatRepository

    @Autowired
    private lateinit var ticketRepository: TicketRepository

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    private lateinit var user: User
    private lateinit var concert: Concert
    private lateinit var schedule: Schedule
    private lateinit var seat1: ScheduleSeat
    private lateinit var seat2: ScheduleSeat

    @BeforeEach
    fun setUp() {
        ticketRepository.deleteAll()
        scheduleSeatRepository.deleteAll()
        scheduleRepository.deleteAll()
        concertRepository.deleteAll()
        venueRepository.deleteAll()
        userRepository.deleteAll()

        user = userRepository.save(
            User.create("test-user-login-id", "test@example.com", "encodedPassword", "테스터", LoginType.NORMAL)
        )

        concert = concertRepository.save(
            Concert.create("아이유 콘서트", "설명", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "poster.jpg")
        )
        val venue = venueRepository.save(Venue.create("체조경기장", "서울", 10000L))
        schedule = scheduleRepository.save(Schedule.create(concert, venue, LocalDateTime.now().plusDays(7), 1))

        seat1 = scheduleSeatRepository.save(
            ScheduleSeat.create(schedule, "VIP", "A-1", 150000, SeatStatus.AVAILABLE)
        )
        seat2 = scheduleSeatRepository.save(
            ScheduleSeat.create(schedule, "VIP", "A-2", 150000, SeatStatus.AVAILABLE)
        )

        val pattern = "seat:occupy:${concert.concertId}:${schedule.scheduleId}:*"
        val keys = stringRedisTemplate.keys(pattern)
        if (!keys.isNullOrEmpty()) {
            stringRedisTemplate.delete(keys)
        }
        stringRedisTemplate.delete(SeatOccupyManager.EXPIRE_QUEUE_KEY)
    }

    @Test
    @DisplayName("티켓 예매 성공")
    fun createTicket_success() {
        val occupyResponse = seatOccupyManager.seatOccupy(
            concert.concertId!!,
            schedule.scheduleId!!,
            seat1.seatNumber,
            user.userId!!
        )

        val request = PaymentTicketRequest(
            concertId = concert.concertId!!,
            seatHolds = listOf(SeatHoldInfo(seat1.seatNumber, occupyResponse.occupyToken))
        )

        val responses = ticketService.createTicket(user.userId!!, schedule.scheduleId!!, request)

        assertThat(responses).hasSize(1)
        assertThat(responses[0].seatNumber).isEqualTo("A-1")

        val updatedSeat = scheduleSeatRepository.findById(seat1.concertSeatPriceId!!).get()
        assertThat(updatedSeat.seatStatus).isEqualTo(SeatStatus.SOLD_OUT)

        val tickets = ticketRepository.findAll()
        assertThat(tickets).hasSize(1)
        assertThat(tickets[0].isValid).isTrue()
    }

    @Test
    @DisplayName("1인 3매 초과 예매 차단")
    fun createTicket_exceedLimit_throwsException() {
        val occ1 = seatOccupyManager.seatOccupy(concert.concertId!!, schedule.scheduleId!!, seat1.seatNumber, user.userId!!)
        val occ2 = seatOccupyManager.seatOccupy(concert.concertId!!, schedule.scheduleId!!, seat2.seatNumber, user.userId!!)

        val req1 = PaymentTicketRequest(
            concertId = concert.concertId!!,
            seatHolds = listOf(
                SeatHoldInfo(seat1.seatNumber, occ1.occupyToken),
                SeatHoldInfo(seat2.seatNumber, occ2.occupyToken)
            )
        )
        ticketService.createTicket(user.userId!!, schedule.scheduleId!!, req1)

        val seat3 = scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-3", 150000, SeatStatus.AVAILABLE))
        val seat4 = scheduleSeatRepository.save(ScheduleSeat.create(schedule, "VIP", "A-4", 150000, SeatStatus.AVAILABLE))

        val occ3 = seatOccupyManager.seatOccupy(concert.concertId!!, schedule.scheduleId!!, seat3.seatNumber, user.userId!!)
        val occ4 = seatOccupyManager.seatOccupy(concert.concertId!!, schedule.scheduleId!!, seat4.seatNumber, user.userId!!)

        val req2 = PaymentTicketRequest(
            concertId = concert.concertId!!,
            seatHolds = listOf(
                SeatHoldInfo(seat3.seatNumber, occ3.occupyToken),
                SeatHoldInfo(seat4.seatNumber, occ4.occupyToken)
            )
        )

        assertThatThrownBy {
            ticketService.createTicket(user.userId!!, schedule.scheduleId!!, req2)
        }.isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.EXCEED_TICKET_LIMIT.message)
    }

    @Test
    @DisplayName("선점 만료 시 예매 차단")
    fun createTicket_expiredHold_throwsException() {
        val request = PaymentTicketRequest(
            concertId = concert.concertId!!,
            seatHolds = listOf(SeatHoldInfo(seat1.seatNumber, "invalid-token"))
        )

        assertThatThrownBy {
            ticketService.createTicket(user.userId!!, schedule.scheduleId!!, request)
        }.isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.SEAT_HOLD_EXPIRED.message)
    }

    @Test
    @DisplayName("티켓 예매 취소 성공")
    fun cancelTicket_success() {
        val occ = seatOccupyManager.seatOccupy(concert.concertId!!, schedule.scheduleId!!, seat1.seatNumber, user.userId!!)
        val req = PaymentTicketRequest(concertId = concert.concertId!!, seatHolds = listOf(SeatHoldInfo(seat1.seatNumber, occ.occupyToken)))
        ticketService.createTicket(user.userId!!, schedule.scheduleId!!, req)

        val ticket = ticketRepository.findAll()[0]
        val ticketId = ticket.ticketId!!

        ticketService.cancelTicket(user.userId!!, ticketId)

        val cancelledTicket = ticketRepository.findById(ticketId).get()
        assertThat(cancelledTicket.isValid).isFalse()

        val restoredSeat = scheduleSeatRepository.findById(seat1.concertSeatPriceId!!).get()
        assertThat(restoredSeat.seatStatus).isEqualTo(SeatStatus.AVAILABLE)
    }
}
