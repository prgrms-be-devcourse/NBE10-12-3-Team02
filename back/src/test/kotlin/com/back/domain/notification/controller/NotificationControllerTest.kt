package com.back.domain.notification.controller

import com.back.domain.notification.entity.Notification
import com.back.domain.notification.repository.NotificationRepository
import com.back.domain.user.constant.LoginType
import com.back.domain.user.entity.User
import com.back.domain.user.repository.UserRepository
import com.back.global.RedisTestConfig
import com.back.global.security.SecurityUser
import com.back.global.security.jwt.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

// subscribe_withQueryToken_success는 .with(user(...))를 쓰지 않고 실제 필터 체인을 타므로
// BlacklistRepository가 실제 Redis(StringRedisTemplate)를 호출한다. CI에는 Redis 서비스가 없어
// RedisTestConfig 없이는 RedisConnectionFailureException으로 실패한다 (로컬은 개발용 Redis가 떠 있어 우연히 통과).
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(RedisTestConfig::class)
@Transactional
class NotificationControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    private lateinit var receiver: User
    private lateinit var actor: User
    private lateinit var securityUser: SecurityUser

    @BeforeEach
    fun setUp() {
        receiver = userRepository.save(
            User.create("noti-receiver", "noti-receiver@test.com", "0000", "받는사람", LoginType.NORMAL)
        )
        actor = userRepository.save(
            User.create("noti-actor", "noti-actor@test.com", "0000", "행동한사람", LoginType.NORMAL)
        )
        securityUser = SecurityUser(receiver.userId!!, receiver.name)
    }

    @Test
    @DisplayName("내 알림 목록 조회 성공")
    fun getMyNotifications() {
        notificationRepository.save(Notification.ofFollow(receiver, actor))

        mockMvc.perform(
            get("/api/v1/notifications")
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].type").value("FOLLOW"))
    }

    @Test
    @DisplayName("안읽은 알림 개수 조회 성공")
    fun getUnreadCount() {
        notificationRepository.save(Notification.ofFollow(receiver, actor))
        notificationRepository.save(Notification.ofLike(receiver, actor, 1L))

        mockMvc.perform(
            get("/api/v1/notifications/unread-count")
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(2))
    }

    @Test
    @DisplayName("본인 알림 읽음 처리 성공")
    fun markAsRead_success() {
        val notification = notificationRepository.save(Notification.ofFollow(receiver, actor))

        mockMvc.perform(
            patch("/api/v1/notifications/{notificationId}/read", notification.notificationId)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)

        assertThat(notificationRepository.findById(notification.notificationId!!).get().isRead).isTrue
    }

    @Test
    @DisplayName("타인의 알림을 읽음 처리하면 403")
    fun markAsRead_forbidden() {
        val notification = notificationRepository.save(Notification.ofFollow(receiver, actor))
        val otherSecurityUser = SecurityUser(actor.userId!!, actor.name)

        mockMvc.perform(
            patch("/api/v1/notifications/{notificationId}/read", notification.notificationId)
                .with(user(otherSecurityUser))
        )
            .andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-7"))
    }

    @Test
    @DisplayName("존재하지 않는 알림을 읽음 처리하면 404")
    fun markAsRead_notFound() {
        mockMvc.perform(
            patch("/api/v1/notifications/{notificationId}/read", Long.MAX_VALUE)
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-12"))
    }

    @Test
    @DisplayName("전체 알림 읽음 처리 성공")
    fun markAllAsRead_success() {
        notificationRepository.save(Notification.ofFollow(receiver, actor))
        notificationRepository.save(Notification.ofLike(receiver, actor, 1L))

        mockMvc.perform(
            patch("/api/v1/notifications/read-all")
                .with(user(securityUser))
        )
            .andDo(print())
            .andExpect(status().isOk)

        assertThat(notificationRepository.countByReceiverUserIdAndIsReadFalse(receiver.userId!!)).isZero()
    }

    @Test
    @DisplayName("비로그인 사용자는 알림 목록 조회 불가")
    fun getMyNotifications_unauthorized() {
        mockMvc.perform(get("/api/v1/notifications"))
            .andDo(print())
            .andExpect(status().isUnauthorized)
    }

    // SseEmitter는 의도적으로 완료되지 않는(계속 열려있는) 스트림이므로,
    // asyncDispatch()로 완료를 기다리면 테스트가 무한 대기한다.
    // 비동기 시작 여부와, 컨트롤러가 동기적으로 써 넣은 최초 이벤트만 확인한다.
    @Test
    @DisplayName("SSE 구독 성공 (Authorization 헤더)")
    fun subscribe_withHeader_success() {
        val result = mockMvc.perform(
            get("/api/v1/notifications/subscribe")
                .with(user(securityUser))
        )
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk)
            .andReturn()

        assertThat(result.response.contentType).startsWith("text/event-stream")
        assertThat(result.response.contentAsString).contains("connect")
    }

    @Test
    @DisplayName("SSE 구독 성공 (쿼리파라미터 token fallback)")
    fun subscribe_withQueryToken_success() {
        val accessToken = jwtTokenProvider.createAccessToken(receiver)

        val result = mockMvc.perform(
            get("/api/v1/notifications/subscribe").param("token", accessToken)
        )
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk)
            .andReturn()

        assertThat(result.response.contentType).startsWith("text/event-stream")
        assertThat(result.response.contentAsString).contains("connect")
    }

    @Test
    @DisplayName("SSE 구독은 토큰이 없으면 401")
    fun subscribe_withoutToken_unauthorized() {
        mockMvc.perform(get("/api/v1/notifications/subscribe"))
            .andDo(print())
            .andExpect(status().isUnauthorized)
    }
}
