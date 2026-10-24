package io.github.allbegray.pebbleforge.spring.boot.autoconfigure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = PebbleForgeWebIntegrationTest.TestApplication.class,
        properties = "pebble.forge.sandbox.enabled=true")
@AutoConfigureMockMvc
class PebbleForgeWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rendersAComponentAndSecurityStateThroughTheOfficialViewResolver() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("ada", "n/a", "ROLE_ADMIN"));

        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<p>Hello Ada</p>")))
                .andExpect(content().string(containsString("<span>signed-in</span>")));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestApplication.HelloController.class)
    static class TestApplication {

        @Controller
        static class HelloController {

            @GetMapping("/hello")
            String hello(Model model) {
                model.addAttribute("name", "Ada");
                return "view";
            }
        }
    }
}
