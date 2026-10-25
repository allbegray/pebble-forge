package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import(DiagnosticsTestApplication.Controllers.class)
class DiagnosticsTestApplication {

    @Controller
    static class Controllers {

        @GetMapping("/broken")
        String broken() {
            return "broken-view";
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("boom");
        }
    }
}
