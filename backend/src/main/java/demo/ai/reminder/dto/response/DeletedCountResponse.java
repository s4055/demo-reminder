package demo.ai.reminder.dto.response;

public record DeletedCountResponse(int deletedCount) {

    public static DeletedCountResponse from(int deletedCount) {
        return new DeletedCountResponse(deletedCount);
    }
}
