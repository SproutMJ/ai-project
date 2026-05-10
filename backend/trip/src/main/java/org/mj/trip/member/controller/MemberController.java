package org.mj.trip.member.controller;

import org.mj.trip.common.dto.ApiResponse;
import org.mj.trip.member.dto.MemberProfileResponse;
import org.mj.trip.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // TODO: 실제 구현 시 JWT 토큰에서 memberId 추출
        Long memberId = 1L;
        MemberProfileResponse profile = memberService.getMyProfile(memberId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}