package io.github.yamatrireddy.api;

import io.github.yamatrireddy.Parser;
import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.lexer.Lexer;
import io.github.yamatrireddy.optimizer.AstOptimizer;
import io.github.yamatrireddy.security.AstValidator;
import io.github.yamatrireddy.security.FieldRegistry;

public final class RSQLParser {
	
	private final FieldRegistry fieldRegistry;
	
	public RSQLParser(FieldRegistry fieldRegistry) {
		this.fieldRegistry=fieldRegistry;
	}
	
	public RSQLExpression parse(String input){
		AstNode ast = new Parser(new Lexer(input)).parse();
		new AstValidator(fieldRegistry).validate(ast,0);
		AstNode opt = new AstOptimizer().optimize(ast);
		return new RSQLExpression(opt);
	}
	
}
