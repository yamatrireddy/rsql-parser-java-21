package io.github.yamatrireddy.optimizer;

import io.github.yamatrireddy.ast.AstNode;
import io.github.yamatrireddy.ast.LogicalNode;

import java.util.ArrayList;
import java.util.List;

public final class AstOptimizer {
	
	public AstNode optimize(AstNode n) {
		if (n instanceof LogicalNode l) {
			List<AstNode> f = new ArrayList<>();
			for (AstNode c : l.children) {
				AstNode o = optimize(c);
				if (o instanceof LogicalNode ol && ol.operator.equals(l.operator)) {
					f.addAll(ol.children);
				} else {
					f.add(o);
				}
			}
			if (f.size() == 1) {
				return f.getFirst();
			}
			l.children.clear();
			l.children.addAll(f);
		}
		return n;
	}
}
