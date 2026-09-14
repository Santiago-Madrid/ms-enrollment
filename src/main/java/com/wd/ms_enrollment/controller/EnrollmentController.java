package com.wd.ms_enrollment.controller;

import com.wd.ms_enrollment.service.EnrollmentService;
import com.world_dance.wd_lib_common.dto.ApproveEnrollmentRequestDto;
import com.world_dance.wd_lib_common.dto.EnrollmentRequestDto;
import com.world_dance.wd_lib_common.dto.EnrollmentResponseDto;
import com.world_dance.wd_lib_common.dto.UserEventRoleResponseDto;
import com.world_dance.wd_lib_common.enums.Category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/enrollment")
    public ResponseEntity<EnrollmentResponseDto> registerUserToEvent(@Valid @RequestBody EnrollmentRequestDto request) {
        EnrollmentResponseDto response = enrollmentService.registerUserToEvent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/approve")
    public ResponseEntity<EnrollmentResponseDto> approveOrRejectEnrollment(
            @Valid @RequestBody ApproveEnrollmentRequestDto request,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {

        EnrollmentResponseDto response = enrollmentService.approveOrRejectEnrollment(request, authenticatedUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<EnrollmentResponseDto>> getEnrollmentsByCategory(
            @PathVariable Category category,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {

        List<EnrollmentResponseDto> response = enrollmentService.getEnrollmentsByCategory(category,
                authenticatedUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<EnrollmentResponseDto>> getMyEnrollments(
            @RequestHeader("X-User-Id") Long userId) {

        List<EnrollmentResponseDto> response = enrollmentService.getMyEnrollments(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<EnrollmentResponseDto>> getEnrollmentsByEvent(
            @PathVariable Long eventId) {

        List<EnrollmentResponseDto> response = enrollmentService.getEnrollmentsByEvent(eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/events/{eventId}/users/{userId}/role")
    public ResponseEntity<UserEventRoleResponseDto> getUserEventRole(
            @PathVariable Long eventId,
            @PathVariable Long userId) {

        UserEventRoleResponseDto response = enrollmentService.getUserEventRole(userId, eventId);
        return ResponseEntity.ok(response);
    }
}