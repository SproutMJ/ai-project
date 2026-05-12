package org.mj.trip.member.controller;

import org.junit.jupiter.api.Test;
import org.mj.trip.member.dto.MemberProfileResponse;
import org.mj.trip.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}