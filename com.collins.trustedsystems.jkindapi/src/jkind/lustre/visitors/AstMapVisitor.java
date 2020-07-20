package jkind.lustre.visitors;

import java.util.List;
import java.util.stream.Collectors;

import jkind.lustre.Assume;
import jkind.lustre.Ast;
import jkind.lustre.Constant;
import jkind.lustre.Contract;
import jkind.lustre.ContractBody;
import jkind.lustre.ContractImport;
import jkind.lustre.ContractItem;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.Guarantee;
import jkind.lustre.IdExpr;
import jkind.lustre.ImportedFunction;
import jkind.lustre.ImportedNode;
import jkind.lustre.Kind2Function;
import jkind.lustre.Mode;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.TypeDef;
import jkind.lustre.VarDecl;
import jkind.lustre.VarDef;

public class AstMapVisitor extends ExprMapVisitor implements AstVisitor<Ast, Expr> {
	@Override
	public Assume visit(Assume e) {
		return new Assume(e.location, e.expr.accept(this));
	}

	@Override
	public Constant visit(Constant e) {
		return new Constant(e.location, e.id, e.type, e.expr.accept(this));
	}

	@Override
	public Contract visit(Contract e) {
		List<VarDecl> inputs = visitVarDecls(e.inputs);
		List<VarDecl> outputs = visitVarDecls(e.outputs);
		ContractBody contractBody = visit(e.contractBody);

		return new Contract(e.id, inputs, outputs, contractBody);
	}

	@Override
	public ContractBody visit(ContractBody e) {
		return new ContractBody(visitContractItems(e.items));
	}

	protected List<ContractItem> visitContractItems(List<? extends ContractItem> is) {
		is.get(0).accept(this);
		return map(this::visitContractItem, is);
	}

	protected ContractItem visitContractItem(ContractItem i) {
		if (i instanceof Assume) {
			return visit((Assume) i);
		} else if (i instanceof Constant) {
			return visit((Constant) i);
		} else if (i instanceof ContractImport) {
			return visit((ContractImport) i);
		} else if (i instanceof Guarantee) {
			return visit((Guarantee) i);
		} else if (i instanceof Mode) {
			return visit((Mode) i);
		} else {
			return visit((VarDef) i);
		}
	}

	@Override
	public ContractImport visit(ContractImport e) {
		return new ContractImport(e.location, e.id, visitExprs(e.inputs),
				visitExprs(e.outputs).stream().map(x -> (IdExpr) x).collect(Collectors.toList()));
	}

	@Override
	public Equation visit(Equation e) {
		// Do not traverse e.lhs since they do not really act like Exprs
		return new Equation(e.location, e.lhs, e.expr.accept(this));
	}

	@Override
	public Function visit(Function e) {
		List<VarDecl> inputs = visitVarDecls(e.inputs);
		List<VarDecl> outputs = visitVarDecls(e.outputs);
		return new Function(e.location, e.id, inputs, outputs);
	}

	@Override
	public Guarantee visit(Guarantee e) {
		return new Guarantee(e.location, e.expr.accept(this));
	}

	public ImportedFunction visit(ImportedFunction e) {
		List<VarDecl> inputs = visitVarDecls(e.inputs);
		List<VarDecl> outputs = visitVarDecls(e.outputs);
		ContractBody contractBody = null;
		if (e.contractBody != null) {
			contractBody = visit(e.contractBody);
		}
		return new ImportedFunction(e.location, e.id, inputs, outputs, contractBody);
	}

	public ImportedNode visit(ImportedNode e) {
		List<VarDecl> inputs = visitVarDecls(e.inputs);
		List<VarDecl> outputs = visitVarDecls(e.outputs);
		ContractBody contractBody = null;
		if (e.contractBody != null) {
			contractBody = visit(e.contractBody);
		}
		return new ImportedNode(e.location, e.id, inputs, outputs, contractBody);
	}

	@Override
	public Kind2Function visit(Kind2Function e) {
		List<VarDecl> inputs = visitVarDecls(e.inputs);
		List<VarDecl> outputs = visitVarDecls(e.outputs);
		ContractBody contractBody = null;
		if (e.contractBody != null) {
			contractBody = visit(e.contractBody);
		}
		List<VarDecl> locals = visitVarDecls(e.locals);
		List<Equation> equations = visitEquations(e.equations);
		List<Expr> assertions = visitAssertions(e.assertions);
		List<String> properties = visitProperties(e.properties);
		return new Kind2Function(e.location, e.id, inputs, outputs, contractBody, locals, equations, assertions,
				properties);
	}

	@Override
	public Mode visit(Mode e) {
		return new Mode(e.id, visitExprs(e.require), visitExprs(e.ensure));
	}

	@Override
	public Node visit(Node e) {
		List<VarDecl> inputs = visitVarDecls(e.inputs);
		List<VarDecl> outputs = visitVarDecls(e.outputs);
		// ContractBody contractBody = null;
		// if (e.contractBody != null) {
		//	contractBody = visit(e.contractBody);
		// }
		List<VarDecl> locals = visitVarDecls(e.locals);
		List<Equation> equations = visitEquations(e.equations);
		List<Expr> assertions = visitAssertions(e.assertions);
		List<String> properties = visitProperties(e.properties);
		List<String> ivc = visitIvc(e.ivc);
		List<String> realizabilityInputs = visitRealizabilityInputs(e.realizabilityInputs);
		return new Node(e.location, e.id, inputs, outputs, locals, equations, properties, assertions,
				realizabilityInputs, e.contractBody, ivc);
	}

	protected List<VarDecl> visitVarDecls(List<VarDecl> es) {
		return map(this::visit, es);
	}

	protected List<Equation> visitEquations(List<Equation> es) {
		return map(this::visit, es);
	}

	protected List<Expr> visitAssertions(List<Expr> es) {
		return visitExprs(es);
	}

	protected List<String> visitProperties(List<String> es) {
		return map(this::visitProperty, es);
	}

	protected String visitProperty(String e) {
		return e;
	}

	protected List<String> visitIvc(List<String> es) {
		return map(this::visitIvc, es);
	}

	protected String visitIvc(String e) {
		return e;
	}

	protected List<String> visitRealizabilityInputs(List<String> es) {
		if (es == null) {
			return null;
		}
		return map(this::visitRealizabilityInput, es);
	}

	protected String visitRealizabilityInput(String e) {
		return e;
	}

	@Override
	public Program visit(Program e) {
		List<TypeDef> types = visitTypeDefs(e.types);
		List<Constant> constants = visitConstants(e.constants);
		List<Function> functions = visitFunctions(e.functions);
		// List<ImportedFunction> importedFunctions = visitImportedFunctions(e.importedFunctions);
		// List<ImportedNode> importedNodes = visitImportedNodes(e.importedNodes);
		// List<Contract> contracts = visitContracts(e.contracts);
		// List<Kind2Function> kind2Functions = visitKind2Functions(e.kind2Functions);

		List<Node> nodes = visitNodes(e.nodes);
		return new Program(e.location, types, constants, functions, e.importedFunctions, e.importedNodes, e.contracts,
				e.kind2Functions, nodes, e.main);
	}

	protected List<TypeDef> visitTypeDefs(List<TypeDef> es) {
		return map(this::visit, es);
	}

	protected List<Constant> visitConstants(List<Constant> es) {
		return map(this::visit, es);
	}

	protected List<ImportedFunction> visitImportedFunctions(List<ImportedFunction> es) {
		return map(this::visit, es);
	}

	protected List<ImportedNode> visitImportedNodes(List<ImportedNode> es) {
		return map(this::visit, es);
	}

	protected List<Contract> visitContracts(List<Contract> es) {
		return map(this::visit, es);
	}

	protected List<Kind2Function> visitKind2Functions(List<Kind2Function> es) {
		return map(this::visit, es);
	}

	protected List<Node> visitNodes(List<Node> es) {
		return map(this::visit, es);
	}

	protected List<Function> visitFunctions(List<Function> es) {
		return map(this::visit, es);
	}

	@Override
	public TypeDef visit(TypeDef e) {
		return e;
	}

	@Override
	public VarDecl visit(VarDecl e) {
		return e;
	}

	@Override
	public VarDef visit(VarDef e) {
		return new VarDef(e.location, visit(e.varDecl), e.expr.accept(this));
	}
}
