package wayble.ade.auth.service;

import wayble.ade.common.dto.AuthQuoteDto;

import java.util.Map;

public interface AuthService {
	/**
	 * 이메일 기반 인증번호 발송/재발송
	 */
	Map<String, Object> createAuthCodeByEmail(AuthQuoteDto dto);
}