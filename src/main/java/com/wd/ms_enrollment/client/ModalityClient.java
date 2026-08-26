package com.wd.ms_enrollment.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.world_dance.wd_lib_common.dto.HttpGlobalResponse;
import com.world_dance.wd_lib_common.dto.ModalityResponseDto;
import com.world_dance.wd_lib_common.enums.Category;

// ModalityClient.java
@FeignClient(name = "ms-event-category", path = "/api/v1/modality")
public interface ModalityClient {
    @GetMapping("/getModalitiesByCategory/{category}")
    HttpGlobalResponse<List<ModalityResponseDto>> getModalitiesByCategory(@PathVariable("category") Category category);
}