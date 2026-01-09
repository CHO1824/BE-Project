package wayble.ade.common.repository;

/*
 * Copyright 2025 SK ecoplant.
 * All rights reserved.
 * Created by MindAll.
 */

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;
import java.util.List;

/**
 * 견적 요청 Repository
 */
@Mapper
public interface ExhstQuoteRepository {
	/**
	* 배출자 견적 요청 목록 조회
	* @param param 조회 조건
	* @return 견적 요청 목록
	*/
	List<Map<String, Object>> getExhstQuoteList(Map<String, Object> param);

	/**
	 * 배출자 견적 요청 상세 조회
	 * @param param 조회 조건 (quoteId)
	 * @return 배출자 견적 요청 상세 정보
	 */
	/** 견적 기본 정보 */
	Map<String, Object> getExhstQuoteBase(Map<String, Object> param);

	/** 폐기물 리스트 */
	List<Map<String, Object>> getExhstQuoteWasteList(Map<String, Object> param);

	/** 사진 파일 리스트 */
	List<Map<String, Object>> getExhstQuotePhotoList(Map<String, Object> param);

	/**
	 * 견적 요청 기본 정보 저장
	 * @param params 견적 요청 정보 (quoteId, request 정보 포함)
	 * @return 삽입된 행 수
	 */
	int createQuote(Map<String, Object> params);

	/**
	 * 견적 요청 폐기물 정보 저장 (Bulk Insert)
	 * @param params 폐기물 품목 리스트 정보 (quoteId, wasteList, userSn 포함)
	 * @return 삽입된 행 수
	 */
	int createQuoteWaste(Map<String, Object> params);
}
