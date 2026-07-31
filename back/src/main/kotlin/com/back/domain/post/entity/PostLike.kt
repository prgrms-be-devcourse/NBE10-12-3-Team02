package com.back.domain.post.entity

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
    name = "post_like",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_post_like_post_user",
            columnNames = ["post_id", "user_id"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_post_like_user",
            columnList = "user_id",
        ),
    ],
)
class PostLike private constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    val post: ConcertPost,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val likeId: Long? = null

    companion object {
        fun create(post: ConcertPost, user: User): PostLike =
            PostLike(post, user)
    }
}
