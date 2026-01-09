package wayble.ade.match.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import wayble.ade.common.code.EstmtSttsCdType;
import wayble.ade.common.code.QuoteSttsCdType;
import wayble.ade.common.data.type.ApiResponseCodeType;
import wayble.ade.common.data.type.EmailTemplateType;
import wayble.ade.common.exception.CommonException;
import wayble.ade.common.exception.ValidationException;
import wayble.ade.common.repository.*;
import wayble.ade.common.service.EmailService;
import wayble.ade.common.service.FileService;
import wayble.ade.common.service.SmsService;
import wayble.ade.common.util.StringUtil;
import wayble.ade.match.dto.*;
import wayble.ade.common.util.UniqueIdUtil;
import wayble.ade.match.dto.request.ExhstQuoteDto;
import wayble.ade.match.dto.request.ExhstQuoteWasteDto;
import wayble.ade.match.dto.request.ExhstQuoteDetailDto;
import wayble.ade.match.service.MatchQuoteService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchQuoteServiceImpl implements MatchQuoteService {
	private static final Logger log = LoggerFactory.getLogger(MatchQuoteServiceImpl.class);
	private static final String DEFAULT_SORT = "crtDt";

	private final FileService fileService;
	private final EmailService emailService;
	private final SmsService smsService;

	private final MatchQuoteRepository matchQuoteRepository;
	private final MatchEstimateRepository matchEstimateRepository;
	private final MatchQuoteWasteRepository matchQuoteWasteRepository;
	private final ExhstQuoteRepository exhstQuoteRepository;
	private final UserRepository userRepository;

	/**
	 * 견적 관리 리스트
	 * @param request
	 * @return
	 */
	@Override
	public Map<String, Object> getQuoteList(ManageQuoteListDto request) {
		String exhaustName = StringUtil.nullIfBlank(request.getExhaustName());
		String status = StringUtil.nullIfBlank(request.getStatus());
		String sort = StringUtil.defaultIfBlank(request.getSort(), DEFAULT_SORT);

		Map<String, Object> queryParam = new HashMap<>();
		queryParam.put("startDate", request.getStartDate());
		queryParam.put("endDate", request.getEndDate());
		queryParam.put("exhaustName", exhaustName);
		queryParam.put("status", status);
		queryParam.put("sort", sort);

		long totalRows = matchQuoteRepository.countQuoteList(queryParam);
		PaginationDto pagination = PaginationDto.of(request.getPage(), request.getRowsPerPage(), totalRows);

		List<Map<String, Object>> quoteList;
		if (totalRows > 0L) {
			int offset = pagination.getOffset();
			queryParam.put("offset", offset);
			queryParam.put("rowsPerPage", pagination.getRowsPerPage());

			quoteList = matchQuoteRepository.getQuoteList(queryParam);
			// quoteList 의 id 값 list로 추출
			List<String> quoteIdList = quoteList.stream()
					.map(q -> (String) q.get("quoteId"))
					.filter(Objects::nonNull)
					.toList();

			List<Map<String, Object>> estimatePartnerList;
			if (quoteIdList.isEmpty()) {
				estimatePartnerList = Collections.emptyList();
			} else {
				Map<String, Object> estimatePartnerQueryParam = new HashMap<>();
				estimatePartnerQueryParam.put("quoteIdList", quoteIdList);

				estimatePartnerList = matchEstimateRepository.getEstimateSummaryListByQuoteId(estimatePartnerQueryParam);
			}

			// estimatePartnerList 의 값을 quoteId 기준으로 toMap 처리
			Map<String, List<Map<String, Object>>> estimatePartnerListByQuoteId = estimatePartnerList.stream()
					.collect(Collectors.groupingBy(
							p -> (String) p.get("quoteId"),
							Collectors.mapping(p -> {
								Map<String, Object> copy = new HashMap<>(p);
								copy.remove("quoteId");
								return copy;
							}, Collectors.toList())
					));


			// estimatePartnerList 데이터 response 용 map 에 set 처리
			for (Map<String, Object> quote : quoteList) {
				String quoteId = (String) quote.get("quoteId");
				List<Map<String, Object>> partners = estimatePartnerListByQuoteId.getOrDefault(quoteId, Collections.emptyList());

				quote.put("estimatePartnerList", partners);
			}
		} else {
			quoteList = Collections.emptyList();
		}

		Map<String, Object> result = new HashMap<>();
		result.put("quoteList", quoteList);
		result.put("paging", pagination);

		return result;
	}

	/**
	 * 견적 상세 조회
	 * @param request
	 * @return
	 */
	@Override
	public Map<String, Object> getQuoteDetail(ManageQuoteDetailDto request) {
		String quoteId = StringUtil.nullIfBlank(request.getQuoteId());

		Map<String, Object> quoteDetail = getQuoteDetailById(quoteId);
		if (quoteDetail == null || quoteDetail.isEmpty()) {
			throw new ValidationException("ERR008");
		}

		String fileGrpId = (String) quoteDetail.get("attachFile");

		List<Map<String, Object>> fileList = Collections.emptyList();
		if (StringUtil.isNotEmpty(fileGrpId)) {
			fileList = fileService.getS3FileAccessInfo(fileGrpId);
		}

		List<Map<String, Object>> wasteList = getQuoteWasteListById(quoteId);

		Map<String, Object> quoteDetailForResponse = new HashMap<>(quoteDetail);
		quoteDetailForResponse.remove("attachFile");

		Map<String, Object> result = new HashMap<>();
		result.put("quoteDetail", quoteDetailForResponse);
		result.put("quoteFileList", fileList);
		result.put("wasteList", wasteList);

		return result;
	}

	/**
	 * 배출자 견적 상세 조회
	 */
	@Override
	public Map<String, Object> getExhstQuoteDetail(ExhstQuoteDetailDto reqDto) {
		Map<String, Object> param = Map.of("quoteId", reqDto.getQuoteId());

		// 기본 정보
		Map<String, Object> base = exhstQuoteRepository.getExhstQuoteBase(param);
		if (base == null || base.isEmpty()) {
			throw new ValidationException("견적 상세 정보가 존재하지 않습니다.");
		}

		// 폐기물
		List<Map<String, Object>> wasteList = exhstQuoteRepository.getExhstQuoteWasteList(param);

		// 첨부파일
		List<Map<String, Object>> photoFiles = exhstQuoteRepository.getExhstQuotePhotoList(param);

		base.put("wasteList", wasteList);
		base.put("photoFiles", photoFiles);

		return base;
	}

	/**
	 * 견적 요청 등록 (RQ0001)
	 * @param request 견적 요청 데이터 Dto
	 * @param images 이미지 파일 리스트 (최대 3장, 선택)
	 * @param userAgent 유저 에이전트
	 * @param userId 견적 요청 DTO
	 * @return 처리 결과 (견적 ID 포함)
	 */
	@Override
	public Map<String, Object> createQuote(ExhstQuoteDto request, List<MultipartFile> images, String userAgent, Long userId) {
		// TODO: log는 추후 테스트 완료후 삭제예정입니다
		log.info("견적 요청 등록 처리 시작 - 이미지 개수: {}", images != null ? images.size() : 0);

		// 1. 이미지 파일 유효성 검증 (DTO validation으로 처리 불가능한 부분)
		validateImages(images);

		// 2. 견적 ID 생성
		String quoteId = UniqueIdUtil.generateQuoteId();
		log.info("견적 ID 생성 완료 - quoteId: {}", quoteId);

		// 3. 이미지 파일 업로드 처리 (S3)
		String fileGrpId = null;
		if (images != null && !images.isEmpty()) {
			log.info("이미지 파일 업로드 시작 - 개수: {}", images.size());
			Map<String, Object> fileInfo = fileService.uploadFileList(images, "uploads", null, userId);
			fileGrpId = fileInfo.get("fileGrpId").toString();
		}

		saveQuote(request, quoteId, fileGrpId, userId, userAgent);

		// NOTI001 Email 발송 - 배출자 1명
		Map<String, Object> emailNoti001Data = new HashMap<>();
		emailNoti001Data.put("rcvAddr", request.getRqstEmail());
		emailNoti001Data.put("roadAddr", request.getRoadAddr());
		emailNoti001Data.put("phoneNo", request.getPhoneNo());
		emailNoti001Data.put("rqstEmail", request.getRqstEmail());
		emailNoti001Data.put("etcCmnt", request.getExtraCmnt());

		emailService.send(EmailTemplateType.NOTI001, emailNoti001Data);

		// NOTI002 SMS 발송 - 배출자 1명
		Map<String, Object> smsData = new HashMap<>();
		smsData.put("rcvAddr", request.getPhoneNo());
		smsData.put("message", "폐기물 처리 견적 요청이 접수 되었습니다.\n" +
				"\n" +
				"웨이블이 직접 검증한 전국 파트너스의 견적을 종합 검토하여, 가장 합리적인 견적을 제안 드립니다.\n" +
				"\n" +
				"영업일 기준 5일 이내 답변 드리겠습니다.");
		smsData.put("subject", "[WAYBLE] 폐기물 처리 견적 신청 안내");

		smsService.send(smsData);

		// NOTI003 Email 발송 - 플랫폼 관리자 n명

		// 필요한 데이터 조회
		List<Map<String, Object>> wasteItems = new ArrayList<>();
		List<ExhstQuoteWasteDto> wasteList = request.getWsteList();

		// wasteItems 데이터 세팅
		for (ExhstQuoteWasteDto map : wasteList) {
			Map<String, Object> newMap = new HashMap<>();
			newMap.put("itemName", map.getWsteNm());
			newMap.put("monthlyAmount", map.getExpctExhstAmt());

			wasteItems.add(newMap);
		}

		List<Map<String, Object>> paList = userRepository.getPlatformAdministrator();
		List<String> recipients =  new ArrayList<>();
		// recipients 데이터 세팅
		for (Map<String, Object> map : paList) {
			recipients.add(map.get("email").toString());
		}

		Map<String, Object> emailNoti003Data = new HashMap<>();
		emailNoti003Data.put("roadAddr", request.getRoadAddr());
		emailNoti003Data.put("phoneNo", request.getPhoneNo());
		emailNoti003Data.put("rqstEmail", request.getRqstEmail());
		emailNoti003Data.put("etcCmnt", request.getExtraCmnt());
		emailNoti003Data.put("quoteId", quoteId);
		emailNoti003Data.put("wasteItems", wasteItems);

		emailService.send(EmailTemplateType.NOTI003, recipients, emailNoti003Data);

		Map<String, Object> result = new HashMap<>();
		result.put("quoteId", quoteId);

		log.info("견적 요청 등록 완료 - quoteId: {}", quoteId);

		return result;
	}

	/**
	 * 견적 상세 기본 정보 수정 (배출자 정보)
	 * @param request
	 * @return
	 */
	@Override
	public int updateExhstInfoByQuoteId(ManageQuoteExhstInfoUpdateDto request, Long userSn) {
		Map<String, Object> queryParam = new HashMap<>();
		queryParam.put("quoteId", request.getQuoteId());
		queryParam.put("exhstNm", StringUtil.defaultIfBlank(request.getExhstNm(), ""));
		queryParam.put("exhstPlace", StringUtil.defaultIfBlank(request.getExhstPlace(), ""));
		queryParam.put("userSn", userSn);

		return matchQuoteRepository.updateExhstInfoByQuoteId(queryParam);
	}

	/**
	 * 견적 발송 상태 업데이트
	 * @param request
	 * @return
	 */
	@Override
	public int updateEstimateSendStatus(ManageQuoteEstimateSendDto request, Long usrSn) {
		log.debug("MatchQuoteServiceImpl.updateEstimateSendStatus -> request: {}, usrSn: {}", request, usrSn);

		String quoteId = request.getQuoteId();

		Map<String, Object> queryParam = new HashMap<>();
		queryParam.put("quoteId", quoteId);
		queryParam.put("quoteSttsCd", QuoteSttsCdType.SEND_ESTIMATE.getCode());
		queryParam.put("estmtSttsCd", EstmtSttsCdType.SEND_ESTIMATE.getCode());
		queryParam.put("estmtSendMsg", request.getEstmtSendMsg());
		queryParam.put("userSn", usrSn);

		int updatedCount = matchQuoteRepository.updateEstimateSendStatus(queryParam);
		log.debug("MatchQuoteServiceImpl.updateEstimateSendStatus -> updatedCount: {}", updatedCount);
		
		// 필요한 데이터 조회
		List<Map<String, Object>> wasteItems = new ArrayList<>();
		List<Map<String, Object>> wasteList = getQuoteWasteListById(quoteId);
		Map<String, Object> quoteData = getQuoteDetailById(quoteId);

		// wasteItems 데이터 세팅
		for (Map<String, Object> map : wasteList) {
			Map<String, Object> newMap = new HashMap<>();
			newMap.put("itemName", map.get("wsteNm"));
			newMap.put("monthlyAmount", map.get("expctExhstAmt"));

			wasteItems.add(newMap);
		}

		Object quoteRqstDt = quoteData.get("quoteRqstDt");
		String quoteEndDt = null;

		if (quoteRqstDt != null) {
			LocalDate plus30 = LocalDate.parse(quoteRqstDt.toString()).plusDays(30);
			quoteEndDt = plus30.toString();
		}

		Map<String, Object> emailData = new HashMap<>();
		emailData.put("rcvAddr", quoteData.get("rqstEmail"));
		emailData.put("roadAddr", quoteData.get("addr"));
		emailData.put("quoteRqstDt", quoteRqstDt);
		emailData.put("quoteEndDt", quoteEndDt);
		emailData.put("quoteId", quoteId);
		emailData.put("wasteItems", wasteItems);

		emailService.send(EmailTemplateType.NOTI202, emailData);

		Map<String, Object> smsData = new HashMap<>();
		smsData.put("rcvAddr", quoteData.get("rqstPhoneNo"));
		smsData.put("message", "폐기물 처리 견적이 제안 되었습니다.\n" +
				"\n" +
				" \n" +
				"\n" +
				"해당 견적은 가견적으로, 현장 실사 후 견적이 확정됩니다\n" +
				"\n" +
				"아래 견적서 확인 후 현장 실사 진행 여부를 확인해 주세요.\n" +
				"\n" +
				" \n" +
				"\n" +
				"견적서 확인: https://www.wayble.eco/matching/request/AM_EA13/" + quoteId);
		smsData.put("subject", "[WAYBLE] 폐기물 처리 견적 제안 완료");

		smsService.send(smsData);

		return updatedCount;
	}

	/**
	 * 견적서 조회 - 이메일 링크
	 * @param request 요청 DTO
	 * @return 견적서
	 */
	@Override
	public Map<String, Object> getEstimateByEmailLink(ExhstEstimateByEmailLinkDto request) {
		// query param 생성
		Map<String,Object> queryParam = new HashMap<>();
		queryParam.put("quoteId", request.getQuoteId());
		queryParam.put("phoneNoLast4", request.getPhoneNoLast4());

		// 요청 견적서 전화번호 뒤 4자리 확인
		if (matchQuoteRepository.selectExistsQuoteByPhoneNoLast4(queryParam) != 1) {
			throw new CommonException(ApiResponseCodeType.BIZ001);
		}

		// TODO: 견적서 조회

		return Map.of();
	}

	/**
	 * 견적 요청 DB 저장
	 * @param request 견적 요청 DTO
	 * @param quoteId 견적 ID
	 * @param fileGrpId fileGroupId
	 * @param userId 유저 아이디 (관리자 신청일 경우 존재)
	 * @param userAgent 유저 에이전트
	 */
	private void saveQuote(ExhstQuoteDto request, String quoteId, String fileGrpId, Long userId, String userAgent) {
		log.info("견적 요청 DB 저장 시작 - quoteId: {}", quoteId);

		// 1. 견적 요청 기본 정보 저장
		Map<String, Object> quoteParams = new HashMap<>();
		quoteParams.put("quoteId", quoteId);
		quoteParams.put("postNo", request.getPostNo());
		quoteParams.put("lotAddr", request.getLotAddr());
		quoteParams.put("roadAddr", request.getRoadAddr());
		quoteParams.put("simpleAddr", request.getSmplAddr());
		quoteParams.put("rqstEmail", request.getRqstEmail());
		quoteParams.put("phoneNo", request.getPhoneNo());
		quoteParams.put("extraCmnt", request.getExtraCmnt());
		quoteParams.put("fileGrpId", fileGrpId);
		quoteParams.put("quoteSttsCd", QuoteSttsCdType.REQUEST_QUOTE.getCode());
		quoteParams.put("estmtSttsCd", EstmtSttsCdType.REQUEST_QUOTE.getCode());
		quoteParams.put("userSn", userId);
		quoteParams.put("userAgnt", userAgent);

		int insertedQuote = matchQuoteRepository.insertQuote(quoteParams);
		log.info("견적 요청 기본 정보 저장 완료 - 저장 건수: {}", insertedQuote);

		// 2. 폐기물 품목 리스트 일괄 저장 (Bulk Insert)
		List<ExhstQuoteWasteDto> wsteList = request.getWsteList();
		Map<String, Object> wasteParams = new HashMap<>();
		wasteParams.put("quoteId", quoteId);
		wasteParams.put("wasteList", wsteList);
		wasteParams.put("userSn", userId);

		int insertedWastes = matchQuoteWasteRepository.insertQuoteWaste(wasteParams);
		log.info("폐기물 품목 일괄 저장 완료 - 저장 건수: {}", insertedWastes);

		log.info("견적 요청 DB 저장 완료 - quoteId: {}", quoteId);
	}

	/**
	 * 이미지 파일 유효성 검증
	 * @param images 이미지 파일 리스트
	 * @throws ValidationException 이미지 파일 검증 실패 시
	 */
	private void validateImages(List<MultipartFile> images) {
		if (images == null || images.isEmpty()) {
			return; // 이미지는 선택사항
		}

		// 파일 개수 검증 (최대 3개)
		if (images.size() > 3) {
			throw new ValidationException("이미지는 최대 3개까지 업로드 가능합니다.");
		}

		// 각 파일 검증
		for (MultipartFile image : images) {
			validateImageFile(image);
		}
	}

	/**
	 * 개별 이미지 파일 유효성 검증
	 * @param image 이미지 파일
	 * @throws ValidationException 이미지 파일 검증 실패 시
	 */
	private void validateImageFile(MultipartFile image) {
		// 빈 파일 체크
		if (image.isEmpty()) {
			throw new ValidationException("빈 이미지 파일은 업로드할 수 없습니다.");
		}

		// 파일 크기 체크 (10MB 제한)
		long maxSize = 10 * 1024 * 1024; // 10MB
		if (image.getSize() > maxSize) {
			throw new ValidationException("이미지 파일 크기는 10MB를 초과할 수 없습니다.");
		}

		// 파일 확장자 체크
		String originalFilename = image.getOriginalFilename();
		if (StringUtil.isBlank(originalFilename)) {
			throw new ValidationException("이미지 파일명이 올바르지 않습니다.");
		}

		String extension = fileService.getFileExtension(originalFilename);
		List<String> allowedExtensions = List.of("jpg", "jpeg", "png");

		if (!allowedExtensions.contains(extension)) {
			throw new ValidationException("이미지 파일은 jpg, jpeg, png 형식만 업로드 가능합니다.");
		}

		// Content-Type 체크
		String contentType = image.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new ValidationException("이미지 파일만 업로드 가능합니다.");
		}
	}

	private Map<String, Object> getQuoteDetailById(String quoteId) {
		Map<String, Object> queryParam = new HashMap<>();
		queryParam.put("quoteId", quoteId);

		return matchQuoteRepository.getQuoteDetailById(queryParam);
	}

	private List<Map<String, Object>> getQuoteWasteListById(String quoteId) {
		Map<String, Object> queryParam = new HashMap<>();
		queryParam.put("quoteId", quoteId);

		return matchQuoteWasteRepository.getQuoteWasteListById(queryParam);
	}
}
