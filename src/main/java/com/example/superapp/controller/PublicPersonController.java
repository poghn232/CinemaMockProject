package com.example.superapp.controller;

import com.example.superapp.dto.MovieItemDto;
import com.example.superapp.dto.PersonDetailDto;
import com.example.superapp.dto.PersonDto;
import com.example.superapp.dto.PersonListResponseDto;
import com.example.superapp.entity.Movie;
import com.example.superapp.entity.Person;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.repository.PersonRepository;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.TvSeriesRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/people")
@RequiredArgsConstructor
public class PublicPersonController {
    private final PersonRepository personRepository;
    private final MovieRepository movieRepository;
    private final TvSeriesRepository tvSeriesRepository;

    @Value("${tmdb.image-base-url}")
    private String imageBaseUrl;

    @GetMapping
    public List<PersonDto> listPeople() {
        List<Person> all = personRepository.findAll();
        return all.stream().map(p -> {
            PersonDto dto = new PersonDto();
            dto.setId(p.getId());
            dto.setName(p.getName());
            if (p.getProfilePath() != null && !p.getProfilePath().isBlank()) dto.setProfilePath(imageBaseUrl + p.getProfilePath());
            return dto;
        }).collect(Collectors.toList());
    }

    // New paginated endpoint used by the actors page. page is 1-based.
    @GetMapping("/page")
    public PersonListResponseDto listPeoplePage(@RequestParam(value = "page", defaultValue = "1") int page,
                                               @RequestParam(value = "q", required = false) String q) {
        int size = 20;
        // For ordering by number of appearances (credits), compute credits counts server-side and sort.
        List<Person> all;
        if (q != null && !q.isBlank()) {
            all = personRepository.findByNameContainingIgnoreCase(q.trim());
        } else {
            all = personRepository.findAll();
        }

        // compute credits count for each person (movie + tv)
        List<java.util.Map.Entry<Person, Integer>> withCounts = new ArrayList<>();
        for (Person p : all) {
            int mc = p.getMovieCredits() != null ? p.getMovieCredits().size() : 0;
            int tc = p.getTvCredits() != null ? p.getTvCredits().size() : 0;
            withCounts.add(new java.util.AbstractMap.SimpleEntry<>(p, mc + tc));
        }
        // sort desc by credits count
        withCounts.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int totalElements = withCounts.size();
        int totalPages = (int) Math.max(1, Math.ceil((double) totalElements / size));
        int currentPage = Math.max(1, page);
        int fromIndex = Math.min(totalElements, (currentPage - 1) * size);
        int toIndex = Math.min(totalElements, fromIndex + size);

        List<PersonDto> items = new ArrayList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            Person p = withCounts.get(i).getKey();
            int creditsCount = withCounts.get(i).getValue();
            PersonDto dto = new PersonDto();
            dto.setId(p.getId());
            dto.setName(p.getName());
            if (p.getProfilePath() != null && !p.getProfilePath().isBlank()) dto.setProfilePath(imageBaseUrl + p.getProfilePath());
            dto.setCreditsCount(creditsCount);
            items.add(dto);
        }

        PersonListResponseDto resp = new PersonListResponseDto();
        resp.setItems(items);
        resp.setPage(currentPage);
        resp.setTotalPages(totalPages);
        resp.setTotalElements(totalElements);
        return resp;
    }

    @GetMapping("/{id}")
    @Transactional
    public PersonDetailDto getPerson(@PathVariable("id") Long id) {
        Person p = personRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Person not found"));
        PersonDetailDto dto = new PersonDetailDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setBiography(p.getBiography());
        if (p.getBirthday() != null) dto.setBirthday(p.getBirthday().toString());
        if (p.getProfilePath() != null && !p.getProfilePath().isBlank()) dto.setProfilePath(imageBaseUrl + p.getProfilePath());

        List<MovieItemDto> credits = new ArrayList<>();
        // movie credits
        if (p.getMovieCredits() != null) {
            p.getMovieCredits().forEach(mc -> {
                Movie m = mc.getMovie();
                if (m == null) return;
                MovieItemDto item = new MovieItemDto();
                item.setId(m.getId());
                item.setType("movie");
                item.setTitle(m.getTitle());
                item.setRating(m.getVoteAverage());
                if (m.getReleaseDate() != null) item.setYear(m.getReleaseDate().getYear());
                if (m.getPosterPath() != null && !m.getPosterPath().isBlank()) item.setImageUrl(imageBaseUrl + m.getPosterPath());
                credits.add(item);
            });
        }
        // tv credits
        if (p.getTvCredits() != null) {
            p.getTvCredits().forEach(tc -> {
                TvSeries tv = tc.getTvSeries();
                if (tv == null) return;
                MovieItemDto item = new MovieItemDto();
                item.setId(tv.getId());
                item.setType("tv");
                item.setTitle(tv.getName());
                item.setRating(tv.getVoteAverage());
                if (tv.getFirstAirDate() != null) item.setYear(tv.getFirstAirDate().getYear());
                if (tv.getPosterPath() != null && !tv.getPosterPath().isBlank()) item.setImageUrl(imageBaseUrl + tv.getPosterPath());
                credits.add(item);
            });
        }

        dto.setCredits(credits);
        return dto;
    }
}
