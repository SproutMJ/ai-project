package org.mj.trip.member.controller;

import org.junit.jupiter.api.Test;
import org.mj.trip.member.dto.MemberProfileResponse;
import org.mj.trip.member.dto.SignupRequest;
import org.mj.trip.member.dto.SignupResponse;
import org.mj.trip.member.dto.UpdateProfileRequest;
import org.mj.trip.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import({org.mj.trip.common.config.WebConfig.class, org.mj.trip.common.exception.GlobalExceptionHandler.class})
class MemberControllerTest {

    @MockitoBean
    private MemberService memberService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 내프로필조회_성공() throws Exception {
        MemberProfileResponse.TravelStyle style = new MemberProfileResponse.TravelStyle(1L, "맛집 중심");
        MemberProfileResponse response = new MemberProfileResponse(
                1L, "user@example.com", "minjun", "https://example.com/profile.jpg", List.of(style),
                "2026-04-21T10:00:00Z", "2026-04-21T10:00:00Z"
        );
        given(memberService.getMyProfile(1L)).willReturn(response);

        mockMvc.perform(get("/v1/members/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.nickname").value("minjun"))
                .andExpect(jsonPath("$.data.travelStyles[0].name").value("맛집 중심"));
    }

    @Test
    void 내프로필조회_인증정보없음() throws Exception {
        mockMvc.perform(get("/v1/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 내프로필수정_성공() throws Exception {
        MemberProfileResponse.TravelStyle style1 = new MemberProfileResponse.TravelStyle(2L, "힐링");
        MemberProfileResponse.TravelStyle style2 = new MemberProfileResponse.TravelStyle(4L, "액티비티");
        
        MemberProfileResponse response = new MemberProfileResponse(
                1L, null, "newNickname", "https://example.com/new-profile.jpg",
                List.of(style1, style2), null, "2026-04-21T10:00:00Z"
        );
        
        given(memberService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).willReturn(response);

        String requestBody = """
                {
                    "nickname": "newNickname",
                    "profileImageUrl": "https://example.com/new-profile.jpg",
                    "travelStyleIds": [2, 4]
                }
                """;

        mockMvc.perform(patch("/v1/members/me")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.nickname").value("newNickname"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/new-profile.jpg"))
                .andExpect(jsonPath("$.data.travelStyles[0].id").value(2))
                .andExpect(jsonPath("$.data.travelStyles[0].name").value("힐링"))
                .andExpect(jsonPath("$.data.travelStyles[1].id").value(4))
                .andExpect(jsonPath("$.data.travelStyles[1].name").value("액티비티"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-04-21T10:00:00Z"));
    }

    @Test
    void 내프로필수정_인증정보없음() throws Exception {
        String requestBody = """
                {
                    "nickname": "newNickname"
                }
                """;

        mockMvc.perform(patch("/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 내프로필수정_유효성검증실패() throws Exception {
        String requestBody = """
                {
                    "nickname": "a"
                }
                """;

        mockMvc.perform(patch("/v1/members/me")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("nickname"));
    }
}