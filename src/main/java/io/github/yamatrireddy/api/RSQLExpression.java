package io.github.yamatrireddy.api;

import io.github.yamatrireddy.ast.AstNode;
import lombok.Getter;
import lombok.ToString;

@ToString
public class RSQLExpression {
	
	@Getter
	private AstNode ast;
	
	public RSQLExpression(AstNode ast) {
		this.ast = ast;
	}
}
