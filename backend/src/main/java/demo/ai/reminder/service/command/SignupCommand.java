package demo.ai.reminder.service.command;

// 회원가입 입력. password는 평문이며 서비스가 해시로 바꿔 저장한다.
public record SignupCommand(String email, String password, String name) {
}
