package com.wd.ms_enrollment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.world_dance.wd_lib_common.dto.UserResponseDto;

@FeignClient(name = "ms-auth-identityservice", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserResponseDto getUserById(@PathVariable("userId") Long userId);
}
