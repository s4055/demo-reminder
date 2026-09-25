package demo.ai.reminder.controller;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.dto.ReminderListRequest;
import demo.ai.reminder.dto.ReminderListResponse;
import demo.ai.reminder.service.ReminderListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
public class ReminderListController {

    private final ReminderListService reminderListService;

    @GetMapping
    public ApiResponse<List<ReminderListResponse>> getLists() {
        return ApiResponse.success(reminderListService.getLists().stream()
                .map(ReminderListResponse::from)
                .toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReminderListResponse> createList(@Valid @RequestBody ReminderListRequest request) {
        return ApiResponse.success(ReminderListResponse.from(reminderListService.createList(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReminderListResponse> updateList(@PathVariable Long id, @Valid @RequestBody ReminderListRequest request) {
        return ApiResponse.success(ReminderListResponse.from(reminderListService.updateList(id, request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteList(@PathVariable Long id) {
        reminderListService.deleteList(id);
        return ApiResponse.success();
    }
}
