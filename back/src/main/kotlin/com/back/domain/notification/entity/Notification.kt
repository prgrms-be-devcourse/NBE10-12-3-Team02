package com.back.domain.notification.entity

import com.back.domain.user.entity.User
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "notification",
    indexes = [
        Index(
            name = "idx_notification_receiver",
            columnList = "receiver_id",
        ),
    ],
)
class Notification private constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    val receiver: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    val actor: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: NotificationType,

    @Column(name = "target_type")
    val targetType: String?,

    @Column(name = "target_id")
    val targetId: Long?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val notificationId: Long? = null

    @Column(nullable = false)
    var isRead: Boolean = false
        protected set

    fun markAsRead() {
        isRead = true
    }

    companion object {
        fun ofLike(receiver: User, actor: User, postId: Long): Notification =
            Notification(receiver, actor, NotificationType.LIKE, "POST", postId)

        fun ofFollow(receiver: User, actor: User): Notification =
            Notification(receiver, actor, NotificationType.FOLLOW, null, null)
    }
}
