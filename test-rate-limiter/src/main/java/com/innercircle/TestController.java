package com.innercircle;

import com.innercicle.annotations.RateLimiting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    @RateLimiting(name = "test-key", cacheKey = "key")
    public String test(String key) {
        log.info("key:{}", key);
        return "test";
    }

}
