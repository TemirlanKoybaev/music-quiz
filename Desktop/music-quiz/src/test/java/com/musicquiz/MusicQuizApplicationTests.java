package com.musicquiz;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "app.audio.upload-dir=./test-uploads"
})
class MusicQuizApplicationTests {

    @Test
    void contextLoads() {
    }
}
