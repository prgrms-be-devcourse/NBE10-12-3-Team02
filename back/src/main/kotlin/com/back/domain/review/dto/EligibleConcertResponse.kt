package com.back.domain.review.dto

import com.back.domain.concert.entity.Concert

data class EligibleConcertResponse(
    val concertId: Long,
    val concertTitle: String,
    val posterUrl: String?
) {
    companion object {
        fun of(concert: Concert): EligibleConcertResponse =
            EligibleConcertResponse(
                concertId = concert.concertId!!,
                concertTitle = concert.concertName,
                posterUrl = concert.urlPoster
            )
    }
}
