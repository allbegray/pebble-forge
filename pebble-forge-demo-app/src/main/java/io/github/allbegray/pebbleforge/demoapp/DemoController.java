package io.github.allbegray.pebbleforge.demoapp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class DemoController {

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("title", "Pebble Forge Demo");
        model.addAttribute("features", List.of(
                "Template-defined components with props, named slots, and default slots",
                "Layout inheritance with extends and blocks",
                "switch, for, break, continue, and with language extensions",
                "Spring Security state and CSRF helpers inside sandboxed templates",
                "Autoescaping on every expression, with no raw output"));
        return "page";
    }

    @GetMapping("/components")
    String components(Model model) {
        model.addAttribute("title", "Components");
        model.addAttribute("suspicious", "<script>alert('nope')</script>");
        return "components";
    }

    @GetMapping("/language")
    String language(Model model) {
        model.addAttribute("title", "Language extensions");
        model.addAttribute("role", "admin");
        model.addAttribute("numbers", List.of(1, 2, 3, 4, 5, 6));
        return "language";
    }

    @GetMapping("/admin")
    String admin(Model model) {
        model.addAttribute("title", "Admin");
        return "admin";
    }

    @GetMapping("/login")
    String login(@RequestParam(name = "error", required = false) String error, Model model) {
        model.addAttribute("title", "Sign in");
        model.addAttribute("param", Map.of("error", error != null));
        return "login";
    }
}
