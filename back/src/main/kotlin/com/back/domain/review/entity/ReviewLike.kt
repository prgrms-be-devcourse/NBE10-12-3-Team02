package com.back.domain.review.entity

import com.back.domain.user.entity.User
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "review_like",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_review_like_review_user",
            columnNames = ["review_id", "user_id"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_review_like_user",
            columnList = "user_id",
        ),
    ],
)
class ReviewLike private constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    val review: ConcertReview,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val likeId: Long? = null

    companion object {
        fun create(review: ConcertReview, user: User): ReviewLike =
            ReviewLike(review, user)
    }
}
