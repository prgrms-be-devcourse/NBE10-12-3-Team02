package com.back.domain.post.entity

import com.back.domain.concert.entity.Concert
import com.back.domain.user.entity.User
import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "concert_post",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_concert_post_concert_user",
            columnNames = ["concert_id", "user_id"],
        ),
    ],
)
open class ConcertPost protected constructor() : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val postId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    lateinit var concert: Concert
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User
        protected set

    @Column(nullable = false, length = 100)
    lateinit var title: String
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    lateinit var content: String
        protected set

    @OneToMany(
        mappedBy = "post",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    private val comments: MutableList<PostComment> = mutableListOf()

    private constructor(
        concert: Concert,
        user: User,
        title: String,
        content: String
    ) : this() {
        this.concert = concert
        this.user = user
        this.title = title
        this.content = content
    }

    fun update(title: String, content: String) {
        this.title = title
        this.content = content
    }

    companion object {
        @JvmStatic
        fun create(concert: Concert, user: User, title: String, content: String): ConcertPost =
            ConcertPost(concert, user, title, content)
    }
}
