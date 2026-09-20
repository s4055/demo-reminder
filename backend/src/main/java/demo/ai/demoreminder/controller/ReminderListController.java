package demo.ai.demoreminder.controller;

import demo.ai.demoreminder.dto.ReminderListRequest;
import demo.ai.demoreminder.dto.ReminderListResponse;
import demo.ai.demoreminder.service.ReminderListService;
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
    public List<ReminderListResponse> getLists() {
        return reminderListService.getLists().stream()
                .map(ReminderListResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderListResponse createList(@Valid @RequestBody ReminderListRequest request) {
        return ReminderListResponse.from(reminderListService.createList(request));
    }

    @PutMapping("/{id}")
    public ReminderListResponse updateList(@PathVariable Long id, @Valid @RequestBody ReminderListRequest request) {
        return ReminderListResponse.from(reminderListService.updateList(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteList(@PathVariable Long id) {
        reminderListService.deleteList(id);
    }
}
