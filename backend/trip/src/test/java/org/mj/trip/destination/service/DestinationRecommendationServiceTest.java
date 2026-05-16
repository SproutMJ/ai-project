package org.mj.trip.destination.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mj.trip.destination.domain.ReasonType;
import org.mj.trip.destination.domain.RecommendationReason;
import org.mj.trip.destination.domain.RecommendationRequest;
import org.mj.trip.destination.dto.DestinationRecommendationRequest;
import org.mj.trip.destination.dto.DestinationRecommendationResponse;
import org.mj.trip.destination.repository.RecommendationReasonRepository;
import org.mj.trip.destination.repository.RecommendationRepository;
import org.mj.trip.destination.repository.RecommendationRequestRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.Mockito;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("DestinationRecommendationService 테스트")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DestinationRecommendationServiceTest {

    @Mock
    private RecommendationRequestRepository recommendationRequestRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationReasonRepository recommendationReasonRepository;

    @InjectMocks
    private DestinationRecommendationService destinationRecommendationService;

    @BeforeEach
    void setUp() {
        // mock을 새로 생성하여 stubbing이 초기화되도록 함
        recommendationRequestRepository = mock(RecommendationRequestRepository.class);
        recommendationRepository = mock(RecommendationRepository.class);
        recommendationReasonRepository = mock(RecommendationReasonRepository.class);
        destinationRecommendationService = new DestinationRecommendationService(
                recommendationRequestRepository,
                recommendationRepository,
                recommendationReasonRepository
        );
    }

    // ==================== 성공 케이스 (Success Cases) ====================

    @Test
    @DisplayName("성공: 여행지 추천 생성 - 기본 케이스")
    void createRecommendation_success_basic() {
        // given
        Long memberId = 1L;
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(memberId)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(memberId, request);

        // then
        assertNotNull(response);
        assertEquals(savedRequest.getRecommendationRequestId(), response.getRecommendationRequestId());
        assertEquals(5, response.getRecommendations().size());
        assertNotNull(response.getCreatedAt());
        verify(recommendationRequestRepository).save(any(RecommendationRequest.class));
    }

    @Test
    @DisplayName("성공: 여행지 추천 생성 - 일본 지역")
    void createRecommendation_success_japan() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("쇼핑")
                .travelStyleIds(List.of(1L))
                .budgetRange("고예산")
                .region("일본")
                .season("봄")
                .companionCount(3)
                .durationDays(7)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("쇼핑")
                .budgetRange("고예산")
                .region("일본")
                .season("봄")
                .companionCount(3)
                .durationDays(7)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        // then
        assertNotNull(response);
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getDestinationName().equals("도쿄") || r.getDestinationName().equals("오사카")));
    }

    @Test
    @DisplayName("성공: 여행지 추천 생성 - 태국 지역")
    void createRecommendation_success_thailand() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("액티비티")
                .travelStyleIds(List.of(2L))
                .budgetRange("중예산")
                .region("태국")
                .season("겨울")
                .companionCount(4)
                .durationDays(10)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("액티비티")
                .budgetRange("중예산")
                .region("태국")
                .season("겨울")
                .companionCount(4)
                .durationDays(10)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        // then
        assertNotNull(response);
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getDestinationName().equals("방콕") || r.getDestinationName().equals("푸켓")));
    }

    @Test
    @DisplayName("성공: 여행지 추천 생성 - 베트남 지역")
    void createRecommendation_success_vietnam() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("문화체험")
                .travelStyleIds(List.of(3L))
                .budgetRange("저예산")
                .region("베트남")
                .season("가을")
                .companionCount(1)
                .durationDays(14)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("문화체험")
                .budgetRange("저예산")
                .region("베트남")
                .season("가을")
                .companionCount(1)
                .durationDays(14)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        // then
        assertNotNull(response);
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getDestinationName().equals("하노이") || r.getDestinationName().equals("호찌민")));
    }

    @Test
    @DisplayName("성공: 여행지 추천 생성 - 국내 지역")
    void createRecommendation_success_domestic() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("맛집탐방")
                .travelStyleIds(List.of(4L))
                .budgetRange("중예산")
                .region("국내")
                .season("여름")
                .companionCount(2)
                .durationDays(3)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("맛집탐방")
                .budgetRange("중예산")
                .region("국내")
                .season("여름")
                .companionCount(2)
                .durationDays(3)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        // then
        assertNotNull(response);
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getDestinationName().equals("서울") || r.getDestinationName().equals("부산")));
    }

    @Test
    @DisplayName("성공: 휴식 목적 reasonSummary 검증")
    void createRecommendation_success_verifyReasonSummaryContainsTripPurpose() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        // then
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getReasonSummary().contains("휴식") && r.getReasonSummary().contains("2명의 동반자와")));
    }

    @Test
    @DisplayName("성공: 쇼핑 목적 reasonSummary 검증")
    void createRecommendation_success_verifyReasonSummaryContainsShopping() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("쇼핑")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(3)
                .durationDays(7)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("쇼핑")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(3)
                .durationDays(7)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        // then
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getReasonSummary().contains("쇼핑을 즐기기 좋고") && r.getReasonSummary().contains("7일 일정에 적합합니다.")));
    }

    @Test
    @DisplayName("성공: 동반자 수 reasonSummary 검증")
    void createRecommendation_success_verifyReasonSummaryContainsCompanionCount() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(5)
                .durationDays(10)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(5)
                .durationDays(10)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        // then
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getReasonSummary().contains("5명의 동반자와")));
    }

    @Test
    @DisplayName("성공: 여행 기간 reasonSummary 검증")
    void createRecommendation_success_verifyReasonSummaryContainsDurationDays() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(15)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(15)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        // then
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getReasonSummary().contains("15일 일정에 적합합니다.")));
    }

    @Test
    @DisplayName("성공: 점수 범위 검증 (80.0 ~ 100.0)")
    void createRecommendation_success_verifyScoreRange() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        // then
        response.getRecommendations().forEach(rec -> {
            assertTrue(rec.getScore() >= 80.0, "점수는 80.0 이상이어야 합니다.");
            assertTrue(rec.getScore() <= 100.0, "점수는 100.0 이하여야 합니다.");
        });
    }

    @Test
    @DisplayName("성공: 랭킹 순서 검증 (1 ~ 5)")
    void createRecommendation_success_verifyRankOrder() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        // then
        List<Integer> rankOrders = response.getRecommendations().stream()
                .map(DestinationRecommendationResponse.Recommendation::getRankOrder)
                .toList();
        assertTrue(rankOrders.contains(1));
        assertTrue(rankOrders.contains(2));
        assertTrue(rankOrders.contains(3));
        assertTrue(rankOrders.contains(4));
        assertTrue(rankOrders.contains(5));
    }

    @Test
    @DisplayName("성공: Repository 저장 호출 검증 (saveAll 문제 해결)")
    void createRecommendation_success_verifyRepositoryCalls() {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(any());

        // when
        destinationRecommendationService.createRecommendation(1L, request);

        // then
        verify(recommendationRequestRepository, times(1)).save(any(RecommendationRequest.class));
        verify(recommendationReasonRepository, atLeast(1)).findByRecommendationId(any());
    }

    // ==================== 실패 케이스 (Failure Cases) ====================

    @Test
    @DisplayName("실패: RecommendationRequest 저장 시 데이터AccessException 발생")
    void createRecommendation_failure_recommendationRequestRepositoryException() {
        // given
        Long memberId = 1L;
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        // recommendationRequestRepository.save()에서 예외 발생
        when(recommendationRequestRepository.save(any(RecommendationRequest.class)))
                .thenThrow(new org.springframework.dao.DataAccessException("DB error") {
                });

        // when & then
        assertThrows(org.springframework.dao.DataAccessException.class, () -> {
            destinationRecommendationService.createRecommendation(memberId, request);
        });

        // recommendationReasonRepository는 호출되지 않아야 함
        verify(recommendationReasonRepository, never()).saveAll(any());
        verify(recommendationReasonRepository, never()).findByRecommendationId(anyLong());
    }

    @Test
    @DisplayName("실패: RecommendationReason 저장 시 DataAccessException 발생")
    void createRecommendation_failure_recommendationReasonRepositoryException() {
        // given
        Long memberId = 1L;
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(memberId)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        // saveAll() 호출 시 예외 발생
        doThrow(new org.springframework.dao.DataAccessException("DB error") {
        }).when(recommendationReasonRepository).saveAll(anyList());

        // when & then
        assertThrows(org.springframework.dao.DataAccessException.class, () -> {
            destinationRecommendationService.createRecommendation(memberId, request);
        });

        // recommendationRequestRepository는 호출됨
        verify(recommendationRequestRepository, times(1)).save(any(RecommendationRequest.class));
    }

    @Test
    @DisplayName("실패: region이 null인 경우 - NullPointerException 발생")
    void createRecommendation_failure_nullRegion() {
        // given
        Long memberId = 1L;
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region(null)  // null region
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(memberId)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("국내")  // fallback으로 국내 사용
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when & then
        // region이 null이면 서비스 코드에서 NullPointerException이 발생함
        assertThrows(NullPointerException.class, () -> {
            destinationRecommendationService.createRecommendation(memberId, request);
        });
    }

    @Test
    @DisplayName("실패: companionCount가 0인 경우 - 논리적 오류 가능성")
    void createRecommendation_failure_zeroCompanionCount() {
        // given
        Long memberId = 1L;
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(0)  // 0명 - 논리적으로 잘못된 값
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(memberId)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(0)
                .durationDays(5)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(memberId, request);

        // then
        assertNotNull(response);
        // companionCount가 0이므로 "0명의 동반자와"라는 이상한 reasonSummary가 생성됨
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getReasonSummary().contains("0명의 동반자와")));
    }

    @Test
    @DisplayName("실패: durationDays가 0인 경우 - 논리적 오류 가능성")
    void createRecommendation_failure_zeroDurationDays() {
        // given
        Long memberId = 1L;
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(0)  // 0일 - 논리적으로 잘못된 값
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(memberId)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(0)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(memberId, request);

        // then
        assertNotNull(response);
        // durationDays가 0이므로 "0일 일정에 적합합니다."라는 이상한 reasonSummary가 생성됨
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getReasonSummary().contains("0일 일정에 적합합니다.")));
    }

    @Test
    @DisplayName("실패: tripPurpose가 빈 문자열인 경우")
    void createRecommendation_failure_emptyTripPurpose() {
        // given
        Long memberId = 1L;
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("")  // 빈 문자열
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(memberId)
                .tripPurpose("")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(memberId, request);

        // then
        assertNotNull(response);
        // tripPurpose가 비어있으므로 else 블록으로 처리됨
        assertNotNull(response.getRecommendations());
        assertTrue(response.getRecommendations().size() > 0);
    }

    @Test
    @DisplayName("실패: travelStyleIds가 빈 리스트인 경우")
    void createRecommendation_failure_emptyTravelStyleIds() {
        // given
        Long memberId = 1L;
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of())  // 빈 리스트
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(memberId)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        doReturn(List.of()).when(recommendationReasonRepository).findByRecommendationId(anyLong());

        // when
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(memberId, request);

        // then
        assertNotNull(response);
        assertNotNull(response.getRecommendations());
        assertTrue(response.getRecommendations().size() > 0);
    }

    @Test
    @DisplayName("실패: recommendationRequestRepository가 null을 반환하는 경우")
    void createRecommendation_failure_repositoryReturnsNull() {
        // given
        Long memberId = 1L;
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        // save()가 null을 반환
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(null);

        // when & then
        assertThrows(NullPointerException.class, () -> {
            destinationRecommendationService.createRecommendation(memberId, request);
        });
    }

    @Test
    @DisplayName("실패: RecommendationReason 조회 시 null 반환")
    void createRecommendation_failure_reasonsNotFound() {
        // given
        Long memberId = 1L;
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        RecommendationRequest savedRequest = RecommendationRequest.builder()
                .memberId(memberId)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        // mock 새로 생성
        RecommendationRequestRepository mockRequestRepo = mock(RecommendationRequestRepository.class);
        RecommendationRepository mockRecRepo = mock(RecommendationRepository.class);
        RecommendationReasonRepository mockReasonRepo = mock(RecommendationReasonRepository.class);

        when(mockRequestRepo.save(any(RecommendationRequest.class))).thenReturn(savedRequest);
        // findByRecommendationId가 null 반환
        doReturn(null).when(mockReasonRepo).findByRecommendationId(any());

        // service 새로 생성
        DestinationRecommendationService testService = new DestinationRecommendationService(
                mockRequestRepo,
                mockRecRepo,
                mockReasonRepo
        );

        // when & then
        // null이 반환되면 Stream에서 NullPointerException이 발생함
        assertThrows(NullPointerException.class, () -> {
            testService.createRecommendation(memberId, request);
        });
    }

    // ==================== listRecommendations 테스트 (List Recommendations) ====================

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 기본 케이스")
    void listRecommendations_success_basic() {
        // given
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).summary("첫 번째").createdAt(LocalDateTime.now()).build(),
                RecommendationRequest.builder().recommendationRequestId(2L).summary("두 번째").createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), (Pageable) any())).thenReturn(mockPage);

        // when
        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 10, "createdAt", "desc");

        // then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("첫 번째", result.getContent().get(0).getSummary());
        verify(recommendationRequestRepository).findAll(any(Specification.class), (Pageable) any());
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 지역 필터")
    void listRecommendations_success_filterByRegion() {
        // given
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).region("일본").summary("일본 여행").createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), (Pageable) any())).thenReturn(mockPage);

        // when
        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                "일본", null, null, null, null, 1, 10, "createdAt", "desc");

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recommendationRequestRepository).findAll(any(Specification.class), (Pageable) any());
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 여행 목적 필터")
    void listRecommendations_success_filterByTripPurpose() {
        // given
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).tripPurpose("휴식").summary("휴식 여행").createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), (Pageable) any())).thenReturn(mockPage);

        // when
        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                null, "휴식", null, null, null, 1, 10, "createdAt", "desc");

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recommendationRequestRepository).findAll(any(Specification.class), (Pageable) any());
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 계절 필터")
    void listRecommendations_success_filterBySeason() {
        // given
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).season("여름").summary("여름 여행").createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), (Pageable) any())).thenReturn(mockPage);

        // when
        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                null, null, "여름", null, null, 1, 10, "createdAt", "desc");

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recommendationRequestRepository).findAll(any(Specification.class), (Pageable) any());
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 날짜 범위 필터")
    void listRecommendations_success_filterByDateRange() {
        // given
        LocalDateTime from = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2023, 12, 31, 23, 59);
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).createdAt(LocalDateTime.of(2023, 6, 1, 0, 0)).summary("6월 여행").build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), (Pageable) any())).thenReturn(mockPage);

        // when
        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                null, null, null, from, to, 1, 10, "createdAt", "desc");

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recommendationRequestRepository).findAll(any(Specification.class), (Pageable) any());
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 정렬 오름차순")
    void listRecommendations_success_sortAsc() {
        // given
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).createdAt(LocalDateTime.of(2023, 1, 1, 0, 0)).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // when
        destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 10, "createdAt", "asc");

        // then
        verify(recommendationRequestRepository).findAll(any(Specification.class), argThat((Pageable pageable) ->
                pageable.getSort().toString().contains("createdAt,ASC,ignoreCase,ignoreRootNullMapping")));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 정렬 내림차순")
    void listRecommendations_success_sortDesc() {
        // given
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).createdAt(LocalDateTime.of(2023, 1, 1, 0, 0)).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // when
        destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 10, "createdAt", "desc");

        // then
        verify(recommendationRequestRepository).findAll(any(Specification.class), argThat((Pageable pageable) ->
                pageable.getSort().toString().contains("createdAt,DESC,ignoreCase,ignoreRootNullMapping")));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 기본 정렬 적용")
    void listRecommendations_success_defaultSort() {
        // given
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // when
        destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 10, null, null);

        // then
        verify(recommendationRequestRepository).findAll(any(Specification.class), argThat((Pageable pageable) ->
                pageable.getSort().toString().contains("createdAt,DESC")));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 빈 결과 반환")
    void listRecommendations_success_emptyResults() {
        // given
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of());
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // when
        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                "존재하지않는지역", null, null, null, null, 1, 10, "createdAt", "desc");

        // then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 여러 필터 조합")
    void listRecommendations_success_multipleFilters() {
        // given
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).region("태국").tripPurpose("쇼핑").season("겨울").createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // when
        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                "태국", "쇼핑", "겨울", null, null, 1, 10, "createdAt", "desc");

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recommendationRequestRepository).findAll(any(Specification.class), (Pageable) any());
    }
}
