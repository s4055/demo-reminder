package demo.ai.reminder.dto;

// 일괄 삭제로 지워진 리마인더 수. 함께 삭제된 하위 작업도 센다.
public record DeletedCountResponse(int deletedCount) {
}
