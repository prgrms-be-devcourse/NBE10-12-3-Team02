package com.back.domain.post.entity

import com.back.domain.user.entity.User
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
open class PostComment protected constructor() : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val commentId: Long? = null

    // 저장 후에는 항상 not-null인 PK. 조회된(영속화된) PostComment에서만 사용해야 한다.
    val commentIdOrThrow: Long
        get() = checkNotNull(commentId) { "commentId must not be null after persist" }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    lateinit var post: ConcertPost
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    lateinit var content: String
        protected set

    private constructor(post: ConcertPost, user: User, content: String) : this() {
        this.post = post
        this.user = user
        this.content = content
    }

    companion object {
        @JvmStatic
        fun create(post: ConcertPost, user: User, content: String): PostComment =
            PostComment(post, user, content)
    }
}
