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
            "PF_PF232456_231213_0419290.webp",
            "PF_PF232456_231213_0419291.webp",
            "PF_PF232467_231213_0534080.webp",
            "PF_PF232471_231213_0703270.webp",
            "PF_PF232473_231213_0749420.webp",
            "PF_PF232473_231213_0749421.webp",
            "PF_PF232473_231213_0749422.webp",
            "PF_PF232473_231213_0749423.webp",
            "PF_PF232473_231213_0749424.webp",
            "PF_PF233707_240110_0141551.webp",
            "PF_PF234436_240124_0503101.webp",
            "PF_PF235543_250407_1055111.webp",
            "PF_PF241378_202606180107025770.webp",
            "PF_PF244704_240710_0203510.webp",
            "PF_PF271736_202508140203360300.webp",
            "PF_PF282011_202512220522130680.webp",
            "PF_PF282014_202512220548232960.webp",
            "PF_PF282015_202512220548239210.webp",
            "PF_PF282015_202512220548239331.webp",
            "PF_PF282016_202512220558497120.webp",
            "PF_PF282031_202512231029374590.webp",
            "PF_PF282031_202601130309422020.webp",
            "PF_PF282031_202602191023418970.webp",
            "PF_PF282176_202512240254373060.webp",
            "PF_PF283207_202601151011272720.webp",
            "PF_PF283207_202601151011272781.webp",
            "PF_PF283207_202601151011272912.webp",
            "PF_PF283793_202601230347476850.webp",
            "PF_PF283793_202601230347477151.webp",
            "PF_PF283793_202601230347477462.webp",
            "PF_PF283878_202601260548562720.webp",
            "PF_PF294720_202606260242088930.webp",
            "PF_PF294720_202606260242089431.webp",
            "PF_PF294720_202606260242089832.webp",
            "PF_PF294720_202606260242089903.webp",
            "PF_PF294721_202606260242092200.webp",
            "PF_PF294722_202606260248058430.webp",
            "PF_PF294723_202606260249097210.webp",
            "PF_PF294724_202606260254106290.webp",
            "PF_PF294726_202606260300194100.webp",
            "PF_PF294727_202606290211051940.webp",
            "PF_PF294728_202606260313565770.webp",
            "PF_PF294729_202606260320075920.webp",
            "PF_PF294730_202606260326256750.webp"
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
