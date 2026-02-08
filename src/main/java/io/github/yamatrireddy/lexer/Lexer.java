package io.github.yamatrireddy.lexer;

public final class Lexer {
	private final String input;
	private int pos = 0;
	
	public Lexer(String input) {
		this.input = input;
	}
	
	private void skip() {
		while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
	}
	
	public Token next() {
		skip();
		
		if (pos >= input.length()) {
			return new Token(TokenType.EOF, "");
		}
		
		char c = input.charAt(pos);
		
		if (c == '(') {
			return take(TokenType.L_PAREN, "(");
		}
		if (c == ')') {
			return take(TokenType.R_PAREN, ")");
		}
		if (c == ';') {
			pos++;
			return new Token(TokenType.AND, "and");
		}
		if (c == ',') {
			pos++;
			return new Token(TokenType.OR, "or");
		}
		if (c == '"') {
			return quoted('"');
		}
		if (c == '\'') {
			return quoted('\'');
		}
		
		// Match Operators
		if (match("=in=")) {
			return op("=in=", 4);
		}
		if (match("=out=")) {
			return op("=out=", 5);
		}
		if (match("=gt=")) {
			return op(">", 4);
		}
		if (match("=ge=")) {
			return op(">=", 4);
		}
		if (match("=lt=")) {
			return op("<", 4);
		}
		if (match("=le=")) {
			return op("<=", 4);
		}
		if (match("==")) {
			return op("==", 2);
		}
		if (match("!=")) {
			return op("!=", 2);
		}
		if (match(">=")) {
			return op(">=",2);
		}
		if (match("<=")) {
			return op("<=", 2);
		}
		if (match(">")) {
			return op(">", 1);
		}
		if (match("<")) {
			return op("<", 1);
		}
		// Match Logical Operation Word
		if (matchWord("and")) {
			return word(TokenType.AND, "and");
		}
		if (matchWord("or")) {
			return word(TokenType.OR, "or");
		}
		
		return value();
	}
	
	private Token quoted(char quote) {
		pos++; // skip opening quote
		StringBuilder sb = new StringBuilder();
		
		while (pos < input.length()) {
			char c = input.charAt(pos);
			
			if (c == '\\' && pos + 1 < input.length()) {
				sb.append(input.charAt(pos + 1));
				pos += 2;
				continue;
			}
			
			if (c == quote) {
				pos++; // skip closing quote
				break;
			}
			
			sb.append(c);
			pos++;
		}
		
		return new Token(TokenType.VALUE, sb.toString());
	}
	
	private Token value() {
		int s = pos;
		while (pos < input.length() && !"()=<>;,".contains(String.valueOf(input.charAt(pos))) && !Character.isWhitespace(input.charAt(pos))) {
			pos++;
		}
		return new Token(TokenType.VALUE, input.substring(s, pos));
	}
	
	private boolean match(String s) {
		return input.startsWith(s, pos);
	}
	
	private boolean matchWord(String w) {
		int e = pos + w.length();
		if (e > input.length()) return false;
		
		if (!input.substring(pos, e).equals(w)) return false;
		
		if (e == input.length()) return true;
		
		char next = input.charAt(e);
		return Character.isWhitespace(next)
				|| next == '('
				|| next == ')'
				|| next == ';'
				|| next == ',';
	}
	
	
	private Token take(TokenType t, String s) {
		pos += s.length();
		return new Token(t, s);
	}
	
	private Token op(String o, int consumedLength) {
		pos += consumedLength;
		return new Token(TokenType.OPERATOR, o);
	}
	
	private Token word(TokenType t, String w) {
		pos += w.length();
		return new Token(t, w);
	}
}
