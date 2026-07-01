package com.back.global.initData.fixture;

import com.back.domain.venue.entity.Venue;
import com.back.domain.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VenueFixture {
    private final VenueRepository venueRepository;

    public List<Venue> createVenues() {
        List<Venue> venues = new ArrayList<>();
        venues.add(Venue.create("부산문화회관", "부산광역시", 1500L));
        venues.add(Venue.create("콘텐츠박스(구. 르메이에르 씨어터)", "서울특별시", 350L));
        venues.add(Venue.create("명동난타극장", "서울특별시", 500L));
        venues.add(Venue.create("한국소리문화의전당", "전북특별자치도", 1800L));
        venues.add(Venue.create("공간아울", "서울특별시", 200L));
        venues.add(Venue.create("빛섬아트갤러리", "충청남도", 150L));
        venues.add(Venue.create("대백프라자", "대구광역시", 300L));
        venues.add(Venue.create("전일빌딩245", "광주광역시", 250L));
        venues.add(Venue.create("공간춤", "충청북도", 120L));
        venues.add(Venue.create("월드아트 서커스 [당진]", "충청남도", 800L));
        venues.add(Venue.create("송도 건원테크노큐브", "인천광역시", 400L));
        venues.add(Venue.create("카디션(CARDICIAN)", "서울특별시", 100L));
        venues.add(Venue.create("모두예술극장", "서울특별시", 600L));
        venues.add(Venue.create("entry55 [사당]", "서울특별시", 120L));
        venues.add(Venue.create("오감클래식", "경기도", 150L));
        venues.add(Venue.create("강동아트센터", "서울특별시", 700L));
        venues.add(Venue.create("성수 드림시어터", "서울특별시", 400L));
        venues.add(Venue.create("금정문화회관", "부산광역시", 900L));
        venues.add(Venue.create("AG아트홀 [청주]", "충청북도", 250L));
        venues.add(Venue.create("아지토 라이브 홀", "서울특별시", 200L));
        venues.add(Venue.create("더뮤지션", "전북특별자치도", 150L));
        venues.add(Venue.create("보헤미안 소극장", "광주광역시", 180L));
        venues.add(Venue.create("노원구민의전당(구. 노원구민회관)", "서울특별시", 900L));
        venues.add(Venue.create("(재) 영화의전당", "부산광역시", 4000L));
        venues.add(Venue.create("김천시문화예술회관", "경상북도", 1000L));
        venues.add(Venue.create("김천시립문화회관", "경상북도", 700L));
        venues.add(Venue.create("공상온도", "서울특별시", 150L));
        venues.add(Venue.create("KT&G 상상마당 [춘천]", "강원특별자치도", 300L));
        venues.add(Venue.create("엑스코(exco)", "대구광역시", 5000L));
        return venueRepository.saveAll(venues);
    }
}