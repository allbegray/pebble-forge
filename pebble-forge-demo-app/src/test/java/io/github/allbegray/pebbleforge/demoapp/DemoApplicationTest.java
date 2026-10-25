package io.github.allbegray.pebbleforge.demoapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageShowsTheDemoHeadingAndSecurityState() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>Pebble Forge Demo</h1>")))
                .andExpect(content().string(containsString("Current security state")))
                .andExpect(content().string(containsString("You are browsing as a guest")));
    }

    @Test
    void componentsPageRendersBothCardsWithPropsAndSlots() throws Exception {
        mockMvc.perform(get("/components"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sandboxed slot content")))
                .andExpect(content().string(containsString("Different props")))
                .andExpect(content().string(containsString("Named slots keep layout inside the component")))
                .andExpect(content().string(containsString("Same component, new values")))
                .andExpect(content().string(containsString("Default slot text for the badge.")))
                .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;nope&#39;)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert('nope')</script>"))));
    }

    @Test
    void languagePageRendersSwitchForBreakContinueAndWith() throws Exception {
        mockMvc.perform(get("/language"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin control panel")))
                .andExpect(content().string(containsString("<li class=\"loop-value\">1</li>")))
                .andExpect(content().string(containsString("<li class=\"loop-value\">3</li>")))
                .andExpect(content().string(containsString("<li class=\"loop-value\">4</li>")))
                .andExpect(content().string(not(containsString("<li class=\"loop-value\">2</li>"))))
                .andExpect(content().string(not(containsString("<li class=\"loop-value\">5</li>"))))
                .andExpect(content().string(containsString("Running <strong>Pebble Forge</strong> 0.1 in the")))
                .andExpect(content().string(containsString("demo environment.")));
    }

    @Test
    void unauthenticatedAdminRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void authenticatedAdminSeesTheProtectedPage() throws Exception {
        mockMvc.perform(get("/admin").with(testSecurityContext()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>Admin</h1>")))
                .andExpect(content().string(containsString("<a href=\"/admin\">Admin</a>")))
                .andExpect(content().string(containsString("<dd>admin</dd>")))
                .andExpect(content().string(containsString("ROLE_ADMIN")));
    }

    @Test
    void formLoginAuthenticatesTheAdminAccount() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"));
    }

    @Test
    void loginPageRendersCsrfFieldAndPasswordInput() throws Exception {
        mockMvc.perform(get("/login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("type=\"hidden\"")))
                .andExpect(content().string(containsString("type=\"password\"")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(not(containsString("Invalid username or password."))));
    }

    @Test
    void loginPageShowsErrorTextWhenParamErrorIsPresent() throws Exception {
        mockMvc.perform(get("/login").param("error", "").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid username or password.")));
    }
}
