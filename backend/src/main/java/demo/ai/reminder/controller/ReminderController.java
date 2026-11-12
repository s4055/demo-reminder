package demo.ai.reminder.controller;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.dto.request.ReminderMoveRequest;
import demo.ai.reminder.dto.request.ReminderOrderRequest;
import demo.ai.reminder.dto.request.ReminderRequest;
import demo.ai.reminder.dto.request.ReminderUpdateRequest;
import demo.ai.reminder.dto.response.DeletedCountResponse;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.dto.response.SliceResponse;
import demo.ai.reminder.service.CompletedCursor;
import demo.ai.reminder.service.OffsetCursor;
import demo.ai.reminder.service.ReminderService;
import demo.ai.reminder.service.SmartView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    static final int SEARCH_QUERY_MAX_LENGTH = 100;
    static final String DEFAULT_PAGE_SIZE = "50";
    static final int MAX_PAGE_SIZE = 100;

    private final ReminderService reminderService;

    @GetMapping
    public ApiResponse<List<ReminderResponse>> getReminders(
            @RequestParam(required = false) Long listId,
            @RequestParam(required = false) String tag) {
        return ApiResponse.success(reminderService.getReminders(listId, tag == null ? null : tag.strip()));
    }

    @GetMapping("/smart/{view}")
    public ApiResponse<List<ReminderResponse>> getSmartReminders(
            @PathVariable SmartView view,
            @RequestParam(required = false) ZoneId zone) {
        return ApiResponse.success(reminderService.getSmartReminders(view, zone));
    }

    @GetMapping("/completed")
    public ApiResponse<SliceResponse<ReminderResponse>> getCompletedReminders(
            @RequestParam(required = false) CompletedCursor cursor,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return ApiResponse.success(reminderService.getCompletedReminders(cursor, size));
    }

    @GetMapping("/search")
    public ApiResponse<SliceResponse<ReminderResponse>> searchReminders(
            @RequestParam @NotBlank @Size(max = SEARCH_QUERY_MAX_LENGTH) String q,
            @RequestParam(required = false) OffsetCursor cursor,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return ApiResponse.success(reminderService.searchReminders(q.strip(), cursor, size));
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<ReminderResponse>> getUpcomingReminders(
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return ApiResponse.success(reminderService.getUpcomingReminders(from, to));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReminderResponse> createReminder(@Valid @RequestBody ReminderRequest request) {
        return ApiResponse.success(reminderService.createReminder(request));
    }

    @DeleteMapping("/completed")
    public ApiResponse<DeletedCountResponse> deleteCompletedReminders(@RequestParam Long listId) {
        return ApiResponse.success(reminderService.deleteCompletedReminders(listId));
    }

    @PatchMapping("/order")
    public ApiResponse<Void> reorderReminders(@Valid @RequestBody ReminderOrderRequest request) {
        reminderService.reorderReminders(request);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<ReminderResponse> getReminder(@PathVariable Long id) {
        return ApiResponse.success(reminderService.getReminder(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateReminder(@PathVariable Long id, @Valid @RequestBody ReminderUpdateRequest request) {
        reminderService.updateReminder(id, request);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/list")
    public ApiResponse<Void> moveReminder(@PathVariable Long id, @Valid @RequestBody ReminderMoveRequest request) {
        reminderService.moveReminder(id, request.listId());
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<Void> toggleComplete(@PathVariable Long id) {
        reminderService.toggleComplete(id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/flag")
    public ApiResponse<Void> toggleFlag(@PathVariable Long id) {
        reminderService.toggleFlag(id);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReminder(@PathVariable Long id) {
        reminderService.deleteReminder(id);
        return ApiResponse.success();
    }
}
