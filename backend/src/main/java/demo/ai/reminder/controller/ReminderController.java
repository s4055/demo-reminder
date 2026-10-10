package demo.ai.reminder.controller;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.dto.DeletedCountResponse;
import demo.ai.reminder.dto.ReminderMoveRequest;
import demo.ai.reminder.dto.ReminderOrderRequest;
import demo.ai.reminder.dto.ReminderRequest;
import demo.ai.reminder.dto.ReminderResponse;
import demo.ai.reminder.dto.ReminderUpdateRequest;
import demo.ai.reminder.service.ReminderService;
import jakarta.validation.Valid;
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

    private final ReminderService reminderService;

    @GetMapping
    public ApiResponse<List<ReminderResponse>> getReminders(
            @RequestParam(required = false) Long listId,
            @RequestParam(required = false) String tag) {
        return ApiResponse.success(reminderService.getReminders(listId, tag));
    }

    @GetMapping("/smart/{view}")
    public ApiResponse<List<ReminderResponse>> getSmartReminders(@PathVariable String view) {
        return ApiResponse.success(reminderService.getSmartReminders(view));
    }

    @GetMapping("/search")
    public ApiResponse<List<ReminderResponse>> searchReminders(@RequestParam String q) {
        return ApiResponse.success(reminderService.searchReminders(q));
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<ReminderResponse>> getUpcomingReminders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.success(reminderService.getUpcomingReminders(from, to));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReminderResponse> createReminder(@Valid @RequestBody ReminderRequest request) {
        return ApiResponse.success(reminderService.createReminder(request));
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
        return ApiResponse.success(reminderService.updateReminder(id, request));
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
