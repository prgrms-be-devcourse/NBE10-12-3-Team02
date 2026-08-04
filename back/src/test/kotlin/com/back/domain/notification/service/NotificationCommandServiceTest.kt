package com.back.domain.notification.service

import com.back.domain.notification.entity.NotificationType
import com.back.domain.notification.repository.NotificationRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils

class NotificationCommandServiceTest {
    private val notificationRepository = mock(NotificationRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val service = NotificationCommandService(notificationRepository, userRepository)

    private fun user(id: Long, name: String): User {
        val user = User.create("login-$id", "user$id@test.com", "0000", name, LoginType.NORMAL)
        ReflectionTestUtils.setField(user, "userId", id)
        return user
    }

    @Test
    @DisplayName("좋아요 알림: receiver/actor가 모두 존재하면 저장한다")
    fun saveLikeNotification_success() {
        val receiver = user(1L, "받는사람")
        val actor = user(2L, "누른사람")
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(receiver)
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(2L)).thenReturn(actor)
        `when`(notificationRepository.save(any(com.back.domain.notification.entity.Notification::class.java)))
            .thenAnswer { it.getArgument(0) }

        val result = service.saveLikeNotification(1L, 2L, 10L)

        assertThat(result).isNotNull
        assertThat(result!!.type).isEqualTo(NotificationType.LIKE)
        assertThat(result.targetId).isEqualTo(10L)
        assertThat(result.receiver).isSameAs(receiver)
        assertThat(result.actor).isSameAs(actor)
    }

    @Test
    @DisplayName("좋아요 알림: receiver가 존재하지 않으면 null을 반환하고 저장하지 않는다")
    fun saveLikeNotification_receiverNotFound_returnsNull() {
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(null)

        val result = service.saveLikeNotification(1L, 2L, 10L)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("좋아요 알림: actor가 존재하지 않으면 null을 반환하고 저장하지 않는다")
    fun saveLikeNotification_actorNotFound_returnsNull() {
        val receiver = user(1L, "받는사람")
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(receiver)
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(2L)).thenReturn(null)

        val result = service.saveLikeNotification(1L, 2L, 10L)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("팔로우 알림: receiver/actor가 모두 존재하면 저장한다")
    fun saveFollowNotification_success() {
        val receiver = user(1L, "팔로우당한사람")
        val actor = user(2L, "팔로우한사람")
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(receiver)
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(2L)).thenReturn(actor)
        `when`(notificationRepository.save(any(com.back.domain.notification.entity.Notification::class.java)))
            .thenAnswer { it.getArgument(0) }

        val result = service.saveFollowNotification(1L, 2L)

        assertThat(result).isNotNull
        assertThat(result!!.type).isEqualTo(NotificationType.FOLLOW)
        assertThat(result.targetId).isNull()
    }

    @Test
    @DisplayName("팔로우 알림: receiver가 존재하지 않으면 null을 반환하고 저장하지 않는다")
    fun saveFollowNotification_receiverNotFound_returnsNull() {
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(null)

        val result = service.saveFollowNotification(1L, 2L)

        assertThat(result).isNull()
    }
}
