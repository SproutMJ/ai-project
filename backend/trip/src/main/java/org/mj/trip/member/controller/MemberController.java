package org.mj.trip.member.controller;

import org.mj.trip.common.dto.ApiResponse;
import org.mj.trip.member.dto.MemberProfileResponse;
import org.mj.trip.member.dto.SignupRequest;
import org.mj.trip.member.dto.SignupResponse;
import org.mj.trip.member.dto.UpdateProfileRequest;
import org.mj.trip.member.dto.WithdrawRequest;
import org.mj.trip.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
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

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> updateMyProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // TODO: 실제 구현 시 JWT 토큰에서 memberId 추출
        Long memberId = 1L;
        MemberProfileResponse profile = memberService.updateProfile(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) WithdrawRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // TODO: 실제 구현 시 JWT 토큰에서 memberId 추출
        Long memberId = 1L;
        memberService.withdraw(memberId, request);
        return ResponseEntity.noContent().build();
    }
}
