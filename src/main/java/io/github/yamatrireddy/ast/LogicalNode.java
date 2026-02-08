package io.github.yamatrireddy.ast;

import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public class LogicalNode extends AstNode {
	public final String operator;
	public final List<AstNode> children = new ArrayList<>();
	public LogicalNode(String operator) { this.operator = operator; }
}
