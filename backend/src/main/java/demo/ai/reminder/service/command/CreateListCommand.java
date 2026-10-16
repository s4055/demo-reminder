package demo.ai.reminder.service.command;

// 리스트 생성 입력. icon이 null이면 기본 아이콘이다.
public record CreateListCommand(String name, String color, String icon) {
}
