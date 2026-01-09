package wayble.ade.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import wayble.ade.common.dto.AuthQuoteDto;
import wayble.ade.auth.service.AuthService;
import wayble.ade.common.database.mdb.MemoryDB;
import wayble.ade.common.exception.ConnectionException;
import wayble.ade.common.exception.ValidationException;
import wayble.ade.common.repository.UserRepository;
import wayble.ade.common.service.SmsService;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static wayble.ade.common.constants.AuthConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private final MemoryDB memoryDB;
	private final UserRepository userRepository;
	private final SmsService smsService;

	@Override
	public Map<String, Object> createAuthCodeByEmail(AuthQuoteDto dto) {
		/* 1. 이메일 검증 */
		String email = dto.getUserEmail();
		if (email == null || email.isBlank()) {
			throw new ValidationException("사용자 이메일이 없습니다.");
		}

		/* 2. 이메일 → 전화번호 조회 */
		String phoneNumber = userRepository.findTelByEmail(email);
		if (phoneNumber == null || phoneNumber.isBlank()) {
			throw new ValidationException("등록된 전화번호가 없습니다.");
		}

		long now = System.currentTimeMillis();

		//MemoryDB Key 변수화
		String codeKey = AUTH_CODE_PREFIX + phoneNumber;
		String sentKey = AUTH_SENT_PREFIX + phoneNumber;
		String failKey = AUTH_FAIL_PREFIX + phoneNumber;

		try {
			/* 3. 재발송 제한 체크 */
			String lastSent = memoryDB.getString(sentKey);
			if (lastSent != null) {
				long diffSec = (now - Long.parseLong(lastSent)) / 1000;
				if (diffSec < RESEND_LIMIT_SECONDS) {
					throw new ValidationException("인증번호 발송 후 1분이 지나지 않았습니다. " + "1분이 지난 후 재발송 해주십시오. (현재 " + diffSec + "초 경과)");
				}
			}

			/* 4. 인증번호 생성 */
			String authCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

			memoryDB.setString(codeKey, authCode, AUTH_CODE_EXPIRE_SECONDS);
			memoryDB.setString(sentKey, String.valueOf(now), RESEND_LIMIT_SECONDS);
			memoryDB.setString(failKey, "0", AUTH_CODE_EXPIRE_SECONDS);

			/* 5. 문자 발송 */
			Map<String, Object> smsData = new HashMap<>();
			smsData.put("rcvAddr", phoneNumber);
			smsData.put("message", "[인증번호] " + authCode);
			long result = smsService.send(smsData);
			if (result <= 0) {
				log.error("SMS 발송 실패 (Error Code: {})", result);
				rollbackKeys(codeKey, sentKey, failKey);
				throw new ValidationException("인증번호 발송에 실패했습니다.");
			}

		} catch (ConnectionException e) {
			log.error("MemoryDB error - phoneNumber={}", phoneNumber, e);
			// 정책상 ValidationException으로 통일
			throw new ValidationException("인증번호 처리 중 시스템 오류가 발생했습니다.");
		}

		/* 7. 응답 */
		Map<String, Object> result = new HashMap<>();
		result.put("expireSeconds", AUTH_CODE_EXPIRE_SECONDS);
		return result;
	}
	private void rollbackKeys(String codeKey, String sentKey, String failKey) {
		try {
			memoryDB.setString(codeKey, "", 1);
			memoryDB.setString(sentKey, "", 1);
			memoryDB.setString(failKey, "", 1);
		} catch (Exception e) {
			log.warn("MemoryDB rollback failed", e);
		}
	}
}