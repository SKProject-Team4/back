package SK_3team.example.planner.controller;
import SK_3team.example.planner.dto.UserDTO;
import SK_3team.example.planner.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/users/register")
    public String saveForm(){ return "save";}

    @PostMapping("/users/register")
    public String save(@ModelAttribute UserDTO userDTO){
        System.out.println("UserController.save");
        userService.save(userDTO);
        return "login";
    }

    @GetMapping("/users/login")
    public String loginForm(){
        return "login";
    }


}
