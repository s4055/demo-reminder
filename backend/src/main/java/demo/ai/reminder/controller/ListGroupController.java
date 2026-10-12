package demo.ai.reminder.controller;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.dto.request.ListGroupRequest;
import demo.ai.reminder.dto.request.ListOrderRequest;
import demo.ai.reminder.dto.response.ListGroupResponse;
import demo.ai.reminder.service.ListGroupService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/list-groups")
@RequiredArgsConstructor
public class ListGroupController {

    private final ListGroupService listGroupService;

    @GetMapping
    public ApiResponse<List<ListGroupResponse>> getGroups() {
        return ApiResponse.success(listGroupService.getGroups());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ListGroupResponse> createGroup(@Valid @RequestBody ListGroupRequest request) {
        return ApiResponse.success(listGroupService.createGroup(request));
    }

    // 요청 형식은 리스트 순서 변경과 같다 (ids 순서가 곧 새 표시 순서).
    @PatchMapping("/order")
    public ApiResponse<Void> reorderGroups(@Valid @RequestBody ListOrderRequest request) {
        listGroupService.reorderGroups(request.ids());
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<ListGroupResponse> renameGroup(@PathVariable Long id, @Valid @RequestBody ListGroupRequest request) {
        return ApiResponse.success(listGroupService.renameGroup(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        listGroupService.deleteGroup(id);
        return ApiResponse.success();
    }
}
