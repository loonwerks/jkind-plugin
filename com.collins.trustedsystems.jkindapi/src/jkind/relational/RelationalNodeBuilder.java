package jkind.relational;

import static jkind.lustre.LustreUtil.and;
import static jkind.lustre.LustreUtil.id;
import static jkind.lustre.LustreUtil.implies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkind.lustre.Constant;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.TypeDef;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.EquationBuilder;
import jkind.lustre.builders.NodeBuilder;
import jkind.lustre.builders.ProgramBuilder;

public class RelationalNodeBuilder {

	private String id;
	
	public Set<String> namespace = new HashSet<>();
	
	private Map<String,Node> support = new LinkedHashMap<>();
	private Map<String,Node> called = new LinkedHashMap<>();
	
	private Map<String,Constant> constants = new LinkedHashMap<>();
	private Map<String,TypeDef> typedefs = new LinkedHashMap<>();
	private Map<String,Function> functions = new LinkedHashMap<>();
	
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
	
	public RelationalNodeBuilder addSupportNode(Node node) {
		checkNamespace(node.id);
		namespace.add(node.id);
		support.put(node.id, node);
		return this;
	}
	
	public RelationalNodeBuilder addCalledNode(Node node) {
		checkNamespace(node.id);
		namespace.add(node.id);
		called.put(node.id, node);
		return this;
	}
	
	public IdExpr createConstant(String name, Type t, Expr e) {
		checkNamespace(name);
		Constant c = new Constant(name,t,e);
		namespace.add(name);
		constants.put(name, c);
		return new IdExpr(name);
	}
	
	public NamedType createTypeDefinition(String name, Type t) {
		checkNamespace(name);
		TypeDef td = new TypeDef(name,t);
		namespace.add(name);
		typedefs.put(name, td);
		return new NamedType(name);
	}
	
	public RelationalNodeBuilder addFunction(Function f) {
		checkNamespace(f.id);
		namespace.add(f.id);
		functions.put(f.id, f);
		return this;
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
	
	public Program build() {

		ProgramBuilder program = new ProgramBuilder();
		
		support.entrySet().forEach(node -> program.addNode(node.getValue()));
		constants.entrySet().forEach(constant -> program.addConstant(constant.getValue()));
		typedefs.entrySet().forEach(typedef -> program.addType(typedef.getValue()));
		functions.entrySet().forEach(function -> program.addFunction(function.getValue()));
		
		NodeBuilder main = new NodeBuilder(id);
		inputs.entrySet().forEach(input -> main.addInput(input.getValue()));
		outputs.entrySet().forEach(output -> main.addInput(output.getValue()));
		locals.entrySet().forEach(local -> main.addInput(local.getValue()));
		
		assumptions.entrySet().forEach(assumption -> main.createOutput(assumption.getKey(), NamedType.BOOL));
		assumptions.entrySet().forEach(assumption -> main.addEquation(crunch(assumption.getValue())));
		assumptions.entrySet().forEach(assumption -> main.addIvc(assumption.getKey()));
		
		constraints.entrySet().forEach(relation -> main.createOutput(relation.getKey(), NamedType.BOOL));
		constraints.entrySet().forEach(relation -> main.addEquation(crunch(relation.getValue())));
		constraints.entrySet().forEach(relation -> main.addIvc(relation.getKey()));
		
		IdExpr conjunct = main.createLocal("conjunct", NamedType.BOOL);
		main.addEquation(new Equation(conjunct, conjunctAllRelations()));
		
		properties.entrySet().forEach(property -> main.createLocal(property.getKey(), NamedType.BOOL));
		properties.entrySet().forEach(property -> main.addEquation(crunchProperty(conjunct, property.getValue())));
		properties.entrySet().forEach(property -> main.addProperty(property.getKey()));
		
		Node n = main.build();
		program.addNode(n);
		program.setMain(n.id);
		
		return program.build();
	}
}
