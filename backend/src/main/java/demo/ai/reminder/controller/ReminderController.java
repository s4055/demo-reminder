package demo.ai.reminder.controller;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.dto.request.ReminderMoveRequest;
import demo.ai.reminder.dto.request.ReminderOrderRequest;
import demo.ai.reminder.dto.request.ReminderRequest;
import demo.ai.reminder.dto.request.ReminderUpdateRequest;
import demo.ai.reminder.dto.response.DeletedCountResponse;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.service.ReminderQueryService;
import demo.ai.reminder.service.ReminderService;
import demo.ai.reminder.service.SmartView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    static final int SEARCH_QUERY_MAX_LENGTH = 100;

    private final ReminderService reminderService;
    private final ReminderQueryService reminderQueryService;

    @GetMapping
    public ApiResponse<List<ReminderResponse>> getReminders(
            @RequestParam(required = false) Long listId,
            @RequestParam(required = false) String tag) {
        return ApiResponse.success(reminderQueryService.getReminders(listId, tag));
    }

    // 뷰 이름은 대소문자를 구분하지 않으며, 지원하지 않는 이름이면 400이다.
    @GetMapping("/smart/{view}")
    public ApiResponse<List<ReminderResponse>> getSmartReminders(@PathVariable String view) {
        return ApiResponse.success(reminderQueryService.getSmartReminders(SmartView.from(view)));
    }

    // 검색어가 비어 있거나 너무 길면 400이다.
    @GetMapping("/search")
    public ApiResponse<List<ReminderResponse>> searchReminders(
            @RequestParam @NotBlank @Size(max = SEARCH_QUERY_MAX_LENGTH) String q) {
        return ApiResponse.success(reminderQueryService.searchReminders(q));
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<ReminderResponse>> getUpcomingReminders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.success(reminderQueryService.getUpcomingReminders(from, to));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReminderResponse> createReminder(@Valid @RequestBody ReminderRequest request) {
        return ApiResponse.success(reminderService.createReminder(request.toCommand()));
    }

    // "/{id}"보다 구체적인 경로라 이 매핑이 우선한다.
    @DeleteMapping("/completed")
    public ApiResponse<DeletedCountResponse> deleteCompletedReminders(@RequestParam Long listId) {
        return ApiResponse.success(reminderService.deleteCompletedReminders(listId));
    }

    @PatchMapping("/order")
    public ApiResponse<Void> reorderReminders(@Valid @RequestBody ReminderOrderRequest request) {
        reminderService.reorderReminders(request.listId(), request.ids());
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<ReminderResponse> updateReminder(@PathVariable Long id, @Valid @RequestBody ReminderUpdateRequest request) {
        return ApiResponse.success(reminderService.updateReminder(id, request.toCommand()));
    }

    @PatchMapping("/{id}/list")
    public ApiResponse<ReminderResponse> moveReminder(@PathVariable Long id, @Valid @RequestBody ReminderMoveRequest request) {
        return ApiResponse.success(reminderService.moveReminder(id, request.listId()));
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<ReminderResponse> toggleComplete(@PathVariable Long id) {
        return ApiResponse.success(reminderService.toggleComplete(id));
    }

    @PatchMapping("/{id}/flag")
    public ApiResponse<ReminderResponse> toggleFlag(@PathVariable Long id) {
        return ApiResponse.success(reminderService.toggleFlag(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReminder(@PathVariable Long id) {
        reminderService.deleteReminder(id);
        return ApiResponse.success();
    }
}
