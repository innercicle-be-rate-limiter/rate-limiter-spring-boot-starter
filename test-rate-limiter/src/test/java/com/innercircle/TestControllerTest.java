package com.innercircle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("")
    void test_case_1() throws Exception {
        // given // when
        int count = 11;
        CountDownLatch countDownLatch = new CountDownLatch(count);
        ExecutorService service = Executors.newFixedThreadPool(count);
        try {
            for (int i = 0; i < count; i++) {
                service.submit(() -> {
                    try {
                        this.mockMvc.perform(get("/api/test?key=1")).andDo(print());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } finally {
            service.shutdown();
            countDownLatch.countDown();
        }
        // then

    }

}