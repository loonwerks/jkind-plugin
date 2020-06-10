package jkind.relational;

import static jkind.lustre.LustreUtil.and;
import static jkind.lustre.LustreUtil.arrow;
import static jkind.lustre.LustreUtil.eq;
import static jkind.lustre.LustreUtil.equal;
import static jkind.lustre.LustreUtil.id;
import static jkind.lustre.LustreUtil.implies;
import static jkind.lustre.LustreUtil.integer;
import static jkind.lustre.LustreUtil.not;
import static jkind.lustre.LustreUtil.plus;
import static jkind.lustre.LustreUtil.pre;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.EquationBuilder;
import jkind.lustre.builders.NodeBuilder;

public class RelationalNodeBuilder {

	private String id;
	
	public Set<String> namespace = new HashSet<>();
	
	private Map<String,VarDecl> inputs = new LinkedHashMap<>();
	private Map<String,VarDecl> outputs = new LinkedHashMap<>();
	private Map<String,VarDecl> locals = new LinkedHashMap<>();
	
	private Map<String,Relation> assumptions = new LinkedHashMap<>();
	private Map<String,Relation> constraints = new LinkedHashMap<>();
	private Map<String,Relation> properties = new LinkedHashMap<>();
	
	public RelationalNodeBuilder(String id) {
		this.id = id;
	}
	
	private void checkNamespace(String name) {
		if (namespace.contains(name)) {
			throw new JKindRelationalException(name + " is already used in this namespace.");
		}		
	}
	
	public List<IdExpr> getReturnVariables() {
		List<IdExpr> idExprs = new ArrayList<>();
		assumptions.entrySet().forEach(assumption -> idExprs.add(id(assumption.getKey())));
		constraints.entrySet().forEach(constraint -> idExprs.add(id(constraint.getKey())));
		return idExprs;
	}
	
	public IdExpr createInput(String name, Type t) {
		addInput(new VarDecl(name, t));
		return new IdExpr(name);
	}
	
	public RelationalNodeBuilder addInput(VarDecl input) {
		checkNamespace(input.id);
		namespace.add(input.id);
		inputs.put(input.id, input);
		return this;
	}
	
	public IdExpr createOutput(String name, Type t) {
		addOutput(new VarDecl(name, t));
		return new IdExpr(name);
	}
	
	public RelationalNodeBuilder addOutput(VarDecl output) {
		checkNamespace(output.id);
		namespace.add(output.id);
		outputs.put(output.id, output);
		return this;
	}
	
	public IdExpr createLocal(String name, Type t) {
		addLocal(new VarDecl(name, t));
		return new IdExpr(name);
	}
	
	public RelationalNodeBuilder addLocal(VarDecl local) {
		checkNamespace(local.id);
		namespace.add(local.id);
		locals.put(local.id, local);
		return this;
	}
	
	public Relation createAssumption(String name, Expr constraint) {
		checkNamespace(name);
		namespace.add(name);
		Relation r = Relation.build(name, constraint);
		assumptions.put(name, r);
		return r;
	}
	
	public RelationalNodeBuilder addAssumption(Relation r) {
		checkNamespace(r.id);
		namespace.add(r.id);
		assumptions.put(r.id, r);
		return this;
	}
	
	public RelationalNodeBuilder addRelation(Relation r) {
		checkNamespace(r.id);
		namespace.add(r.id);
		constraints.put(r.id, r);
		return this;
	}
	
	public Relation createConstraint(String name, Expr constraint) {
		checkNamespace(name);
		namespace.add(name);
		Relation r = Relation.build(name, constraint);
		constraints.put(name, r);
		return r;
	}
	
	public RelationalNodeBuilder addProperty(Relation r) {
		checkNamespace(r.id);
		namespace.add(r.id);
		properties.put(r.id, r);
		return this;
	}

	public Relation createProperty(String name, Expr constraint) {
		checkNamespace(name);		
		namespace.add(name);
		Relation r = Relation.build(name, constraint);
		properties.put(name, r);
		return r;
	}
	
	private Equation crunch(Relation r) {
		EquationBuilder eq = new EquationBuilder();
		eq.addLhs(r.id);
		eq.setExpr(r.expr);
		return eq.build();
	}
	
	private Expr conjunctAllRelations() {
		List<Expr> relationIds = new ArrayList<>();
		assumptions.entrySet().forEach(assumption -> relationIds.add(id(assumption.getKey())));
		constraints.entrySet().forEach(constraint -> relationIds.add(id(constraint.getKey())));
		return new NodeCallExpr("historically", and(relationIds));
	}
	
	private Equation crunchProperty(IdExpr id, Relation r) {
		EquationBuilder eq = new EquationBuilder();
		eq.addLhs(r.id);
		eq.setExpr(implies(id ,r.expr));
		return eq.build();		
	}
	
	public NodeCallExpr call(List<Expr> args) {
		int input_size = inputs.size() + outputs.size() + locals.size();
		if (args.size() != input_size) {
			throw new JKindRelationalException(this.id + " expects " + input_size + " arguments, but received " + args.size());
		}
		
		return new NodeCallExpr(this.id, args);
	}
	
	public Node build() {
		NodeBuilder node = new NodeBuilder(id);
		inputs.entrySet().forEach(input -> node.addInput(input.getValue()));
		outputs.entrySet().forEach(output -> node.addInput(output.getValue()));
		locals.entrySet().forEach(local -> node.addInput(local.getValue()));
		
		assumptions.entrySet().forEach(assumption -> node.createOutput(assumption.getKey(), NamedType.BOOL));
		assumptions.entrySet().forEach(assumption -> node.addEquation(crunch(assumption.getValue())));
		
		constraints.entrySet().forEach(relation -> node.createOutput(relation.getKey(), NamedType.BOOL));
		constraints.entrySet().forEach(relation -> node.addEquation(crunch(relation.getValue())));
		
		return node.build();
	}
	
	public Node buildEntailment() {
		NodeBuilder node = new NodeBuilder(this.build());
		
		IdExpr conjunct = node.createLocal("conjunct", NamedType.BOOL);
		node.addEquation(new Equation(conjunct, conjunctAllRelations()));
		
		properties.entrySet().forEach(property -> node.createLocal(property.getKey(), NamedType.BOOL));
		properties.entrySet().forEach(property -> node.addEquation(crunchProperty(conjunct, property.getValue())));
		properties.entrySet().forEach(property -> node.addProperty(property.getKey()));
		
		assumptions.entrySet().forEach(assumption -> node.addIvc(assumption.getKey()));
		constraints.entrySet().forEach(constraint -> node.addIvc(constraint.getKey()));
		
		return node.build();
	}
	
	public Node buildConsistency(int N) {
		NodeBuilder node = new NodeBuilder(this.build());
		
		IdExpr conjunct = node.createLocal("conjunct", NamedType.BOOL);
		node.addEquation(new Equation(conjunct, conjunctAllRelations()));
		
		IdExpr step = node.createLocal("step", NamedType.INT);
		node.addEquation(eq(step, plus(arrow(integer(0),pre(step)),integer(1))));
		
		IdExpr consistent = node.createLocal("consistent", NamedType.BOOL);
		node.addEquation(eq(consistent, not(and(equal(step,integer(N)), conjunct))));
		node.addProperty(consistent);
		
		assumptions.entrySet().forEach(assumption -> node.addIvc(assumption.getKey()));
		constraints.entrySet().forEach(constraint -> node.addIvc(constraint.getKey()));
		
		return node.build();
	}
	
	public Node buildRealizability() {
		NodeBuilder node = new NodeBuilder(this.build());
		
		IdExpr conjunct = node.createLocal("conjunct", NamedType.BOOL);
		node.addEquation(new Equation(conjunct, conjunctAllRelations()));
		
		assumptions.entrySet().forEach(assumption -> node.addAssertion(assumption.getValue().expr));
		properties.entrySet().forEach(property -> node.createLocal(property.getKey(), NamedType.BOOL));
		properties.entrySet().forEach(property -> node.addEquation(crunchProperty(conjunct, property.getValue())));
		
		List<String> realizabilityInputs = new ArrayList<>();
		this.inputs.entrySet().forEach(input -> realizabilityInputs.add(input.getKey()));
		node.setRealizabilityInputs(realizabilityInputs);
		
		return node.build();
	}
}
