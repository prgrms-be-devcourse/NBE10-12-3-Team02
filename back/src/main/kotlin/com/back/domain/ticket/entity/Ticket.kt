package com.back.domain.ticket.entity

import com.back.domain.schedule.entity.Schedule
import com.back.domain.schedule.entity.ScheduleSeat
import com.back.domain.user.entity.User
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
class Ticket(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    val schedule: Schedule,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_seat_id", nullable = false)
    val scheduleSeat: ScheduleSeat,

    @Column(nullable = false, unique = true)
    val ticketNumber: String,

    @Column
    val groupToken: String? = null,

    @Column(nullable = false)
    val ticketPrice: Int = 0,

    isValid: Boolean = true
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val ticketId: Long? = null

    @Column(nullable = false)
    var isValid: Boolean = isValid
        protected set

    fun cancel() {
        if (!isValid) {
            throw ServiceException(ErrorCode.TICKET_ALREADY_CANCELLED)
        }
        this.isValid = false
    }

    companion object {
        fun create(
            user: User,
            schedule: Schedule,
            scheduleSeat: ScheduleSeat,
            ticketNumber: String,
            ticketPrice: Int,
            groupToken: String
        ): Ticket = Ticket(
            user = user,
            schedule = schedule,
            scheduleSeat = scheduleSeat,
            ticketNumber = ticketNumber,
            groupToken = groupToken,
            ticketPrice = ticketPrice,
            isValid = true
        )
    }
}
