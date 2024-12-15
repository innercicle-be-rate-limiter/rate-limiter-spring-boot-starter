package com.innercicle;

import com.innercicle.annotations.RateLimiting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    @GetMapping
    @RateLimiting(name = "test", cacheKey = "#searchKeyword.number")
    public String test(SearchKeyword searchKeyword) {
        log.error("key: {}", searchKeyword.getNumber());
        return "test";
    }

}
