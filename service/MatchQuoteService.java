package wayble.ade.match.service;

import wayble.ade.match.dto.*;
import org.springframework.web.multipart.MultipartFile;
import wayble.ade.match.dto.request.ExhstQuoteDto;
import wayble.ade.match.dto.request.ExhstQuoteDetailDto;

import java.util.List;
import java.util.Map;

public interface MatchQuoteService {
	Map<String, Object> getQuoteList(ManageQuoteListDto request);
	Map<String, Object> getQuoteDetail(ManageQuoteDetailDto request);
	int updateExhstInfoByQuoteId(ManageQuoteExhstInfoUpdateDto request, Long userSn);
	int updateEstimateSendStatus(ManageQuoteEstimateSendDto request, Long userSn);

	/**
	 * 견적 요청 등록 (RQ0001)
	 * @param request 견적 요청 DTO
	 * @param images 이미지 파일 리스트 (최대 3장, 선택)
	 * @param userAgent 유저 에이전트
	 * @param userId 유저 아이디 (관리자 신청일 경우 존재)
	 * @return 처리 결과 (견적 ID 포함)
	 */
	Map<String, Object> createQuote(ExhstQuoteDto request, List<MultipartFile> images, String userAgent, Long userId);

	/**
	 * 배출자 견적 상세 조회
	 * @param reqDto 견적 ID를 포함한 요청 DTO
	 * @return 견적 상세 정보
	 */
	Map<String, Object> getExhstQuoteDetail(ExhstQuoteDetailDto reqDto);
	Map<String, Object> getEstimateByEmailLink(ExhstEstimateByEmailLinkDto request);
}
