package com.back.domain.review.entity

import com.back.domain.user.entity.User
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
open class ReviewComment protected constructor() : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val commentId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    lateinit var review: ConcertReview
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    lateinit var content: String
        protected set

    private constructor(review: ConcertReview, user: User, content: String) : this() {
        this.review = review
        this.user = user
        this.content = content
    }

    companion object {
        @JvmStatic
        fun create(review: ConcertReview, user: User, content: String): ReviewComment =
            ReviewComment(review, user, content)
    }
}
