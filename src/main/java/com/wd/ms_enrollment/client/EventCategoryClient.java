package com.wd.ms_enrollment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.world_dance.wd_lib_common.dto.EventResponseDto;
import com.world_dance.wd_lib_common.dto.HttpGlobalResponse;

@FeignClient(name = "ms-event-category", path = "/api/v1/events")
public interface EventCategoryClient {

    @GetMapping("/{eventId}")
    HttpGlobalResponse<EventResponseDto> getEventById(@PathVariable("eventId") Long eventId);
}