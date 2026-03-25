package com.example.superapp.controller;

import com.example.superapp.dto.MovieItemDto;
import com.example.superapp.service.MaybeYouWantToWatchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/recommendations")
@RequiredArgsConstructor
public class MaybeYouWantToWatchController {

    private final MaybeYouWantToWatchService service;

    @GetMapping("/maybe-you-want-to-watch")
    public List<MovieItemDto> maybeYouWantToWatch(
            Authentication auth,
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "12") int limit
    ) {
        return service.recommend(auth, request, limit);
    }
}

