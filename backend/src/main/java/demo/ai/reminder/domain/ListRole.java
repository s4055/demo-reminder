package demo.ai.reminder.domain;

// 리스트 멤버의 역할. 소유자만 리스트 수정/삭제와 멤버 관리를 할 수 있고, 편집자는 리마인더만 다룬다.
public enum ListRole {
    OWNER,
    EDITOR
}
