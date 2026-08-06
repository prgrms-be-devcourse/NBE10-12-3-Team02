package com.back.global.initData

import com.back.domain.concert.repository.ConcertRepository
import com.back.global.initData.fixture.*
import com.back.global.rsData.RsData
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/test")
@Profile("test")
class TestDatabaseResetController(
    private val entityManager: EntityManager,
    private val redisTemplate: StringRedisTemplate,
    private val concertRepository: ConcertRepository,
    private val venueFixture: VenueFixture,
    private val concertFixture: ConcertFixture,
    private val concertDetailFixture: ConcertDetailFixture,
    private val scheduleFixture: ScheduleFixture,
    private val scheduleSeatFixture: ScheduleSeatFixture
) {

    @PostMapping("/reset-db")
    @Transactional
    fun resetDb(): RsData<String> {
        runCatching {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate()
            val tableNames = entityManager.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'"
            ).resultList as List<*>

            for (table in tableNames) {
                val tableName = table.toString()
                if (!tableName.startsWith("HT_")) {
                    entityManager.createNativeQuery("TRUNCATE TABLE $tableName").executeUpdate()
                }
            }
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate()
        }

        runCatching {
            redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
        }

        if (concertRepository.count() == 0L) {
            runCatching {
                val venues = venueFixture.createVenues()
                val concerts = concertFixture.createConcerts()
                concertDetailFixture.createDetails(concerts)
                val schedules = scheduleFixture.createSchedules(concerts, venues)
                scheduleSeatFixture.createSeats(schedules)
            }
        }

        return RsData("200-1", "Database & Redis Reset Success", "OK")
    }
}
