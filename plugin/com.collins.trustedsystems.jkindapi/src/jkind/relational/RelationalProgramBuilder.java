package jkind.relational;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jkind.lustre.Constant;
import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.IdExpr;
import jkind.lustre.LustreUtil;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.TypeDef;
import jkind.lustre.builders.ProgramBuilder;

public class RelationalProgramBuilder {

	public Set<String> namespace = new HashSet<>();
	
	private String mainId;
	private Map<String,Node> nodes = new LinkedHashMap<>();
	
	private Map<String,Constant> constants = new LinkedHashMap<>();
	private Map<String,TypeDef> typedefs = new LinkedHashMap<>();
	private Map<String,Function> functions = new LinkedHashMap<>();
	
	public RelationalProgramBuilder() {
		addNode(LustreUtil.historically("historically"));
		addNode(LustreUtil.once("once"));
	}
	
	private void checkNamespace(String name) {
		if (namespace.contains(name)) {
			throw new JKindRelationalException(name + " is already used in this namespace.");
		}		
	}
	
	public RelationalProgramBuilder addMainNode(Node node) {
		addNode(node);
		this.mainId = node.id;
		return this;
	}
	
	public RelationalProgramBuilder addNode(Node node) {
		checkNamespace(node.id);
		namespace.add(node.id);
		nodes.put(node.id, node);
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
	
	public RelationalProgramBuilder addFunction(Function f) {
		checkNamespace(f.id);
		namespace.add(f.id);
		functions.put(f.id, f);
		return this;
	}
	
	public Program build() {
		ProgramBuilder program = new ProgramBuilder();
		
		nodes.entrySet().forEach(node -> program.addNode(node.getValue()));
		constants.entrySet().forEach(constant -> program.addConstant(constant.getValue()));
		typedefs.entrySet().forEach(typedef -> program.addType(typedef.getValue()));
		functions.entrySet().forEach(function -> program.addFunction(function.getValue()));
		
		program.setMain(this.mainId);
		return program.build();
	}

}
