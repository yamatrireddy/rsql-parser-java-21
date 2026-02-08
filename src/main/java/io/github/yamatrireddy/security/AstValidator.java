package io.github.yamatrireddy.security;

import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.ComparisonNode;
import io.github.yamatrireddy.ast.LogicalNode;

public final class AstValidator {
	
	private final FieldRegistry fieldRegistry;
	
	public AstValidator(FieldRegistry fieldRegistry) {
		this.fieldRegistry = fieldRegistry;
	}
	
	public void validate(AstNode node, int depth) {
		if (depth > Limits.MAX_DEPTH) {
			throw new SecurityException("Query too deep");
		}
		if (node instanceof ComparisonNode c) {
			fieldRegistry.validate(c.field);
			OperatorRegistry.validate(c.operator);
			for (String v : c.values) {
				if (v.length() > Limits.MAX_VALUE_LENGTH)
					throw new SecurityException("Value too long");
				if (v.chars().filter(ch -> ch == '*').count() > Limits.MAX_WILDCARDS)
					throw new SecurityException("Too many wildcards");
			}
		}
		if (node instanceof LogicalNode l) {
			for (AstNode ch : l.children) validate(ch, depth + 1);
		}
	}
	
}
