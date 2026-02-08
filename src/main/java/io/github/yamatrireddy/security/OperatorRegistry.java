package io.github.yamatrireddy.security;

import java.util.Set;

public final class OperatorRegistry {
	
	private static final Set<String> OPS = Set.of("==","!=",">",">=","<","<=","=in=","=out=");
	
	public static void validate(String op) {
		if (!OPS.contains(op)) {
			throw new SecurityException("Operator not allowed: " + op);
		}
	}
}
