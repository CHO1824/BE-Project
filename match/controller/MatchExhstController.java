package wayble.ade.match.controller;

/*
 * Copyright 2025 SK ecoplant.
 * All rights reserved.
 * Created by MindAll.
 */

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wayble.ade.common.data.response.ApiResponse;
import wayble.ade.common.data.type.ApiResponseCodeType;
import wayble.ade.common.dto.UserPrincipalDto;
import wayble.ade.common.util.TokenUtil;
import wayble.ade.match.dto.ExhstEstimateByEmailLinkDto;
import wayble.ade.match.dto.ExhstEstimateByRequestDetailDto;
import wayble.ade.match.dto.request.ExhstQuoteDto;
import wayble.ade.match.dto.request.ExhstQuoteDetailDto;
import wayble.ade.match.service.MatchProposalService;
import wayble.ade.match.service.MatchQuoteService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exhst")
@RequiredArgsConstructor
public class MatchExhstController {
	private static final Logger log = LoggerFactory.getLogger(MatchExhstController.class);

	private final MatchQuoteService quoteService;
	private final MatchProposalService matchProposalService;

	/**
	 * 견적 요청 등록 API (RQ0001)
	 * @param userAgent 유저 에이전트
	 * @param request 견적 요청 DTO
	 * @param images 이미지 파일 (최대 3장, 선택)
	 * @return API 결과 ApiResponse
	 */
	@PostMapping(path = "/RQ0001", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResponse createQuote(
			@RequestHeader("User-Agent") String userAgent,
			@Valid @RequestPart("body") ExhstQuoteDto request,
			@RequestPart(value = "images", required = false) List<MultipartFile> images) {
		log.info("견적 요청 등록 API 호출 - 이미지 개수: {}", images != null ? images.size() : 0);

		// SecurityContext에서 인증된 사용자 정보 추출
		UserPrincipalDto userInfo = TokenUtil.getUserInfo();
		Long userId = userInfo != null ? userInfo.getUserSn() : null;

		Map<String, Object> result = quoteService.createQuote(request, images, userAgent, userId);

		return ApiResponse.ofSuccess(ApiResponseCodeType.COM000, result);
	}

	/**
	 * 배출자 견적 상세 조회
	 * @param reqDto 견적 ID를 포함한 요청 DTO
	 * @return 견적 상세 정보
	 */
	@GetMapping(path = "/RQ0003")
	public ApiResponse getExhstQuoteDetail(@Valid ExhstQuoteDetailDto reqDto) {
		return ApiResponse.ofSuccess(ApiResponseCodeType.COM000, quoteService.getExhstQuoteDetail(reqDto));
	}

	/**
	 * 견적서 조회 - 이메일 링크
	 * @param request 요청 DTO
	 * @return API 결과 ApiResponse
	 */
	@PostMapping(path = "/RQ0004")
	public ApiResponse getEstimateByEmailLink(@Valid @RequestBody ExhstEstimateByEmailLinkDto request) {
		// TODO : 요청 견적서의 요청 전화번호 뒤 4자리 체크 후 요청 견적서 조회
		return ApiResponse.ofSuccess(ApiResponseCodeType.COM000, null);
	}

	/**
	 * 견적서 조회 - 요청내역
	 * @param request 요청 DTO
	 * @return API 결과 ApiResponse
	 */
	@GetMapping(path = "/RQ0005")
	public ApiResponse getEstimateByRequestDetail(@Valid ExhstEstimateByRequestDetailDto request) {
		return ApiResponse.ofSuccess(ApiResponseCodeType.COM000, matchProposalService.getEstimateProposal(request.getQuoteId()));
	}
}
