package wayble.ade.match.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import wayble.ade.common.data.type.ApiResponseCodeType;
import wayble.ade.common.exception.CommonException;
import wayble.ade.common.exception.ValidationException;
import wayble.ade.common.repository.MatchCommonRepository;
import wayble.ade.common.repository.MatchEstimateRepository;
import wayble.ade.common.repository.MatchQuoteRepository;
import wayble.ade.common.repository.ExhstQuoteRepository;
import wayble.ade.common.util.TokenUtil;
import wayble.ade.match.dto.ExhstEstimateByEmailLinkDto;
import wayble.ade.match.dto.ManageQuoteEstimatePreviewDto;
import wayble.ade.match.dto.ManageQuoteExhstInfoUpdateDto;
import wayble.ade.match.dto.PaginationDto;
import wayble.ade.match.dto.ManageQuoteListDto;
import wayble.ade.match.dto.request.ExhstQuoteDto;
import wayble.ade.match.dto.request.ExhstQuoteWasteDto;
import wayble.ade.match.dto.request.ExhstQuoteDetailDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchQuoteServiceImplTest {
	@Mock
	private MatchQuoteRepository matchQuoteRepository;
	@Mock
	private MatchEstimateRepository matchEstimateRepository;
	@Mock
	private ExhstQuoteRepository exhstQuoteRepository;
	@Mock
	private wayble.ade.common.service.FileService fileService;
	@Mock
	private wayble.ade.common.service.EmailService emailService;
	@Mock
	private MatchCommonRepository matchCommonRepository;
	@Mock
	private wayble.ade.common.repository.MatchQuoteWasteRepository matchQuoteWasteRepository;
	@Mock
	private wayble.ade.common.service.SmsService smsService;
	@Mock
	private wayble.ade.common.repository.UserRepository userRepository;
	@InjectMocks
	private MatchQuoteServiceImpl service;

	@Test
	@DisplayName("정상 케이스 호출")
	void getQuoteListTest001() {
		ManageQuoteListDto request = ManageQuoteListDto.builder()
				.startDate(LocalDate.of(2025, 1, 1))
				.endDate(LocalDate.of(2025, 1, 31))
				.page(2)
				.rowsPerPage(10)
				.exhaustName("  ")
				.status(null)
				.sort(null)
				.build();

		when(matchQuoteRepository.countQuoteList(anyMap())).thenReturn(100L);

		List<Map<String, Object>> mockQuoteList = new ArrayList<>();

		Map<String, Object> quote = new HashMap<>();
		quote.put("quoteId", "QID");
		quote.put("estmtSttsCd", "견적의뢰");
		quote.put("addr", "주소");
		quote.put("wsteCnt", 3);
		quote.put("quoteRqstDt", LocalDate.of(2025, 12, 1));
		quote.put("estmtRqstCnt", 2);
		quote.put("estmtSbmtCnt", 2);
		quote.put("expireDt", LocalDate.of(2025, 12, 10));
		quote.put("estmtSendDt", null);
		quote.put("inspctCnt", 1);

		mockQuoteList.add(quote);

		List<Map<String, Object>> mockEstimateList = new ArrayList<>();
		Map<String, Object> estimate = new HashMap<>();
		estimate.put("quoteId", "QID");
		estimate.put("entrpsNm", "테스트");
		estimate.put("bidSttsCd", "입찰완료");
		estimate.put("inspctPrcsYn", "Y");
		mockEstimateList.add(estimate);

		when(matchEstimateRepository.getEstimateSummaryListByQuoteId(anyMap())).thenReturn(mockEstimateList);

		when(matchQuoteRepository.getQuoteList(anyMap())).thenReturn(mockQuoteList);

		Map<String, Object> result = service.getQuoteList(request);

		assertNotNull(result);
		assertTrue(result.containsKey("quoteList"));
		assertTrue(result.containsKey("paging"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> quoteList = (List<Map<String, Object>>) result.get("quoteList");
		assertNotNull(quoteList);
		assertFalse(quoteList.isEmpty());
		assertEquals(1, quoteList.size());

		Map<String, Object> firstQuote = quoteList.get(0);

		assertEquals("QID", firstQuote.get("quoteId"));
		assertEquals("견적의뢰", firstQuote.get("estmtSttsCd"));
		assertEquals("주소", firstQuote.get("addr"));
		assertEquals(3, firstQuote.get("wsteCnt"));
		assertEquals(LocalDate.of(2025, 12, 1), firstQuote.get("quoteRqstDt"));
		assertEquals(2, firstQuote.get("estmtRqstCnt"));
		assertEquals(2, firstQuote.get("estmtSbmtCnt"));
		assertEquals(LocalDate.of(2025, 12, 10), firstQuote.get("expireDt"));
		assertNull(firstQuote.get("estmtSendDt"));
		assertEquals(1, firstQuote.get("inspctCnt"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> estimatePartnerList = (List<Map<String, Object>>) firstQuote.get("estimatePartnerList");

		assertNotNull(estimatePartnerList);
		assertEquals(1, estimatePartnerList.size());
		assertEquals("테스트", estimatePartnerList.get(0).get("entrpsNm"));
		assertEquals("입찰완료", estimatePartnerList.get(0).get("bidSttsCd"));
		assertEquals("Y", estimatePartnerList.get(0).get("inspctPrcsYn"));

		Object pagingObj = result.get("paging");
		assertNotNull(pagingObj);
		assertInstanceOf(PaginationDto.class, pagingObj);

		PaginationDto paging = (PaginationDto) pagingObj;
		assertEquals(2, paging.getPage());
		assertEquals(10, paging.getRowsPerPage());
		assertEquals(100L, paging.getTotalRows()); // 하드 코딩 되어있음.
		assertEquals(10, paging.getTotalPages());
	}

	@Test
	@DisplayName("page 와 rowsPerPage가 null 인경우")
	void getQuoteListTest002() {
		ManageQuoteListDto request = ManageQuoteListDto.builder()
				.startDate(LocalDate.of(2025, 1, 1))
				.endDate(LocalDate.of(2025, 1, 31))
				.page(null)
				.rowsPerPage(null)
				.build();

		Map<String, Object> result = service.getQuoteList(request);

		PaginationDto paging = (PaginationDto) result.get("paging");
		assertEquals(1, paging.getPage());
		assertEquals(10, paging.getRowsPerPage());
	}

	// ==================== createQuote 테스트 ====================
	@Test
	@DisplayName("견적 요청 등록 성공 - 이미지 없음")
	void createQuoteSuccessWithoutImages() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();

		// Repository mock 설정
		when(matchQuoteRepository.insertQuote(anyMap())).thenReturn(1);
		when(matchQuoteWasteRepository.insertQuoteWaste(anyMap())).thenReturn(1);

		// 플랫폼 관리자 목록 mock 설정
		List<Map<String, Object>> platformAdmins = new ArrayList<>();
		Map<String, Object> admin1 = new HashMap<>();
		admin1.put("email", "admin1@wayble.co.kr");
		platformAdmins.add(admin1);

		Map<String, Object> admin2 = new HashMap<>();
		admin2.put("email", "admin2@wayble.co.kr");
		platformAdmins.add(admin2);

		when(userRepository.getPlatformAdministrator()).thenReturn(platformAdmins);

		// When
		Map<String, Object> result = service.createQuote(request, null, "test-user-agent", 123L);

		// Then
		assertNotNull(result);
		assertNotNull(result.get("quoteId"));
		assertTrue(result.get("quoteId").toString().startsWith("Q"));

		// Repository 호출 검증
		verify(matchQuoteRepository, times(1)).insertQuote(anyMap());
		verify(matchQuoteWasteRepository, times(1)).insertQuoteWaste(anyMap());

		// 이메일 발송 검증 (NOTI001, NOTI003)
		verify(emailService, times(1)).send(eq(wayble.ade.common.data.type.EmailTemplateType.NOTI001), anyMap());
		verify(emailService, times(1)).send(eq(wayble.ade.common.data.type.EmailTemplateType.NOTI003), anyList(), anyMap());

		// SMS 발송 검증 (NOTI002)
		verify(smsService, times(1)).send(anyMap());
	}

	@Test
	@DisplayName("견적 요청 등록 성공 - 이미지 1개")
	void createQuoteSuccessWithOneImage() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();
		List<MultipartFile> images = createValidImageFiles(1);

		// FileService mock 설정
		when(fileService.getFileExtension("test0.jpg")).thenReturn("jpg");

		// uploadFileList mock 설정
		Map<String, Object> mockFileInfo = new HashMap<>();
		mockFileInfo.put("fileGrpId", "F20250119123456789");
		when(fileService.uploadFileList(anyList(), eq("uploads"), isNull(), eq(123L))).thenReturn(mockFileInfo);

		// Repository mock 설정
		when(matchQuoteRepository.insertQuote(anyMap())).thenReturn(1);
		when(matchQuoteWasteRepository.insertQuoteWaste(anyMap())).thenReturn(1);

		// 플랫폼 관리자 목록 mock 설정
		List<Map<String, Object>> platformAdmins = new ArrayList<>();
		Map<String, Object> admin1 = new HashMap<>();
		admin1.put("email", "admin1@wayble.co.kr");
		platformAdmins.add(admin1);

		Map<String, Object> admin2 = new HashMap<>();
		admin2.put("email", "admin2@wayble.co.kr");
		platformAdmins.add(admin2);

		when(userRepository.getPlatformAdministrator()).thenReturn(platformAdmins);

		// When
		Map<String, Object> result = service.createQuote(request, images, "test-user-agent", 123L);

		// Then
		assertNotNull(result);
		assertNotNull(result.get("quoteId"));
		assertTrue(result.get("quoteId").toString().startsWith("Q"));

		// FileService 및 Repository 호출 검증
		verify(fileService, times(1)).uploadFileList(anyList(), eq("uploads"), isNull(), eq(123L));
		verify(matchQuoteRepository, times(1)).insertQuote(anyMap());
		verify(matchQuoteWasteRepository, times(1)).insertQuoteWaste(anyMap());

		// 이메일 발송 검증 (NOTI001, NOTI003)
		verify(emailService, times(1)).send(eq(wayble.ade.common.data.type.EmailTemplateType.NOTI001), anyMap());
		verify(emailService, times(1)).send(eq(wayble.ade.common.data.type.EmailTemplateType.NOTI003), anyList(), anyMap());

		// SMS 발송 검증 (NOTI002)
		verify(smsService, times(1)).send(anyMap());
	}

	@Test
	@DisplayName("견적 요청 등록 성공 - 이미지 3개 (최대)")
	void createQuoteSuccessWithMaxImages() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();
		List<MultipartFile> images = createValidImageFiles(3);

		// FileService mock 설정
		when(fileService.getFileExtension("test0.jpg")).thenReturn("jpg");
		when(fileService.getFileExtension("test1.jpg")).thenReturn("jpg");
		when(fileService.getFileExtension("test2.jpg")).thenReturn("jpg");

		// uploadFileList mock 설정
		Map<String, Object> mockFileInfo = new HashMap<>();
		mockFileInfo.put("fileGrpId", "F20250119123456789");
		when(fileService.uploadFileList(anyList(), eq("uploads"), isNull(), eq(123L))).thenReturn(mockFileInfo);

		// Repository mock 설정
		when(matchQuoteRepository.insertQuote(anyMap())).thenReturn(1);
		when(matchQuoteWasteRepository.insertQuoteWaste(anyMap())).thenReturn(1);

		// 플랫폼 관리자 목록 mock 설정
		List<Map<String, Object>> platformAdmins = new ArrayList<>();
		Map<String, Object> admin1 = new HashMap<>();
		admin1.put("email", "admin1@wayble.co.kr");
		platformAdmins.add(admin1);

		Map<String, Object> admin2 = new HashMap<>();
		admin2.put("email", "admin2@wayble.co.kr");
		platformAdmins.add(admin2);

		when(userRepository.getPlatformAdministrator()).thenReturn(platformAdmins);

		// When
		Map<String, Object> result = service.createQuote(request, images, "test-user-agent", 123L);

		// Then
		assertNotNull(result);
		assertNotNull(result.get("quoteId"));
		assertTrue(result.get("quoteId").toString().startsWith("Q"));

		// FileService 및 Repository 호출 검증
		verify(fileService, times(1)).uploadFileList(anyList(), eq("uploads"), isNull(), eq(123L));
		verify(matchQuoteRepository, times(1)).insertQuote(anyMap());
		verify(matchQuoteWasteRepository, times(1)).insertQuoteWaste(anyMap());

		// 이메일 발송 검증 (NOTI001, NOTI003)
		verify(emailService, times(1)).send(eq(wayble.ade.common.data.type.EmailTemplateType.NOTI001), anyMap());
		verify(emailService, times(1)).send(eq(wayble.ade.common.data.type.EmailTemplateType.NOTI003), anyList(), anyMap());

		// SMS 발송 검증 (NOTI002)
		verify(smsService, times(1)).send(anyMap());
	}

	@Test
	@DisplayName("견적 요청 등록 실패 - 이미지 개수 초과 (4개)")
	void createQuoteFailTooManyImages() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();
		List<MultipartFile> images = createValidImageFiles(4);

		// When & Then
		ValidationException exception = assertThrows(ValidationException.class, () -> service.createQuote(request, images, "test-user-agent", 123L));

		assertEquals("이미지는 최대 3개까지 업로드 가능합니다.", exception.getCode());
	}

	@Test
	@DisplayName("견적 요청 등록 실패 - 빈 이미지 파일")
	void createQuoteFailEmptyImage() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();
		List<MultipartFile> images = new ArrayList<>();
		images.add(new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]));

		// When & Then
		ValidationException exception = assertThrows(ValidationException.class, () -> service.createQuote(request, images, "test-user-agent", 123L));

		assertEquals("빈 이미지 파일은 업로드할 수 없습니다.", exception.getCode());
	}

	@Test
	@DisplayName("견적 요청 등록 실패 - 파일 크기 초과 (10MB 초과)")
	void createQuoteFailImageSizeExceeded() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();
		List<MultipartFile> images = new ArrayList<>();

		// 10MB보다 큰 파일 (10MB + 1 byte)
		byte[] largeContent = new byte[10 * 1024 * 1024 + 1];
		images.add(new MockMultipartFile("file", "large.jpg", "image/jpeg", largeContent));

		// When & Then
		ValidationException exception = assertThrows(ValidationException.class, () -> service.createQuote(request, images, "test-user-agent", 123L));

		assertEquals("이미지 파일 크기는 10MB를 초과할 수 없습니다.", exception.getCode());
	}

	@Test
	@DisplayName("견적 요청 등록 실패 - 허용되지 않은 확장자 (gif)")
	void createQuoteFailInvalidExtension() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();
		List<MultipartFile> images = new ArrayList<>();
		images.add(new MockMultipartFile("file", "test.gif", "image/gif", "test content".getBytes()));

		// FileService mock 설정
		when(fileService.getFileExtension("test.gif")).thenReturn("gif");

		// When & Then
		ValidationException exception = assertThrows(ValidationException.class, () -> service.createQuote(request, images, "test-user-agent", 123L));

		assertEquals("이미지 파일은 jpg, jpeg, png 형식만 업로드 가능합니다.", exception.getCode());
	}

	@Test
	@DisplayName("견적 요청 등록 실패 - Content-Type이 image가 아님")
	void createQuoteFailInvalidContentType() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();
		List<MultipartFile> images = new ArrayList<>();
		images.add(new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes()));

		// FileService mock 설정 (확장자 체크가 Content-Type 체크보다 먼저 실행됨)
		when(fileService.getFileExtension("test.txt")).thenReturn("txt");

		// When & Then
		ValidationException exception = assertThrows(ValidationException.class, () -> service.createQuote(request, images, "test-user-agent", 123L));

		// txt 확장자로 인해 확장자 검증에서 먼저 실패
		assertEquals("이미지 파일은 jpg, jpeg, png 형식만 업로드 가능합니다.", exception.getCode());
	}

	@Test
	@DisplayName("견적 요청 등록 실패 - 파일명이 없음")
	void createQuoteFailNoFilename() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();
		List<MultipartFile> images = new ArrayList<>();
		images.add(new MockMultipartFile("file", "", "image/jpeg", "test content".getBytes()));

		// When & Then
		ValidationException exception = assertThrows(ValidationException.class, () -> service.createQuote(request, images, "test-user-agent", 123L));

		assertEquals("이미지 파일명이 올바르지 않습니다.", exception.getCode());
	}

	@Test
	@DisplayName("견적 요청 등록 실패 - jpg 확장자이지만 Content-Type이 image가 아님")
	void createQuoteFailValidExtensionButInvalidContentType() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();
		List<MultipartFile> images = new ArrayList<>();
		// jpg 확장자를 가지지만 Content-Type은 text/plain
		images.add(new MockMultipartFile("file", "test.jpg", "text/plain", "test content".getBytes()));

		// FileService mock 설정
		when(fileService.getFileExtension("test.jpg")).thenReturn("jpg");

		// When & Then
		ValidationException exception = assertThrows(ValidationException.class, () ->
				service.createQuote(request, images, "test-user-agent", 123L)
		);

		assertEquals("이미지 파일만 업로드 가능합니다.", exception.getCode());
	}

	@Test
	@DisplayName("견적 요청 등록 성공 - 다양한 이미지 확장자 (jpg, jpeg, png)")
	void createQuoteSuccessWithVariousExtensions() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();
		List<MultipartFile> images = new ArrayList<>();
		images.add(new MockMultipartFile("file1", "test1.jpg", "image/jpeg", "test content".getBytes()));
		images.add(new MockMultipartFile("file2", "test2.jpeg", "image/jpeg", "test content".getBytes()));
		images.add(new MockMultipartFile("file3", "test3.png", "image/png", "test content".getBytes()));

		// FileService mock 설정
		when(fileService.getFileExtension("test1.jpg")).thenReturn("jpg");
		when(fileService.getFileExtension("test2.jpeg")).thenReturn("jpeg");
		when(fileService.getFileExtension("test3.png")).thenReturn("png");

		// uploadFileList mock 설정
		Map<String, Object> mockFileInfo = new HashMap<>();
		mockFileInfo.put("fileGrpId", "F20250119123456789");
		when(fileService.uploadFileList(anyList(), eq("uploads"), isNull(), eq(123L))).thenReturn(mockFileInfo);

		// Repository mock 설정
		when(matchQuoteRepository.insertQuote(anyMap())).thenReturn(1);
		when(matchQuoteWasteRepository.insertQuoteWaste(anyMap())).thenReturn(1);

		// 플랫폼 관리자 목록 mock 설정
		List<Map<String, Object>> platformAdmins = new ArrayList<>();
		Map<String, Object> admin1 = new HashMap<>();
		admin1.put("email", "admin1@wayble.co.kr");
		platformAdmins.add(admin1);

		Map<String, Object> admin2 = new HashMap<>();
		admin2.put("email", "admin2@wayble.co.kr");
		platformAdmins.add(admin2);

		when(userRepository.getPlatformAdministrator()).thenReturn(platformAdmins);

		// When
		Map<String, Object> result = service.createQuote(request, images, "test-user-agent", 123L);

		// Then
		assertNotNull(result);
		assertNotNull(result.get("quoteId"));
		assertTrue(result.get("quoteId").toString().startsWith("Q"));

		// FileService 및 Repository 호출 검증
		verify(fileService, times(1)).uploadFileList(anyList(), eq("uploads"), isNull(), eq(123L));
		verify(matchQuoteRepository, times(1)).insertQuote(anyMap());
		verify(matchQuoteWasteRepository, times(1)).insertQuoteWaste(anyMap());

		// 이메일 발송 검증 (NOTI001, NOTI003)
		verify(emailService, times(1)).send(eq(wayble.ade.common.data.type.EmailTemplateType.NOTI001), anyMap());
		verify(emailService, times(1)).send(eq(wayble.ade.common.data.type.EmailTemplateType.NOTI003), anyList(), anyMap());

		// SMS 발송 검증 (NOTI002)
		verify(smsService, times(1)).send(anyMap());
	}

	@Test
	@DisplayName("견적 요청 등록 성공 - 이메일/SMS 발송 데이터 검증 (NOTI001, NOTI002, NOTI003)")
	void createQuoteSuccessWithEmailDataVerification() {
		// Given
		ExhstQuoteDto request = createValidQuoteRequest();

		// Repository mock 설정
		when(matchQuoteRepository.insertQuote(anyMap())).thenReturn(1);
		when(matchQuoteWasteRepository.insertQuoteWaste(anyMap())).thenReturn(1);

		// 플랫폼 관리자 목록 mock 설정
		List<Map<String, Object>> platformAdmins = new ArrayList<>();
		Map<String, Object> admin1 = new HashMap<>();
		admin1.put("email", "admin1@wayble.co.kr");
		platformAdmins.add(admin1);

		Map<String, Object> admin2 = new HashMap<>();
		admin2.put("email", "admin2@wayble.co.kr");
		platformAdmins.add(admin2);

		when(userRepository.getPlatformAdministrator()).thenReturn(platformAdmins);

		// When
		Map<String, Object> result = service.createQuote(request, null, "test-user-agent", 123L);

		// Then
		assertNotNull(result);
		assertTrue(result.get("quoteId").toString().startsWith("Q"));

		// NOTI001 이메일 발송 검증 (배출자)
		ArgumentCaptor<wayble.ade.common.data.type.EmailTemplateType> noti001TemplateCaptor =
			ArgumentCaptor.forClass(wayble.ade.common.data.type.EmailTemplateType.class);
		ArgumentCaptor<Map> noti001DataCaptor = ArgumentCaptor.forClass(Map.class);

		verify(emailService, times(1)).send(
			eq(wayble.ade.common.data.type.EmailTemplateType.NOTI001),
			noti001DataCaptor.capture()
		);

		Map<String, Object> noti001Data = noti001DataCaptor.getValue();
		assertEquals("test@example.com", noti001Data.get("rcvAddr"));
		assertEquals("서울시 강남구 테헤란로 123", noti001Data.get("roadAddr"));
		assertEquals("010-1234-5678", noti001Data.get("phoneNo"));
		assertEquals("test@example.com", noti001Data.get("rqstEmail"));
		assertEquals("기타 요청사항", noti001Data.get("etcCmnt"));

		// NOTI002 SMS 발송 검증 (배출자)
		ArgumentCaptor<Map> smsDataCaptor = ArgumentCaptor.forClass(Map.class);
		verify(smsService, times(1)).send(smsDataCaptor.capture());

		Map<String, Object> smsData = smsDataCaptor.getValue();
		assertEquals("010-1234-5678", smsData.get("rcvAddr"));
		assertTrue(((String) smsData.get("message")).contains("폐기물 처리 견적 요청이 접수 되었습니다"));
		assertEquals("[WAYBLE] 폐기물 처리 견적 신청 안내", smsData.get("subject"));

		// NOTI003 이메일 발송 검증 (플랫폼 관리자)
		ArgumentCaptor<List> recipientsCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Map> noti003DataCaptor = ArgumentCaptor.forClass(Map.class);

		verify(emailService, times(1)).send(
			eq(wayble.ade.common.data.type.EmailTemplateType.NOTI003),
			recipientsCaptor.capture(),
			noti003DataCaptor.capture()
		);

		// 플랫폼 관리자 이메일 목록 검증 (DB에서 조회한 값)
		List<String> recipients = recipientsCaptor.getValue();
		assertEquals(2, recipients.size());
		assertTrue(recipients.contains("admin1@wayble.co.kr"));
		assertTrue(recipients.contains("admin2@wayble.co.kr"));

		Map<String, Object> noti003Data = noti003DataCaptor.getValue();
		assertEquals("서울시 강남구 테헤란로 123", noti003Data.get("roadAddr"));
		assertEquals("010-1234-5678", noti003Data.get("phoneNo"));
		assertEquals("test@example.com", noti003Data.get("rqstEmail"));
		assertEquals("기타 요청사항", noti003Data.get("etcCmnt"));
		assertTrue(noti003Data.get("quoteId").toString().startsWith("Q"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> wasteItems = (List<Map<String, Object>>) noti003Data.get("wasteItems");
		assertEquals(1, wasteItems.size());
		assertEquals("일반 폐기물", wasteItems.get(0).get("itemName"));
		assertEquals(BigDecimal.valueOf(100.0), wasteItems.get(0).get("monthlyAmount"));
	}

	// ==================== Helper 메서드 ====================

	/**
	 * 유효한 견적 요청 DTO 생성
	 */
	private ExhstQuoteDto createValidQuoteRequest() {
		ExhstQuoteDto request = new ExhstQuoteDto();
		request.setRqstEmail("test@example.com");
		request.setPhoneNo("010-1234-5678");
		request.setPostNo("06234");
		request.setLotAddr("서울시 강남구 역삼동 123-45");
		request.setRoadAddr("서울시 강남구 테헤란로 123");
		request.setSmplAddr("서울시 강남구");
		request.setExtraCmnt("기타 요청사항");

		// 폐기물 정보 추가
		ExhstQuoteWasteDto waste = new ExhstQuoteWasteDto();
		waste.setWsteNm("일반 폐기물");
		waste.setWsteCd("W001");
		waste.setExpctExhstAmt(BigDecimal.valueOf(100.0));
		request.setWsteList(List.of(waste));

		return request;
	}

	/**
	 * 유효한 이미지 파일 리스트 생성
	 */
	private List<MultipartFile> createValidImageFiles(int count) {
		List<MultipartFile> images = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			images.add(new MockMultipartFile(
					"file" + i,
					"test" + i + ".jpg",
					"image/jpeg",
					"test content".getBytes()
			));
		}
		return images;
	}

	@Test
	@DisplayName("ME0013 : 견적 상세 배출자 정보 업데이트 - 호출 횟수 + 모든 값 입력")
	void updateExhstInfoByQuoteIdSuccess001() {
		ManageQuoteExhstInfoUpdateDto request = ManageQuoteExhstInfoUpdateDto.builder()
				.quoteId("QID")
				.exhstNm("배출자")
				.exhstPlace("배출 사업장")
				.build();

		when(matchQuoteRepository.updateExhstInfoByQuoteId(anyMap())).thenReturn(1);

		int response = service.updateExhstInfoByQuoteId(request, 10L);

		assertEquals(1, response);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(matchQuoteRepository, times(1)).updateExhstInfoByQuoteId(captor.capture());

		Map<String, Object> param = captor.getValue();
		assertEquals("QID", param.get("quoteId"));
		assertEquals("배출자", param.get("exhstNm"));
		assertEquals("배출 사업장", param.get("exhstPlace"));
		assertEquals(10L, param.get("userSn"));

		verifyNoMoreInteractions(matchQuoteRepository);
	}

	@Test
	@DisplayName("ME0013 : 견적 상세 배출자 정보 업데이트 성공 - 호출 횟수 + 필수 값 제외 null or blank")
	void updateExhstInfoByQuoteIdSuccess002() {
		ManageQuoteExhstInfoUpdateDto request = ManageQuoteExhstInfoUpdateDto.builder()
				.quoteId("QID")
				.exhstNm(null)
				.exhstPlace("  ")
				.build();

		when(matchQuoteRepository.updateExhstInfoByQuoteId(anyMap())).thenReturn(1);

		int response = service.updateExhstInfoByQuoteId(request, 10L);

		assertEquals(1, response);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(matchQuoteRepository, times(1)).updateExhstInfoByQuoteId(captor.capture());

		Map<String, Object> param = captor.getValue();
		assertEquals("QID", param.get("quoteId"));
		assertEquals("", param.get("exhstNm"));
		assertEquals("", param.get("exhstPlace"));
		assertEquals(10L, param.get("userSn"));

		verifyNoMoreInteractions(matchQuoteRepository);
	}

	@Test
	@DisplayName("ME0013 : 0을 리턴하면 service도 0을 리턴")
	void updateExhstInfoByQuoteIdRepoRetrunZero() {
		ManageQuoteExhstInfoUpdateDto request = ManageQuoteExhstInfoUpdateDto.builder()
				.quoteId("QID")
				.exhstNm("배출자")
				.exhstPlace("배출 사업장")
				.build();

		when(matchQuoteRepository.updateExhstInfoByQuoteId(anyMap())).thenReturn(0);

		int response = service.updateExhstInfoByQuoteId(request, 10L);

		assertEquals(0, response);
		verify(matchQuoteRepository, times(1)).updateExhstInfoByQuoteId(anyMap());
	}

	@Test
	@DisplayName("ME0013 : 토큰 유저 정보가 없으면 usrSn=null로 전달")
	void updateExhstInfoByQuoteIdUserInfoNull() {
		ManageQuoteExhstInfoUpdateDto request = ManageQuoteExhstInfoUpdateDto.builder()
				.quoteId("QID")
				.exhstNm("배출자")
				.exhstPlace("배출 사업장")
				.build();

		when(matchQuoteRepository.updateExhstInfoByQuoteId(anyMap())).thenReturn(1);

		int response = service.updateExhstInfoByQuoteId(request, null);

		assertEquals(1, response);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(matchQuoteRepository).updateExhstInfoByQuoteId(captor.capture());

		assertNull(captor.getValue().get("usrSn"));
	}

	@Test
	@DisplayName("배출자 견적 요청 상세 조회 - 정상")
	void getExhstQuoteDetail_success() {
		ExhstQuoteDetailDto reqDto = new ExhstQuoteDetailDto();
		reqDto.setQuoteId("Q001");

		// 견적 기본 정보
		Map<String, Object> baseInfo = new HashMap<>();
		baseInfo.put("quoteId", "Q001");
		baseInfo.put("quoteSttsCd", "REQ_CMPL");
		baseInfo.put("codeNm", "요청 완료");
		baseInfo.put("quoteRqstDt", "2025-01-01");
		baseInfo.put("estimateSendDt", null);
		baseInfo.put("rqstEmail", "test@gmail.com");
		baseInfo.put("rqstPhoneNo", "010-1234-1234");
		baseInfo.put("postNo", "03143");
		baseInfo.put("roadAddr", "서울 종로구 율목로2길 19");
		baseInfo.put("extraCmnt", "배출자가 전달하는 추가사항입니다.");

		// 폐기물 리스트 (서비스 로직상 필터링 없음 → 3건 그대로 반환됨)
		List<Map<String, Object>> wasteList = List.of(
				Map.of("no", 1, "wsteNm", "폐합성수지류(51-03-01)", "expctExhstAmt", "10,000 ton"),
				Map.of("no", 2, "wsteNm", "오니류(51-01-01)", "expctExhstAmt", "5,000 ton"),
				Map.of("no", 3, "wasteNm", "플라스틱입니다...", "expctExhstAmt", "100 ton")
		);

		// 사진 파일 리스트
		List<Map<String, Object>> photoFiles = List.of(
				Map.of("fileSn", "10", "fileGrpId", "FG001", "fileOrignNm", "1.jpg", "filePath", "/upload/2025/01/", "fileSaveNm", "a1b2c3.jpg"),
				Map.of("fileSn", "11", "fileGrpId", "FG001", "fileOrignNm", "2.jpg", "filePath", "/upload/2025/01/", "fileSaveNm", "d4e5f6.jpg"),
				Map.of("fileSn", "12", "fileGrpId", "FG001", "fileOrignNm", "3.jpg", "filePath", "/upload/2025/01/", "fileSaveNm", "g7h8i9.jpg")
		);

		when(exhstQuoteRepository.getExhstQuoteBase(anyMap())).thenReturn(baseInfo);
		when(exhstQuoteRepository.getExhstQuoteWasteList(anyMap())).thenReturn(wasteList);
		when(exhstQuoteRepository.getExhstQuotePhotoList(anyMap())).thenReturn(photoFiles);

		Map<String, Object> result = service.getExhstQuoteDetail(reqDto);

		assertNotNull(result);
		assertEquals(reqDto.getQuoteId(), result.get("quoteId"));

		List<?> resultWasteList = (List<?>) result.get("wasteList");
		List<?> resultPhotoFiles = (List<?>) result.get("photoFiles");

		assertEquals(3, resultWasteList.size());
		assertEquals(3, resultPhotoFiles.size());

		Map<?, ?> firstWaste = (Map<?, ?>) resultWasteList.get(0);
		assertEquals(1, firstWaste.get("no"));
		assertEquals("폐합성수지류(51-03-01)", firstWaste.get("wsteNm"));

		Map<?, ?> thirdWaste = (Map<?, ?>) resultWasteList.get(2);
		assertEquals("플라스틱입니다...", thirdWaste.get("wasteNm"));

		Map<?, ?> firstPhoto = (Map<?, ?>) resultPhotoFiles.get(0);
		assertEquals("1.jpg", firstPhoto.get("fileOrignNm"));
	}

	/**
	 * 배출자 견적 요청 상세 조회 - 데이터 없음
	 */
	@Test
	@DisplayName("배출자 견적 요청 상세 조회 - 데이터 없음")
	void getExhstQuoteDetail_notFound() {
		ExhstQuoteDetailDto reqDto = new ExhstQuoteDetailDto();
		reqDto.setQuoteId("Q999");

		when(exhstQuoteRepository.getExhstQuoteBase(anyMap())).thenReturn(null);

		assertThrows(ValidationException.class, () -> service.getExhstQuoteDetail(reqDto));
	}

	// ==================== updateEstimateSendStatus 테스트 ====================
	@Test
	@DisplayName("견적 발송 상태 업데이트 - 정상 케이스 (이메일, SMS 발송 포함)")
	void updateEstimateSendStatusSuccess001() {
		// Given
		wayble.ade.match.dto.ManageQuoteEstimateSendDto request = wayble.ade.match.dto.ManageQuoteEstimateSendDto.builder()
				.quoteId("Q20250119123456")
				.estmtSendMsg("견적서가 발송되었습니다. 확인 부탁드립니다.")
				.build();
		Long usrSn = 100L;

		// Mock 폐기물 리스트
		List<Map<String, Object>> wasteList = new ArrayList<>();
		Map<String, Object> waste1 = new HashMap<>();
		waste1.put("wsteNm", "일반폐기물");
		waste1.put("expctExhstAmt", new BigDecimal("100"));
		wasteList.add(waste1);

		Map<String, Object> waste2 = new HashMap<>();
		waste2.put("wsteNm", "건설폐기물");
		waste2.put("expctExhstAmt", new BigDecimal("200"));
		wasteList.add(waste2);

		// Mock 견적 상세 정보
		Map<String, Object> quoteDetail = new HashMap<>();
		quoteDetail.put("quoteRqstDt", "2025-01-19");
		quoteDetail.put("rqstEmail", "test@example.com");
		quoteDetail.put("addr", "서울시 강남구");
		quoteDetail.put("rqstPhoneNo", "010-1234-5678");

		when(matchQuoteRepository.updateEstimateSendStatus(any())).thenReturn(1);
		when(matchQuoteWasteRepository.getQuoteWasteListById(argThat(map -> map != null && "Q20250119123456".equals(map.get("quoteId"))))).thenReturn(wasteList);
		when(matchQuoteRepository.getQuoteDetailById(argThat(map -> map != null && "Q20250119123456".equals(map.get("quoteId"))))).thenReturn(quoteDetail);

		// When
		int result = service.updateEstimateSendStatus(request, usrSn);

		// Then
		assertEquals(1, result);

		// Repository 호출 검증
		ArgumentCaptor<Map<String, Object>> repoCaptor = ArgumentCaptor.forClass(Map.class);
		verify(matchQuoteRepository, times(1)).updateEstimateSendStatus(repoCaptor.capture());

		Map<String, Object> param = repoCaptor.getValue();
		assertEquals("Q20250119123456", param.get("quoteId"));
		assertEquals("300", param.get("quoteSttsCd"));
		assertEquals("300", param.get("estmtSttsCd"));
		assertEquals("견적서가 발송되었습니다. 확인 부탁드립니다.", param.get("estmtSendMsg"));
		assertEquals(100L, param.get("userSn"));

		// 이메일 발송 검증
		ArgumentCaptor<Map> emailCaptor = ArgumentCaptor.forClass(Map.class);
		verify(emailService, times(1)).send(eq(wayble.ade.common.data.type.EmailTemplateType.NOTI202), emailCaptor.capture());

		Map<String, Object> emailData = emailCaptor.getValue();
		assertEquals("test@example.com", emailData.get("rcvAddr"));
		assertEquals("서울시 강남구", emailData.get("roadAddr"));
		assertEquals("2025-01-19", emailData.get("quoteRqstDt"));
		assertEquals("2025-02-18", emailData.get("quoteEndDt")); // 30일 후
		assertEquals("Q20250119123456", emailData.get("quoteId"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> wasteItems = (List<Map<String, Object>>) emailData.get("wasteItems");
		assertEquals(2, wasteItems.size());
		assertEquals("일반폐기물", wasteItems.get(0).get("itemName"));
		assertEquals(new BigDecimal("100"), wasteItems.get(0).get("monthlyAmount"));

		// SMS 발송 검증
		ArgumentCaptor<Map> smsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(smsService, times(1)).send(smsCaptor.capture());

		Map<String, Object> smsData = smsCaptor.getValue();
		assertEquals("010-1234-5678", smsData.get("rcvAddr"));
		assertTrue(((String) smsData.get("message")).contains("폐기물 처리 견적이 제안 되었습니다"));
		assertTrue(((String) smsData.get("message")).contains("Q20250119123456"));
		assertEquals("[WAYBLE] 폐기물 처리 견적 제안 완료", smsData.get("subject"));
	}

	@Test
	@DisplayName("견적 발송 상태 업데이트 - quoteRqstDt가 null인 경우")
	void updateEstimateSendStatusWithNullQuoteRqstDt() {
		// Given
		wayble.ade.match.dto.ManageQuoteEstimateSendDto request = wayble.ade.match.dto.ManageQuoteEstimateSendDto.builder()
				.quoteId("Q20250119999999")
				.estmtSendMsg("견적 발송")
				.build();
		Long usrSn = 200L;

		// Mock 빈 폐기물 리스트
		List<Map<String, Object>> emptyWasteList = new ArrayList<>();

		// Mock 견적 상세 정보 (quoteRqstDt가 null)
		Map<String, Object> quoteDetail = new HashMap<>();
		quoteDetail.put("quoteRqstDt", null);
		quoteDetail.put("rqstEmail", "test2@example.com");
		quoteDetail.put("addr", "서울시 서초구");
		quoteDetail.put("rqstPhoneNo", "010-9999-8888");

		when(matchQuoteRepository.updateEstimateSendStatus(any())).thenReturn(1);
		when(matchQuoteWasteRepository.getQuoteWasteListById(argThat(map -> map != null && "Q20250119999999".equals(map.get("quoteId"))))).thenReturn(emptyWasteList);
		when(matchQuoteRepository.getQuoteDetailById(argThat(map -> map != null && "Q20250119999999".equals(map.get("quoteId"))))).thenReturn(quoteDetail);

		// When
		int result = service.updateEstimateSendStatus(request, usrSn);

		// Then
		assertEquals(1, result);

		// 이메일 발송 검증 - quoteEndDt가 null이어야 함
		ArgumentCaptor<Map> emailCaptor = ArgumentCaptor.forClass(Map.class);
		verify(emailService, times(1)).send(eq(wayble.ade.common.data.type.EmailTemplateType.NOTI202), emailCaptor.capture());

		Map<String, Object> emailData = emailCaptor.getValue();
		assertNull(emailData.get("quoteRqstDt"));
		assertNull(emailData.get("quoteEndDt"));

		// SMS 발송도 실행되어야 함
		verify(smsService, times(1)).send(any());
	}

	@Test
	@DisplayName("견적서 조회 - 이메일 링크 인증 성공 (전화번호 뒷자리 일치)")
	public void getEstimateByEmailLinkTest(){
		ExhstEstimateByEmailLinkDto request = ExhstEstimateByEmailLinkDto.builder()
				.quoteId("Q251201125959999")
				.phoneNoLast4("1234").build();

		// Repository Mocking
		when(matchQuoteRepository.selectExistsQuoteByPhoneNoLast4(anyMap())).thenReturn(1);

		// when
		Map<String, Object> result = service.getEstimateByEmailLink(request);

		assertEquals(result, Map.of());
	}

	@Test
	@DisplayName("견적서 조회 - 이메일 링크 인증 실패 (전화번호 뒷자리 불일치)")
	public void getEstimateByEmailLinkTest001(){
		ExhstEstimateByEmailLinkDto request = ExhstEstimateByEmailLinkDto.builder()
				.quoteId("Q251201125959999")
				.phoneNoLast4("1234").build();

		// Repository Mocking
		when(matchQuoteRepository.selectExistsQuoteByPhoneNoLast4(anyMap())).thenReturn(0);

		// when
		CommonException e = assertThrows(CommonException.class, () -> {
			service.getEstimateByEmailLink(request);
		});

		// then
		assertEquals(ApiResponseCodeType.BIZ001, e.getCode());
	}
}
