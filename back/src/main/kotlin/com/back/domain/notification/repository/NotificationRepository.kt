package com.back.domain.notification.repository

import com.back.domain.notification.entity.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun countByReceiverUserIdAndIsReadFalse(receiverId: Long): Long

    @Query(
        value = """
            SELECT n
            FROM Notification n
            JOIN FETCH n.actor
            WHERE n.receiver.userId = :receiverId
            ORDER BY n.createDate DESC
        """,
        countQuery = """
            SELECT COUNT(n)
            FROM Notification n
            WHERE n.receiver.userId = :receiverId
        """,
    )
    fun findPageByReceiverId(
        @Param("receiverId") receiverId: Long,
        pageable: Pageable,
    ): Page<Notification>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.userId = :receiverId AND n.isRead = false")
    fun markAllAsRead(@Param("receiverId") receiverId: Long): Int
}
