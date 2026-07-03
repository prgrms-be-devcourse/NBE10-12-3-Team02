package com.back.global.initData.fixture;

import com.back.domain.concert.entity.Concert;
import com.back.domain.concert.entity.ConcertDetail;
import com.back.domain.concert.repository.ConcertDeatilRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ConcertDetailFixture {
    private final ConcertDeatilRepository concertDeatilRepository;
    private static final String BASE_URL = "/images/concerts/";
    private static final Pattern MT20ID_PATTERN = Pattern.compile("(PF\\d+)");

    private static final List<String> DETAIL_IMAGE_FILES = List.of(
            "PF232456.png",
            "PF232467.png",
            "PF232471.png",
            "PF232473.png",
            "PF233707.png",
            "PF234436.png",
            "PF235543.png",
            "PF241378.png",
            "PF244704.png",
            "PF271736.png",
            "PF282011.png",
            "PF_PF282014_202512220548232960.webp",
            "PF282015.png",
            "PF282016.png",
            "PF282031.png",
            "PF282176.png",
            "PF283207.png",
            "PF283793.png",
            "PF_PF283878_202601260548562720.webp",
            "PF294720.png",
            "PF294721.png",
            "PF294722.png",
            "PF_PF294723_202606260249097210.webp",
            "PF_PF294724_202606260254106290.webp",
            "PF294726.png",
            "PF294727.png",
            "PF294728.png",
            "PF294729.png",
            "PF294730.png"
    );
    public List<ConcertDetail> createDetails(List<Concert> concerts) {
        List<ConcertDetail> details = new ArrayList<>();

        for (Concert concert : concerts) {
            String mt20Id = extractMt20Id(concert.getUrlPoster());
            if (mt20Id == null) continue;

            List<String> matchedFiles = DETAIL_IMAGE_FILES.stream()
                    .filter(filename -> mt20Id.equals(extractMt20Id(filename)))
                    .sorted()
                    .collect(Collectors.toList());

            for (String filename : matchedFiles) {
                details.add(ConcertDetail.create(concert, BASE_URL + filename));
            }
        }

        return concertDeatilRepository.saveAll(details);
    }

    private String extractMt20Id(String text) {
        if (text == null) return null;
        Matcher matcher = MT20ID_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
