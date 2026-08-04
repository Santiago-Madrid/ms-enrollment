package com.wd.ms_enrollment.controller;

import com.wd.ms_enrollment.dto.RejectionRequest;
import com.wd.ms_enrollment.service.EnrollmentService;
import com.world_dance.wd_lib_common.dto.EnrollmentRequestDto;
import com.world_dance.wd_lib_common.dto.EnrollmentResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/inscribirse")
    public ResponseEntity<EnrollmentResponseDto> registerUserToEvent(@Valid @RequestBody EnrollmentRequestDto request) {
        EnrollmentResponseDto response = enrollmentService.registerUserToEvent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }













/* 
    // RF-25 y RF-26: Inscribirse en categoría
    @PostMapping("/event/{eventId}/category/{categoryId}")
    //  @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<EnrollmentResponseDto> enrollInEvent(
            @PathVariable Long eventId,
            @PathVariable Long categoryId,
            @AuthenticationPrincipal Object principal
    ) {
        Long userId = 1L; 
        EnrollmentResponseDto response = enrollmentService.createEnrollment(userId, eventId, categoryId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // RF-27: Aprobar inscripción
    @PatchMapping("/{enrollmentId}/approve")
    // @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EnrollmentResponseDto> approveEnrollment(@PathVariable Long enrollmentId) {
        EnrollmentResponseDto approved = enrollmentService.approveEnrollment(enrollmentId);
        return ResponseEntity.ok(approved);
    }

    // RF-28: Rechazar inscripción con justificación
    @PatchMapping("/{enrollmentId}/reject")
    // @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EnrollmentResponseDto> rejectEnrollment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody RejectionRequest rejectionRequest
    ) {
        EnrollmentResponseDto rejected = enrollmentService.rejectEnrollment(enrollmentId, rejectionRequest);
        return ResponseEntity.ok(rejected);
    }

    // RF-29: Consultar inscritos por categoría (Rol ORGANIZADOR / ADMIN)
    @GetMapping("/category/{categoryId}")
    // @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN', 'STAFF')")
    public ResponseEntity<List<EnrollmentResponseDto>> getEnrollmentsByCategory(@PathVariable Long categoryId) {
        List<EnrollmentResponseDto> list = enrollmentService.getEnrollmentsByCategory(categoryId);
        return ResponseEntity.ok(list);
    }

    // RF-30: Consultar mis inscripciones y sus estados (Rol PARTICIPANTE)
    @GetMapping("/my-enrollments")
    // @PreAuthorize("hasRole('PARTICIPANT')") // Ya lo tienes comentado, ¡perfecto!
    public ResponseEntity<List<EnrollmentResponseDto>> getMyEnrollments(@AuthenticationPrincipal Object principal) {
        // Long userId = extractUserIdFromPrincipal(principal); // Comentamos temporalmente
        Long userId = 1L; // Quemamos el ID del usuario para la prueba local
        
        List<EnrollmentResponseDto> list = enrollmentService.getMyEnrollments(userId);
        return ResponseEntity.ok(list);
    }

    // private Long extractUserIdFromPrincipal(Object principal) {
    //     return Long.valueOf(principal.toString());
    // }
 */
}