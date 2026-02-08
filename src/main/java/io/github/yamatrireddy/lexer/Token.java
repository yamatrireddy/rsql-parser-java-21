package io.github.yamatrireddy.lexer;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Token {
	private TokenType type;
	private String value;
}
