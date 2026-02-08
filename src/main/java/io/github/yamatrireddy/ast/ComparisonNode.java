package io.github.yamatrireddy.ast;

import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public class ComparisonNode extends AstNode {
	public String field;
	public String operator;
	public final List<String> values = new ArrayList<>();
}
