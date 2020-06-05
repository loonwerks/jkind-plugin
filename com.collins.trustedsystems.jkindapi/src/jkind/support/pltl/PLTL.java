package jkind.support.pltl;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.Expr;
import jkind.lustre.LustreUtil;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;

public class PLTL {
	
	public static final String HISTORICALLY = "historically";
	public static final String ONCE = "once";
	public static final String SINCE = "since";
	public static final String TRIGGERS = "triggers";
	
	public static List<Node> getAllNodes() {
		List<Node> list = new ArrayList<>();
		list.add(historically());
		list.add(once());
		list.add(triggers());
		list.add(since());
		return list;
	}

	public static Node historically() {
		return LustreUtil.historically(HISTORICALLY);
	}
	
	public static NodeCallExpr historically(Expr e) {
		return new NodeCallExpr(HISTORICALLY, e);
	}
	
	public static Node once() {
		return LustreUtil.once(ONCE);
	}
	
	public static NodeCallExpr once(Expr e) {
		return new NodeCallExpr(ONCE, e);
	}
	
	public static Node since() {
		return LustreUtil.since(SINCE);
	}
	
	public static NodeCallExpr since(Expr e1, Expr e2) {
		return new NodeCallExpr(SINCE, e1, e2);
	}
	
	public static Node triggers() {
		return LustreUtil.triggers(TRIGGERS);
	}
	
	public static NodeCallExpr triggers(Expr e1, Expr e2) {
		return new NodeCallExpr(TRIGGERS, e1, e2);
	}
}
