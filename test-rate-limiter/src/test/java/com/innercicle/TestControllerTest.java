package com.innercicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("")
    void test() throws Exception {
        // given
        int count = 11;
        CountDownLatch countDownLatch = new CountDownLatch(count);
        try {
            for (int i = 0; i < count; i++) {
                try {
                    this.mockMvc.perform(get("/api/test").param("number", "1111"))
                        .andDo(print());
                } catch (Exception e) {
                    e.printStackTrace();
                    countDownLatch.countDown();
                }
            }
        } finally {
        }

        // when

        // then

    }

}