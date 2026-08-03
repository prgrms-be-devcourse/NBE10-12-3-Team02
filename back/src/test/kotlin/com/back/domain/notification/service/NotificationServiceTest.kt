package com.back.domain.notification.service

import com.back.domain.notification.dto.NotificationPushPayload
import com.back.domain.notification.entity.Notification
import com.back.domain.notification.entity.NotificationType
import com.back.domain.notification.event.PostLikedEvent
import com.back.domain.notification.event.UserFollowedEvent
import com.back.domain.notification.repository.NotificationRepository
import com.back.domain.notification.sse.NotificationSseEmitterRegistry
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

class NotificationServiceTest {
    private val notificationRepository = mock(NotificationRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val sseEmitterRegistry = mock(NotificationSseEmitterRegistry::class.java)
    private val service = NotificationService(notificationRepository, userRepository, sseEmitterRegistry)

    // Mockito의 any(Class)는 null을 반환하는데, Kotlin 비-null 파라미터 위치에 그대로 쓰면
    // "any(...) must not be null" NPE가 발생하므로 우회용 헬퍼를 사용한다.
    private fun <T> anyObject(): T {
        any<T>()
        return null as T
    }

    private fun user(id: Long, name: String): User {
        val user = User.create("login-$id", "user$id@test.com", "0000", name, LoginType.NORMAL)
        ReflectionTestUtils.setField(user, "userId", id)
        return user
    }

    @Test
    @DisplayName("좋아요 알림: receiver/actor가 모두 존재하면 알림을 저장하고 SSE로 push한다")
    fun onPostLiked_success() {
        val receiver = user(1L, "받는사람")
        val actor = user(2L, "누른사람")
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(receiver)
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(2L)).thenReturn(actor)

        val saved = Notification.ofLike(receiver, actor, 10L)
        ReflectionTestUtils.setField(saved, "notificationId", 100L)
        `when`(notificationRepository.save(any(Notification::class.java))).thenReturn(saved)

        service.onPostLiked(PostLikedEvent(postId = 10L, postOwnerId = 1L, actorId = 2L))

        verify(notificationRepository, times(1)).save(any(Notification::class.java))
        verify(sseEmitterRegistry, times(1)).send(eq(1L), anyObject<NotificationPushPayload>())
    }

    @Test
    @DisplayName("좋아요 알림: receiver가 존재하지 않으면 알림을 저장하지 않는다")
    fun onPostLiked_receiverNotFound_skips() {
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(null)

        service.onPostLiked(PostLikedEvent(postId = 10L, postOwnerId = 1L, actorId = 2L))

        verify(notificationRepository, never()).save(any(Notification::class.java))
        verify(sseEmitterRegistry, never()).send(anyLong(), anyObject<NotificationPushPayload>())
    }

    @Test
    @DisplayName("팔로우 알림: receiver/actor가 모두 존재하면 알림을 저장하고 SSE로 push한다")
    fun onUserFollowed_success() {
        val receiver = user(1L, "팔로우당한사람")
        val actor = user(2L, "팔로우한사람")
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(receiver)
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(2L)).thenReturn(actor)

        val saved = Notification.ofFollow(receiver, actor)
        ReflectionTestUtils.setField(saved, "notificationId", 200L)
        `when`(notificationRepository.save(any(Notification::class.java))).thenReturn(saved)

        service.onUserFollowed(UserFollowedEvent(followeeId = 1L, followerId = 2L))

        verify(notificationRepository, times(1)).save(any(Notification::class.java))
        verify(sseEmitterRegistry, times(1)).send(eq(1L), anyObject<NotificationPushPayload>())
    }

    @Test
    @DisplayName("팔로우 알림: actor가 존재하지 않으면 알림을 저장하지 않는다")
    fun onUserFollowed_actorNotFound_skips() {
        val receiver = user(1L, "팔로우당한사람")
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(receiver)
        `when`(userRepository.findByUserIdAndDeletedAtIsNull(2L)).thenReturn(null)

        service.onUserFollowed(UserFollowedEvent(followeeId = 1L, followerId = 2L))

        verify(notificationRepository, never()).save(any(Notification::class.java))
        verify(sseEmitterRegistry, never()).send(anyLong(), anyObject<NotificationPushPayload>())
    }

    @Test
    @DisplayName("내 알림 목록을 페이징 조회한다")
    fun getMyNotifications_success() {
        val receiver = user(1L, "받는사람")
        val actor = user(2L, "누른사람")
        val notification = Notification.ofFollow(receiver, actor)
        ReflectionTestUtils.setField(notification, "notificationId", 1L)
        val pageable = PageRequest.of(0, 20)
        `when`(notificationRepository.findPageByReceiverId(1L, pageable))
            .thenReturn(PageImpl(listOf(notification)))

        val result = service.getMyNotifications(1L, pageable)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].notificationId).isEqualTo(1L)
        assertThat(result.content[0].type).isEqualTo(NotificationType.FOLLOW)
    }

    @Test
    @DisplayName("안읽은 알림 개수를 조회한다")
    fun getUnreadCount_success() {
        `when`(notificationRepository.countByReceiverUserIdAndIsReadFalse(1L)).thenReturn(3L)

        val result = service.getUnreadCount(1L)

        assertThat(result).isEqualTo(3L)
    }

    @Test
    @DisplayName("본인 알림을 읽음 처리하면 isRead가 true가 된다")
    fun markAsRead_success() {
        val receiver = user(1L, "받는사람")
        val actor = user(2L, "누른사람")
        val notification = Notification.ofFollow(receiver, actor)
        ReflectionTestUtils.setField(notification, "notificationId", 1L)
        `when`(notificationRepository.findById(1L)).thenReturn(Optional.of(notification))

        service.markAsRead(1L, 1L)

        assertThat(notification.isRead).isTrue
    }

    @Test
    @DisplayName("존재하지 않는 알림을 읽음 처리하면 예외가 발생한다")
    fun markAsRead_notFound_throws() {
        `when`(notificationRepository.findById(999L)).thenReturn(Optional.empty())

        assertThatThrownBy { service.markAsRead(999L, 1L) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.NOTIFICATION_NOT_FOUND.message)
    }

    @Test
    @DisplayName("본인 알림이 아니면 읽음 처리 시 예외가 발생한다")
    fun markAsRead_forbidden_throws() {
        val receiver = user(1L, "받는사람")
        val actor = user(2L, "누른사람")
        val notification = Notification.ofFollow(receiver, actor)
        ReflectionTestUtils.setField(notification, "notificationId", 1L)
        `when`(notificationRepository.findById(1L)).thenReturn(Optional.of(notification))

        assertThatThrownBy { service.markAsRead(1L, 999L) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.NOTIFICATION_FORBIDDEN.message)

        assertThat(notification.isRead).isFalse
    }

    @Test
    @DisplayName("전체 읽음 처리는 repository에 위임한다")
    fun markAllAsRead_success() {
        service.markAllAsRead(1L)

        verify(notificationRepository, times(1)).markAllAsRead(1L)
    }
}
