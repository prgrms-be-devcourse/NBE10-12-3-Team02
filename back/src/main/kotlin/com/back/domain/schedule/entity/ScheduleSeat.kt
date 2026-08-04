package com.back.domain.schedule.entity

import com.back.domain.schedule.constant.SeatStatus

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
class ScheduleSeat protected constructor() : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val concertSeatPriceId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    lateinit var schedule: Schedule
        protected set

    @Column(nullable = false)
    lateinit var gradeName: String
        protected set

    @Column(nullable = false)
    lateinit var seatNumber: String
        protected set

    @Column(nullable = false)
    var seatPrice: Int = 0
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var seatStatus: SeatStatus
        protected set

    private constructor(
        schedule: Schedule,
        gradeName: String,
        seatNumber: String,
        seatPrice: Int,
        seatStatus: SeatStatus
    ) : this() {
        this.schedule = schedule
        this.gradeName = gradeName
        this.seatNumber = seatNumber
        this.seatPrice = seatPrice
        this.seatStatus = seatStatus
    }

    fun occupyHold() {
        if (seatStatus == SeatStatus.SOLD_OUT) {
            throw ServiceException(ErrorCode.SEAT_ALREADY_SOLD)
        }
        if (seatStatus != SeatStatus.HOLD) {
            this.seatStatus = SeatStatus.HOLD
        }
    }

    fun sell() {
        when (seatStatus) {
            SeatStatus.SOLD_OUT -> throw ServiceException(ErrorCode.SEAT_ALREADY_SOLD)
            SeatStatus.HOLD -> this.seatStatus = SeatStatus.SOLD_OUT
            else -> throw ServiceException(ErrorCode.SEAT_HOLD_EXPIRED)
        }
    }

    // 티켓 취소 시 호출: HOLD 또는 SOLD_OUT 상태의 좌석을 AVAILABLE로 복구한다.
    fun releaseToAvailable() {
        if (seatStatus == SeatStatus.HOLD || seatStatus == SeatStatus.SOLD_OUT) {
            this.seatStatus = SeatStatus.AVAILABLE
        }
    }

    // 만료 스케줄러 전용: HOLD → AVAILABLE 전환만 허용한다.
    // releaseToAvailable()과 달리 SOLD_OUT 좌석은 건드리지 않아,
    // 스케줄러가 결제 완료된 좌석을 실수로 해제하는 경로를 원천 차단한다.
    fun releaseHoldToAvailable() {
        if (seatStatus == SeatStatus.HOLD) {
            this.seatStatus = SeatStatus.AVAILABLE
        }
    }

    companion object {
        @JvmStatic
        fun create(
            schedule: Schedule,
            gradeName: String,
            seatNumber: String,
            seatPrice: Int,
            seatStatus: SeatStatus
        ): ScheduleSeat = ScheduleSeat(schedule, gradeName, seatNumber, seatPrice, seatStatus)
    }
}
