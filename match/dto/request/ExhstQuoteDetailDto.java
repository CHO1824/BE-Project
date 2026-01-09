package wayble.ade.match.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ExhstQuoteDetailDto {
	/**
	 * 견적 ID
	 */
	@NotBlank
	private String quoteId;
}
