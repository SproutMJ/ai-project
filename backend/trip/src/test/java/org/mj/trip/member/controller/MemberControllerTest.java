package org.mj.trip.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.auth.token.JwtAuthenticationInterceptor;
import org.mj.trip.common.config.WebConfig;
import org.mj.trip.common.exception.GlobalExceptionHandler;
import org.mj.trip.member.dto.MemberProfileResponse;
import org.mj.trip.member.dto.SignupRequest;
import org.mj.trip.member.dto.SignupResponse;
import org.mj.trip.member.dto.UpdateProfileRequest;
import org.mj.trip.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Member Controller 테스트")
@WebMvcTest(MemberController.class)
@Import({WebConfig.class, GlobalExceptionHandler.class})
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private MemberService memberService;
    @MockBean
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        // 인터셉터가 요청 객체에 memberId를 설정하도록 stub
        lenient().when(jwtAuthenticationInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenAnswer(invocation -> {
                    HttpServletRequest request = invocation.getArgument(0);
                    request.setAttribute("memberId", 1L);
                    return true;
                });
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        // given
        SignupRequest request = SignupRequest.builder()
                .email("test@test.com")
                .password("12345678")
                .nickname("tester")
                .build();
        SignupResponse response = SignupResponse.builder()
                .memberId(1L)
                .email("test@test.com")
                .nickname("tester")
                .build();
        when(memberService.signup(any(SignupRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nickname").value("tester"));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효성 검사 오류")
    void signup_validation_fail() throws Exception {
        // given
        SignupRequest request = SignupRequest.builder()
                .email("invalid")
                .password("123")
                .nickname("")
                .build();

        // when & then
        mockMvc.perform(post("/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("내 프로필 조회 성공")
    void getMyProfile_success() throws Exception {
        // given
        Long memberId = 1L;
        MemberProfileResponse response = MemberProfileResponse.builder()
                .memberId(memberId)
                .nickname("tester")
                .travelStyles(List.of())
                .build();
        when(memberService.getMyProfile(memberId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("tester"));
    }

    @Test
    @DisplayName("프로필 수정 성공")
    void updateProfile_success() throws Exception {
        // given
        Long memberId = 1L;
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .nickname("updated")
                .build();
        MemberProfileResponse response = MemberProfileResponse.builder()
                .memberId(memberId)
                .nickname("updated")
                .build();
        when(memberService.updateProfile(any(Long.class), any(UpdateProfileRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(patch("/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("updated"));
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_success() throws Exception {
        // given
        Long memberId = 1L;

        // when & then
        mockMvc.perform(delete("/v1/members/me"))
                .andExpect(status().isNoContent());
    }
}
