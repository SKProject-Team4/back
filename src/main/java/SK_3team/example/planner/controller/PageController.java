package SK_3team.example.planner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/register")
    public String registerPage() {
        return "register"; // templates/register.html
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // templates/login.html
    }

    @GetMapping("/main")
    public String mainPage() {
        return "main"; // templates/main.html
    }
}
