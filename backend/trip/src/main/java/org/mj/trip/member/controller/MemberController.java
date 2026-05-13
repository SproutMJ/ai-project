package org.mj.trip.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.mj.trip.auth.token.JwtAuthenticationInterceptor;
import org.mj.trip.common.dto.ApiResponse;
import org.mj.trip.member.dto.MemberProfileResponse;
import org.mj.trip.member.dto.SignupRequest;
import org.mj.trip.member.dto.SignupResponse;
import org.mj.trip.member.dto.UpdateProfileRequest;
import org.mj.trip.member.dto.WithdrawRequest;
import org.mj.trip.member.service.MemberService;
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
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(HttpServletRequest request) {
        Long memberId = JwtAuthenticationInterceptor.getMemberIdFromRequest(request);
        MemberProfileResponse profile = memberService.getMyProfile(memberId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> updateMyProfile(
            HttpServletRequest request,
            @Valid @RequestBody UpdateProfileRequest updateRequest) {
        Long memberId = JwtAuthenticationInterceptor.getMemberIdFromRequest(request);
        MemberProfileResponse profile = memberService.updateProfile(memberId, updateRequest);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            HttpServletRequest request,
            @RequestBody(required = false) WithdrawRequest withdrawRequest) {
        Long memberId = JwtAuthenticationInterceptor.getMemberIdFromRequest(request);
        memberService.withdraw(memberId, withdrawRequest);
        return ResponseEntity.noContent().build();
    }
}
