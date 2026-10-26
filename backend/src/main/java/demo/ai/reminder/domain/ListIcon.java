package demo.ai.reminder.domain;

import java.util.Set;

public final class ListIcon {

    public static final Set<String> KEYS = Set.of(
            "list", "shopping-cart", "briefcase", "house", "heart", "star",
            "book-open", "gift", "plane", "dumbbell", "music", "car",
            "graduation-cap", "utensils", "baby", "pill", "wallet", "calendar",
            "flag", "bookmark", "paw-print", "leaf", "code", "gamepad-2"
    );

    private ListIcon() {
    }

    public static boolean isAllowed(String icon) {
        return icon == null || KEYS.contains(icon);
    }
}
