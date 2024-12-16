package com.innercircle.service;

import com.innercicle.annotations.RateLimiting;
import com.innercicle.testratelimiter.TestController;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    @RateLimiting(name="test", cacheKey = "#parameter.test")
    public void test(TestController.RequestParameter parameter) {
        System.out.println("parameter" + parameter.getTest());
    }
}
