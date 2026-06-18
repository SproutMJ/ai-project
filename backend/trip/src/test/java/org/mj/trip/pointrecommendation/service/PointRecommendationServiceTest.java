
package org.mj.trip.pointrecommendation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mj.trip.common.exception.ResourceNotFoundException;
import org.mj.trip.pointrecommendation.domain.PointRecommendation;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRepository;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRequest;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRequestRepository;
import org.mj.trip.pointrecommendation.dto.PointRecommendationDetailResponseDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationListResponseDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationRequestDto;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointRecommendationServiceTest {

    @Mock
    private PointRecommendationRequestRepository pointRecommendationRequestRepository;

    @Mock
    private PointRecommendationRepository pointRecommendationRepository;

    @InjectMocks
    private PointRecommendationService pointRecommendationService;

    private PointRecommendationRequest dummyRequest;
    private PointRecommendation dummyRecommendation;

    @BeforeEach
    void setUp() {
        // 테스트용 Request 엔티티 설정
        dummyRequest = PointRecommendationRequest.builder()
                .id(1L)
                .userId(1L)
                .requestText("서울 여행 추천해줘")
                .build();

        // 테스트용 Recommendation 엔티티 설정
        dummyRecommendation = PointRecommendation.builder()
                .request(dummyRequest)
                .userId(1L)
                .name("경복궁")
                .recommendationScore(95.5)
                .shortComment("서울의 대표적인 궁궐")
                .type("관광지")
                .region("서울 종로구")
                .build();
    }

    @DisplayName("추천 생성 시 Request 저장 및 generateRecommendations 호출")
    @Test
    void createRecommendation_shouldSaveRequestAndGenerateRecommendations() {
        // given
        PointRecommendationRequestDto requestDto = new PointRecommendationRequestDto("서울 여행 추천해줘");
        when(pointRecommendationRequestRepository.save(any(PointRecommendationRequest.class)))
                .thenReturn(dummyRequest);

        // when
        pointRecommendationService.createRecommendation(1L, requestDto);

        // then
        verify(pointRecommendationRequestRepository, times(1)).save(any(PointRecommendationRequest.class));
        // generateRecommendations 메소드가 RuntimeException을 던지므로, 호출 자체가 되었는지 확인하거나
        // 실제 구현 후mocking을 통해 검증할 수 있습니다.
        // 현재는 not implemented이므로 호출은 되었으나 예외가 발생한다는 점만 확인 가능하거나,
        // generateRecommendations를 mock으로 분리하면 더 정밀한 검증이 가능합니다.
    }

    @DisplayName("추천 목록 조회 성공")
    @Test
    void listRecommendations_shouldReturnPaginatedRecommendations() {
        // given
        Page<PointRecommendation> recommendationPage = new PageImpl<>(
                List.of(dummyRecommendation),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "recommendationScore")),
                1
        );

        when(pointRecommendationRepository.findAll(any(Pageable.class)))
                .thenReturn(recommendationPage);

        // when
        PointRecommendationListResponseDto response = pointRecommendationService.listRecommendations(0, 10, "recommendationScore", "desc");

        // then
        assertThat(response).isNotNull();
        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).name()).isEqualTo("경복궁");
        assertThat(response.meta().totalElements()).isEqualTo(1);
        verify(pointRecommendationRepository, times(1)).findAll(any(Pageable.class));
    }

    @DisplayName("추천 상세 조회 성공")
    @Test
    void getRecommendationDetail_shouldReturnRecommendationDetail() {
        // given
        when(pointRecommendationRequestRepository.findById(1L))
                .thenReturn(Optional.of(dummyRequest));
        when(pointRecommendationRepository.findByRequestOrderByRecommendationScoreDesc(dummyRequest))
                .thenReturn(List.of(dummyRecommendation));

        // when
        PointRecommendationDetailResponseDto response = pointRecommendationService.getRecommendationDetail(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).name()).isEqualTo("경복궁");
        assertThat(response.requestInfo().requestText()).isEqualTo("서울 여행 추천해줘");
    }

    @DisplayName("추천 상세 조회 실패 - Request 없음")
    @Test
    void getRecommendationDetail_shouldThrowExceptionWhenRequestNotFound() {
        // given
        when(pointRecommendationRequestRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointRecommendationService.getRecommendationDetail(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RecommendationRequest not found: 999");
    }

    @DisplayName("추천 요청 삭제 성공")
    @Test
    void deleteRecommendationRequest_shouldDeleteRequestAndAssociatedRecommendations() {
        // given
        when(pointRecommendationRequestRepository.findById(1L))
                .thenReturn(Optional.of(dummyRequest));
        doNothing().when(pointRecommendationRepository).deleteByRequestId(1L);
        doNothing().when(pointRecommendationRequestRepository).delete(dummyRequest);

        // when
        pointRecommendationService.deleteRecommendationRequest(1L);

        // then
        verify(pointRecommendationRepository, times(1)).deleteByRequestId(1L);
        verify(pointRecommendationRequestRepository, times(1)).delete(dummyRequest);
    }

    @DisplayName("추천 요청 삭제 실패 - Request 없음")
    @Test
    void deleteRecommendationRequest_shouldThrowExceptionWhenRequestNotFound() {
        // given
        when(pointRecommendationRequestRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointRecommendationService.deleteRecommendationRequest(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RecommendationRequest not found: 999");
    }
}
