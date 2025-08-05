package hicc_project.RottenToday.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // 인증이 필요한 API (JWT 필요)
    @GetMapping("/protected")
    public String protectedEndpoint() {
        return "🔒 인증된 사용자만 접근 가능합니다.";
    }

    // 누구나 접근 가능한 API (JWT 불필요)
    @GetMapping("/public")
    public String publicEndpoint() {
        return "🌐 누구나 접근 가능한 공개 API입니다.";
    }
}