package wayble.ade.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import wayble.ade.auth.service.AuthService;
import wayble.ade.common.dto.AuthQuoteDto;
import wayble.ade.common.exception.ExceptionAdvice;
import wayble.ade.common.exception.ValidationException;
import wayble.ade.common.util.MessageUtil;
import wayble.ade.common.util.TokenUtil;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@Mock
	private AuthService authService;

	@Mock
	private MessageSourceAccessor messageSourceAccessor;

	@InjectMocks
	private AuthController authController;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();

		Mockito.lenient().doAnswer(invocation -> invocation.getArgument(0))
				.when(messageSourceAccessor).getMessage(anyString(), anyString());
		Mockito.lenient().doAnswer(invocation -> invocation.getArgument(0))
				.when(messageSourceAccessor).getMessage(anyString(), any(), anyString());
		MessageUtil.setMessageSourceAccessor(messageSourceAccessor);
		mockMvc = MockMvcBuilders.standaloneSetup(authController).setControllerAdvice(new ExceptionAdvice()).build();
	}

	@Test
	@DisplayName("[성공] 인증번호 발송 성공")
	void createAuthCodeSuccess() throws Exception {
		AuthQuoteDto reqDto = new AuthQuoteDto();
		reqDto.setUserEmail("test@test.com");

		try (MockedStatic<TokenUtil> mocked = Mockito.mockStatic(TokenUtil.class)) {
			mocked.when(TokenUtil::getEmailWithThrow).thenReturn("test@test.com");
			Mockito.when(authService.createAuthCodeByEmail(any(AuthQuoteDto.class))).thenReturn(Map.of("expireSeconds", 180));
			mockMvc.perform(post("/public/CA0003")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(reqDto)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.code").value("COM000"))
					.andExpect(jsonPath("$.result.expireSeconds").value(180));
		}
	}

	@Test
	@DisplayName("[실패] 인증번호 재발송 제한")
	void createAuthCodeFail_resendLimit() throws Exception {
		AuthQuoteDto reqDto = new AuthQuoteDto();
		reqDto.setUserEmail("test@test.com");

		try (MockedStatic<TokenUtil> mocked = Mockito.mockStatic(TokenUtil.class)) {
			mocked.when(TokenUtil::getEmailWithThrow).thenReturn("test@test.com");
			Mockito.when(authService.createAuthCodeByEmail(any(AuthQuoteDto.class))).thenThrow(new ValidationException("인증번호 발송 후 1분이 지나지 않았습니다."));
			mockMvc.perform(post("/public/CA0003")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(reqDto)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.code").value("ERR008"));
		}
	}
}