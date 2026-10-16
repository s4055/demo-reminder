package demo.ai.reminder.service.command;

// 리스트 수정 입력. version은 편집을 시작할 때 받은 리스트의 버전이다.
public record UpdateListCommand(String name, String color, String icon, Long version) {
}
