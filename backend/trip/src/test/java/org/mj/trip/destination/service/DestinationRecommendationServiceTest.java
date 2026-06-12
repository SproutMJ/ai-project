package org.mj.trip.destination.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mj.trip.common.exception.ResourceNotFoundException;
import org.mj.trip.destination.domain.ReasonType;
import org.mj.trip.destination.domain.Recommendation;
import org.mj.trip.destination.domain.RecommendationRequest;
import org.mj.trip.destination.dto.DestinationRecommendationDetailResponse;
import org.mj.trip.destination.dto.DestinationRecommendationRequest;
import org.mj.trip.destination.dto.DestinationRecommendationResponse;
import org.mj.trip.destination.repository.RecommendationRepository;
import org.mj.trip.destination.repository.RecommendationRequestRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DestinationRecommendationService 테스트")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DestinationRecommendationServiceTest {

    @Mock
    private RecommendationRequestRepository recommendationRequestRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    private DestinationRecommendationService destinationRecommendationService;

    @BeforeEach
    void setUp() {
        recommendationRequestRepository = mock(RecommendationRequestRepository.class);
        recommendationRepository = mock(RecommendationRepository.class);

        doAnswer(invocation -> invocation.getArgument(0))
                .when(recommendationRepository).saveAll(any(List.class));

        destinationRecommendationService = new DestinationRecommendationService(
                recommendationRequestRepository,
                recommendationRepository
        );
    }

    private DestinationRecommendationRequest createRequest(String tripPurpose, List<Long> travelStyleIds,
                                                           String budgetRange, String region, String season,
                                                           Integer companionCount, Integer durationDays) {
        return DestinationRecommendationRequest.builder()
                .tripPurpose(tripPurpose)
                .travelStyleIds(travelStyleIds)
                .budgetRange(budgetRange)
                .region(region)
                .season(season)
                .companionCount(companionCount)
                .durationDays(durationDays)
                .build();
    }

    private RecommendationRequest createSavedRequest(Long memberId, String tripPurpose, String budgetRange,
                                                     String region, String season, Integer companionCount,
                                                     Integer durationDays) {
        return RecommendationRequest.builder()
                .recommendationRequestId(1L)
                .memberId(memberId)
                .tripPurpose(tripPurpose)
                .budgetRange(budgetRange)
                .region(region)
                .season(season)
                .companionCount(companionCount)
                .durationDays(durationDays)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== 생성 테스트 ====================

    @Test
    @DisplayName("성공: 여행지 추천 생성 - 기본 케이스")
    void createRecommendation_success_basic() {
        Long memberId = 1L;
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 2, 5
        );

        RecommendationRequest savedRequest = createSavedRequest(
                memberId, "휴식", "저예산", "일본", "여름", 2, 5
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(memberId, request);

        assertNotNull(response);
        assertEquals(savedRequest.getRecommendationRequestId(), response.getRecommendationRequestId());
        assertEquals(5, response.getRecommendations().size());
        assertNotNull(response.getCreatedAt());
        verify(recommendationRequestRepository).save(any(RecommendationRequest.class));
    }

    @Test
    @DisplayName("성공: 여행지 추천 생성 - 일본 지역")
    void createRecommendation_success_japan() {
        DestinationRecommendationRequest request = createRequest(
                "쇼핑", List.of(1L), "고예산", "일본", "봄", 3, 7
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "쇼핑", "고예산", "일본", "봄", 3, 7
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        assertNotNull(response);
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getDestinationName().equals("도쿄") || r.getDestinationName().equals("오사카")));
    }

    @Test
    @DisplayName("성공: 여행지 추천 생성 - 태국 지역")
    void createRecommendation_success_thailand() {
        DestinationRecommendationRequest request = createRequest(
                "액티비티", List.of(2L), "중예산", "태국", "겨울", 4, 10
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "액티비티", "중예산", "태국", "겨울", 4, 10
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        assertNotNull(response);
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getDestinationName().equals("방콕") || r.getDestinationName().equals("푸켓")));
    }

    @Test
    @DisplayName("성공: 여행지 추천 생성 - 베트남 지역")
    void createRecommendation_success_vietnam() {
        DestinationRecommendationRequest request = createRequest(
                "문화체험", List.of(3L), "저예산", "베트남", "가을", 1, 14
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "문화체험", "저예산", "베트남", "가을", 1, 14
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        assertNotNull(response);
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getDestinationName().equals("하노이") || r.getDestinationName().equals("호찌민")));
    }

    @Test
    @DisplayName("성공: 여행지 추천 생성 - 국내 지역")
    void createRecommendation_success_domestic() {
        DestinationRecommendationRequest request = createRequest(
                "맛집탐방", List.of(4L), "중예산", "국내", "여름", 2, 3
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "맛집탐방", "중예산", "국내", "여름", 2, 3
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        assertNotNull(response);
        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getDestinationName().equals("서울") || r.getDestinationName().equals("부산")));
    }

    @Test
    @DisplayName("성공: 휴식 목적 reasonSummary 검증")
    void createRecommendation_success_verifyReasonSummaryContainsTripPurpose() {
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 2, 5
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "휴식", "저예산", "일본", "여름", 2, 5
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getReasonSummary().contains("휴식") && r.getReasonSummary().contains("2명의 동반자와")));
    }

    @Test
    @DisplayName("성공: 쇼핑 목적 reasonSummary 검증")
    void createRecommendation_success_verifyReasonSummaryContainsShopping() {
        DestinationRecommendationRequest request = createRequest(
                "쇼핑", List.of(1L), "저예산", "일본", "여름", 3, 7
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "쇼핑", "저예산", "일본", "여름", 3, 7
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getReasonSummary().contains("쇼핑을 즐기기 좋고") && r.getReasonSummary().contains("7일 일정에 적합합니다.")));
    }

    @Test
    @DisplayName("성공: 동반자 수 reasonSummary 검증")
    void createRecommendation_success_verifyReasonSummaryContainsCompanionCount() {
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 5, 10
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "휴식", "저예산", "일본", "여름", 5, 10
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getReasonSummary().contains("5명의 동반자와")));
    }

    @Test
    @DisplayName("성공: 여행 기간 reasonSummary 검증")
    void createRecommendation_success_verifyReasonSummaryContainsDurationDays() {
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 2, 15
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "휴식", "저예산", "일본", "여름", 2, 15
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        assertTrue(response.getRecommendations().stream()
                .anyMatch(r -> r.getReasonSummary().contains("15일 일정에 적합합니다.")));
    }

    @Test
    @DisplayName("성공: 점수 범위 검증 (80.0 ~ 100.0)")
    void createRecommendation_success_verifyScoreRange() {
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 2, 5
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "휴식", "저예산", "일본", "여름", 2, 5
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

        response.getRecommendations().forEach(rec -> {
            assertTrue(rec.getScore() >= 80.0, "점수는 80.0 이상이어야 합니다.");
            assertTrue(rec.getScore() <= 100.0, "점수는 100.0 이하여야 합니다.");
        });
    }

    @Test
    @DisplayName("성공: 랭킹 순서 검증 (1 ~ 5)")
    void createRecommendation_success_verifyRankOrder() {
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 2, 5
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "휴식", "저예산", "일본", "여름", 2, 5
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(1L, request);

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
    @DisplayName("성공: Repository 저장 호출 검증")
    void createRecommendation_success_verifyRepositoryCalls() {
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 2, 5
        );

        RecommendationRequest savedRequest = createSavedRequest(
                1L, "휴식", "저예산", "일본", "여름", 2, 5
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        destinationRecommendationService.createRecommendation(1L, request);

        verify(recommendationRequestRepository, times(1)).save(any(RecommendationRequest.class));
    }

    @Test
    @DisplayName("실패: RecommendationRequest 저장 시 데이터AccessException 발생")
    void createRecommendation_failure_recommendationRequestRepositoryException() {
        Long memberId = 1L;
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 2, 5
        );

        when(recommendationRequestRepository.save(any(RecommendationRequest.class)))
                .thenThrow(new org.springframework.dao.DataAccessException("DB error") {
                });

        assertThrows(org.springframework.dao.DataAccessException.class, () ->
                destinationRecommendationService.createRecommendation(memberId, request));

    }

    @Test
    @DisplayName("실패: region이 null인 경우 - NullPointerException 발생")
    void createRecommendation_failure_nullRegion() {
        Long memberId = 1L;
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", null, "여름", 2, 5
        );

        RecommendationRequest savedRequest = createSavedRequest(
                memberId, "휴식", "저예산", null, "여름", 2, 5
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        assertThrows(NullPointerException.class, () ->
                destinationRecommendationService.createRecommendation(memberId, request));
    }

    @Test
    @DisplayName("실패: companionCount가 0인 경우 - IllegalArgumentException 발생")
    void createRecommendation_failure_zeroCompanionCount() {
        Long memberId = 1L;

        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 0, 5
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> destinationRecommendationService.createRecommendation(memberId, request)
        );

        assertEquals("동반자 수는 1명 이상이어야 합니다.", exception.getMessage());

        verify(recommendationRequestRepository, never()).save(any());
        verify(recommendationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("실패: durationDays가 0인 경우 - IllegalArgumentException 발생")
    void createRecommendation_failure_zeroDurationDays() {
        Long memberId = 1L;

        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 2, 0
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> destinationRecommendationService.createRecommendation(memberId, request)
        );

        assertEquals("여행 기간은 1일 이상이어야 합니다.", exception.getMessage());

        verify(recommendationRequestRepository, never())
                .save(any(RecommendationRequest.class));
    }

    @Test
    @DisplayName("실패: tripPurpose가 빈 문자열인 경우")
    void createRecommendation_failure_emptyTripPurpose() {
        Long memberId = 1L;
        DestinationRecommendationRequest request = createRequest(
                "", List.of(1L), "저예산", "일본", "여름", 2, 5
        );

        RecommendationRequest savedRequest = createSavedRequest(
                memberId, "", "저예산", "일본", "여름", 2, 5
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(memberId, request);

        assertNotNull(response);
        assertNotNull(response.getRecommendations());
        assertTrue(response.getRecommendations().size() > 0);
    }

    @Test
    @DisplayName("실패: travelStyleIds가 빈 리스트인 경우")
    void createRecommendation_failure_emptyTravelStyleIds() {
        Long memberId = 1L;
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(), "저예산", "일본", "여름", 2, 5
        );

        RecommendationRequest savedRequest = createSavedRequest(
                memberId, "휴식", "저예산", "일본", "여름", 2, 5
        );
        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(savedRequest);

        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(memberId, request);

        assertNotNull(response);
        assertNotNull(response.getRecommendations());
        assertTrue(response.getRecommendations().size() > 0);
    }

    @Test
    @DisplayName("실패: recommendationRequestRepository가 null을 반환하는 경우")
    void createRecommendation_failure_repositoryReturnsNull() {
        Long memberId = 1L;
        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 2, 5
        );

        when(recommendationRequestRepository.save(any(RecommendationRequest.class))).thenReturn(null);

        assertThrows(NullPointerException.class, () ->
                destinationRecommendationService.createRecommendation(memberId, request));
    }

    @Test
    @DisplayName("성공: RecommendationReason 조회 결과가 null이면 빈 추천 목록 반환")
    void createRecommendation_success_whenReasonsAreNull() {
        Long memberId = 1L;

        DestinationRecommendationRequest request = createRequest(
                "휴식", List.of(1L), "저예산", "일본", "여름", 2, 5
        );

        RecommendationRequest savedRequest = createSavedRequest(
                memberId, "휴식", "저예산", "일본", "여름", 2, 5
        );

        RecommendationRequestRepository mockRequestRepo = mock(RecommendationRequestRepository.class);
        RecommendationRepository mockRecRepo = mock(RecommendationRepository.class);

        when(mockRequestRepo.save(any(RecommendationRequest.class)))
                .thenReturn(savedRequest);


        DestinationRecommendationService testService =
                new DestinationRecommendationService(
                        mockRequestRepo,
                        mockRecRepo
                );

        DestinationRecommendationResponse response =
                testService.createRecommendation(memberId, request);

        assertNotNull(response);
        assertNotNull(response.getRecommendations());
        assertTrue(response.getRecommendations().isEmpty());
    }

    // ==================== 목록 조회 테스트 ====================

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 기본 케이스")
    void listRecommendations_success_basic() {
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).summary("첫 번째").createdAt(LocalDateTime.now()).build(),
                RecommendationRequest.builder().recommendationRequestId(2L).summary("두 번째").createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 10, "createdAt", "desc");

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("첫 번째", result.getContent().get(0).getSummary());
        verify(recommendationRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 지역 필터")
    void listRecommendations_success_filterByRegion() {
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).region("일본").summary("일본 여행").createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                "일본", null, null, null, null, 1, 10, "createdAt", "desc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recommendationRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 여행 목적 필터")
    void listRecommendations_success_filterByTripPurpose() {
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).tripPurpose("휴식").summary("휴식 여행").createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                null, "휴식", null, null, null, 1, 10, "createdAt", "desc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recommendationRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 계절 필터")
    void listRecommendations_success_filterBySeason() {
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).season("여름").summary("여름 여행").createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                null, null, "여름", null, null, 1, 10, "createdAt", "desc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recommendationRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 날짜 범위 필터")
    void listRecommendations_success_filterByDateRange() {
        LocalDateTime from = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2023, 12, 31, 23, 59);
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).createdAt(LocalDateTime.of(2023, 6, 1, 0, 0)).summary("6월 여행").build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                null, null, null, from, to, 1, 10, "createdAt", "desc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recommendationRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 정렬 오름차순")
    void listRecommendations_success_sortAsc() {
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).createdAt(LocalDateTime.of(2023, 1, 1, 0, 0)).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 10, "createdAt", "asc");

        verify(recommendationRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 정렬 내림차순")
    void listRecommendations_success_sortDesc() {
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).createdAt(LocalDateTime.of(2023, 1, 1, 0, 0)).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 10, "createdAt", "desc");

        verify(recommendationRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 기본 정렬 적용")
    void listRecommendations_success_defaultSort() {
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 10, null, null);

        verify(recommendationRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 빈 결과 반환")
    void listRecommendations_success_emptyResults() {
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of());
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                "존재하지않는지역", null, null, null, null, 1, 10, "createdAt", "desc");

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("성공: 여행지 추천 목록 조회 - 여러 필터 조합")
    void listRecommendations_success_multipleFilters() {
        Page<RecommendationRequest> mockPage = new PageImpl<>(List.of(
                RecommendationRequest.builder().recommendationRequestId(1L).region("태국").tripPurpose("쇼핑").season("겨울").createdAt(LocalDateTime.now()).build()
        ));
        when(recommendationRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                "태국", "쇼핑", "겨울", null, null, 1, 10, "createdAt", "desc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recommendationRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // ==================== 상세 조회 테스트 ====================

    @Test
    @DisplayName("성공: 추천 상세 조회")
    void getRecommendationDetail_success() {
        RecommendationRequest requestEntity = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .summary("테스트 요약")
                .createdAt(LocalDateTime.now())
                .build();
        when(recommendationRequestRepository.findById(1L)).thenReturn(Optional.of(requestEntity));

        Recommendation rec1 = Recommendation.builder()
                .recommendationRequestId(1L)
                .destinationId(100L)
                .destinationName("도쿄")
                .score(95.0)
                .rankOrder(1)
                .reasonSummary("reason1")
                .build();

        Recommendation rec2 = Recommendation.builder()
                .recommendationRequestId(1L)
                .destinationId(101L)
                .destinationName("오사카")
                .score(90.0)
                .rankOrder(2)
                .reasonSummary("reason2")
                .build();

        when(recommendationRepository.findByRecommendationRequestIdOrderByRankOrderAsc(any(Long.class)))
                .thenReturn(List.of(rec1, rec2));

        DestinationRecommendationDetailResponse.DestinationRecommendationDetailData result =
                destinationRecommendationService.getRecommendationDetail(1L);

        assertNotNull(result);
        assertEquals("휴식", result.getTripPurpose());
        assertEquals("저예산", result.getBudgetRange());
        assertEquals("일본", result.getRegion());
        assertEquals("여름", result.getSeason());
        assertNotNull(result.getCreatedAt());
        assertEquals(2, result.getRecommendations().size());
    }

    @Test
    @DisplayName("실패: 추천 상세 조회 - 존재하지 않는 요청 (404)")
    void getRecommendationDetail_notFound() {
        when(recommendationRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> destinationRecommendationService.getRecommendationDetail(99L));
    }

    // ==================== 삭제 테스트 ====================

    @Test
    @DisplayName("성공: 추천 요청 삭제 - 관련 데이터 포함")
    void deleteRecommendationRequest_success() {
        // given
        RecommendationRequest requestEntity = RecommendationRequest.builder()
                .recommendationRequestId(1L)
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .summary("테스트 요약")
                .createdAt(LocalDateTime.now())
                .build();
        when(recommendationRequestRepository.findById(1L)).thenReturn(Optional.of(requestEntity));

        Recommendation rec1 = Recommendation.builder()
                .id(1L)
                .recommendationRequestId(1L)
                .destinationId(100L)
                .destinationName("도쿄")
                .score(95.0)
                .rankOrder(1)
                .reasonSummary("reason1")
                .build();

        Recommendation rec2 = Recommendation.builder()
                .id(2L)
                .recommendationRequestId(1L)
                .destinationId(101L)
                .destinationName("오사카")
                .score(90.0)
                .rankOrder(2)
                .reasonSummary("reason2")
                .build();

        when(recommendationRepository.findByRecommendationRequestIdOrderByRankOrderAsc(1L))
                .thenReturn(List.of(rec1, rec2));

        // when
        destinationRecommendationService.deleteRecommendationRequest(1L);

        // then
        verify(recommendationRepository, times(1)).deleteByRecommendationRequestId(1L);
        verify(recommendationRequestRepository, times(1)).delete(requestEntity);
    }

    @Test
    @DisplayName("실패: 추천 요청 삭제 - 존재하지 않는 요청 (404)")
    void deleteRecommendationRequest_notFound() {
        // given
        when(recommendationRequestRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () ->
                destinationRecommendationService.deleteRecommendationRequest(99L));

        verify(recommendationRepository, never()).findByRecommendationRequestIdOrderByRankOrderAsc(anyLong());
        verify(recommendationRepository, never()).deleteByRecommendationRequestId(anyLong());
        verify(recommendationRequestRepository, never()).delete((RecommendationRequest) any());
    }
}
