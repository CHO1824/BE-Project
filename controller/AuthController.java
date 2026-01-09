package wayble.ade.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import wayble.ade.auth.service.AuthService;
import wayble.ade.common.dto.AuthQuoteDto;
import wayble.ade.common.data.response.ApiResponse;
import wayble.ade.common.data.type.ApiResponseCodeType;

import java.util.Map;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;
	@PostMapping(path = "/CA0003")
	public ApiResponse createAuthCode(@RequestBody AuthQuoteDto dto) {
		Map<String, Object> result = authService.createAuthCodeByEmail(dto);
		return ApiResponse.ofSuccess(ApiResponseCodeType.COM000, result);
	}
}