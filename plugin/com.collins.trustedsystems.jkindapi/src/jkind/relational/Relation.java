package jkind.relational;

import jkind.lustre.Expr;

public class Relation {

	public static Relation build(String id, Expr expr) {
		return new Relation(id, expr);
	}
	
	public final String id;
	public final Expr expr;
	
	private Relation(String id, Expr expr) {
		this.id = id;
		this.expr = expr;
	}	
}
