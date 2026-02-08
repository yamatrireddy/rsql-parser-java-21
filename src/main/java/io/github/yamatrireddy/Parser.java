package io.github.yamatrireddy;

import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;
import io.github.yamatrireddy.lexer.Lexer;
import io.github.yamatrireddy.lexer.Token;
import io.github.yamatrireddy.lexer.TokenType;

public final class Parser {
	
	private Token cur;
	private final Lexer lexer;
	
	public Parser(Lexer lexer) {
		this.lexer = lexer;
		this.cur = this.lexer.next();
	}
	
	public AstNode parse() {
		return or();
	}
	
	private AstNode or(){
		AstNode left = and();
		LogicalNode n = null;
		while (cur.getType() == TokenType.OR) {
			eat(TokenType.OR);
			if (n == null) {
				n = new LogicalNode("OR");
				n.children.add(left);
			}
			n.children.add(and());
		}
		return n ==null ? left : n;
	}
	
	private AstNode and(){
		AstNode left = constraint();
		LogicalNode n = null;
		while (cur.getType() == TokenType.AND) {
			eat(TokenType.AND);
			if (n == null) {
				n = new LogicalNode("AND");
				n.children.add(left);
			}
			n.children.add(constraint());
		}
		return n == null ? left : n;
	}
	
	private AstNode constraint(){
		if (cur.getType() == TokenType.L_PAREN) {
			eat(TokenType.L_PAREN);
			AstNode n = or();
			eat(TokenType.R_PAREN);
			return n;
		}
		return comparison();
	}
	
	private AstNode comparison(){
		ComparisonNode c = new ComparisonNode();
		c.field = eat(TokenType.VALUE).getValue();
		c.operator = eat(TokenType.OPERATOR).getValue();
		if (cur.getType() == TokenType.L_PAREN){
			eat(TokenType.L_PAREN);
			c.values.add(eat(TokenType.VALUE).getValue());
			while (cur.getType() == TokenType.OR){
				eat(TokenType.OR);
				c.values.add(eat(TokenType.VALUE).getValue());
			}
			eat(TokenType.R_PAREN);
		} else {
			c.values.add(eat(TokenType.VALUE).getValue());
		}
		return c;
	}
	
	private Token eat(TokenType t){
		if (cur.getType() != t)
			throw new IllegalStateException("Expected "+t);
		Token o = cur;
		cur = lexer.next();
		return o;
	}
}
