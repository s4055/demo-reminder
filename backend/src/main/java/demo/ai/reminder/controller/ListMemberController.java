package demo.ai.reminder.controller;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.dto.request.ListMemberRequest;
import demo.ai.reminder.dto.response.ListMemberResponse;
import demo.ai.reminder.service.ListMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lists/{listId}/members")
@RequiredArgsConstructor
public class ListMemberController {

    private final ListMemberService listMemberService;

    @GetMapping
    public ApiResponse<List<ListMemberResponse>> getMembers(@PathVariable Long listId) {
        return ApiResponse.success(listMemberService.getMembers(listId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ListMemberResponse> invite(@PathVariable Long listId,
                                                  @Valid @RequestBody ListMemberRequest request) {
        return ApiResponse.success(listMemberService.invite(listId, request.email()));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long listId, @PathVariable Long userId) {
        listMemberService.removeMember(listId, userId);
        return ApiResponse.success();
    }
}
