package com.back.domain.follow.entity

import com.back.domain.user.entity.User
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "follow",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_follow_follower_followee",
            columnNames = ["follower_id", "followee_id"],
        ),
    ],
)
class Follow private constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    val follower: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    val followee: User,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val followId: Long? = null

    companion object {
        fun create(follower: User, followee: User): Follow =
            Follow(follower, followee)
    }
}
