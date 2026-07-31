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
    name = "review_bookmark",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_review_bookmark_review_user",
            columnNames = ["review_id", "user_id"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_review_bookmark_user_created",
            columnList = "user_id, create_date",
        ),
    ],
)
class ReviewBookmark private constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    val review: ConcertReview,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val bookmarkId: Long? = null

    companion object {
        fun create(review: ConcertReview, user: User): ReviewBookmark =
            ReviewBookmark(review, user)
    }
}
