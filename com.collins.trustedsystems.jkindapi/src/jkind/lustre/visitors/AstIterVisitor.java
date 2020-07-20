package jkind.lustre.visitors;

import java.util.List;

import jkind.lustre.Assume;
import jkind.lustre.Constant;
import jkind.lustre.Contract;
import jkind.lustre.ContractBody;
import jkind.lustre.ContractImport;
import jkind.lustre.ContractItem;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.Guarantee;
import jkind.lustre.ImportedFunction;
import jkind.lustre.ImportedNode;
import jkind.lustre.Kind2Function;
import jkind.lustre.Mode;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.TypeDef;
import jkind.lustre.VarDecl;
import jkind.lustre.VarDef;
import jkind.util.Util;

public class AstIterVisitor extends ExprIterVisitor implements AstVisitor<Void, Void> {
	@Override
	public Void visit(Assume e) {
		e.expr.accept(this);
		return null;
	}

	@Override
	public Void visit(Constant e) {
		e.expr.accept(this);
		return null;
	}

	@Override
	public Void visit(Contract e) {
		visitVarDecls(e.inputs);
		visitVarDecls(e.outputs);
		visit(e.contractBody);
		return null;
	}

	@Override
	public Void visit(ContractBody e) {
		visitContractItems(e.items);
		return null;
	}

	protected void visitContractItems(List<ContractItem> es) {
		for (ContractItem e : es) {
			e.accept(this);
		}
	}

	@Override
	public Void visit(ContractImport e) {
		visitExprs(e.inputs);
		visitExprs(Util.safeList(e.outputs));
		return null;
	}

	@Override
	public Void visit(Equation e) {
		e.expr.accept(this);
		return null;
	}

	@Override
	public Void visit(Function e) {
		visitVarDecls(e.inputs);
		visitVarDecls(e.outputs);
		return null;
	}

	@Override
	public Void visit(Guarantee e) {
		e.expr.accept(this);
		return null;
	}

	@Override
	public Void visit(ImportedFunction e) {
		visitVarDecls(e.inputs);
		visitVarDecls(e.outputs);
		// if (e.contractBody != null) {
		//	visit(e.contractBody);
		// }
		return null;
	}

	@Override
	public Void visit(ImportedNode e) {
		visitVarDecls(e.inputs);
		visitVarDecls(e.outputs);
		// if (e.contractBody != null) {
		//	visit(e.contractBody);
		// }
		return null;
	}

	@Override
	public Void visit(Kind2Function e) {
		visitVarDecls(e.inputs);
		visitVarDecls(e.outputs);
		visitVarDecls(e.locals);
		visitEquations(e.equations);
		visitAssertions(e.assertions);
		return null;
	}

	@Override
	public Void visit(Mode e) {
		visitExprs(e.require);
		visitExprs(e.ensure);
		return null;
	}

	@Override
	public Void visit(Node e) {
		visitVarDecls(e.inputs);
		visitVarDecls(e.outputs);
		visitVarDecls(e.locals);
		visitEquations(e.equations);
		visitAssertions(e.assertions);
		// if (e.contractBody != null) {
		//	visit(e.contractBody);
		// }
		return null;
	}

	protected void visitVarDecls(List<VarDecl> es) {
		for (VarDecl e : es) {
			visit(e);
		}
	}

	protected void visitEquations(List<Equation> es) {
		for (Equation e : es) {
			visit(e);
		}
	}

	protected void visitAssertions(List<Expr> es) {
		visitExprs(es);
	}

	@Override
	public Void visit(Program e) {
		visitTypeDefs(e.types);
		visitConstants(e.constants);
		visitFunctions(e.functions);
		// visitImportedFunctions(e.importedFunctions);
		// visitImportedNodes(e.importedNodes);
		// visitContracts(e.contracts);
		// visitKind2Functions(e.kind2Functions);
		visitNodes(e.nodes);
		return null;
	}

	protected void visitTypeDefs(List<TypeDef> es) {
		for (TypeDef e : es) {
			visit(e);
		}
	}

	protected void visitConstants(List<Constant> es) {
		for (Constant e : es) {
			visit(e);
		}
	}

	protected void visitFunctions(List<Function> es) {
		for (Function e : es) {
			visit(e);
		}
	}

	protected void visitImportedFunctions(List<ImportedFunction> es) {
		for (ImportedFunction e : es) {
			visit(e);
		}
	}

	protected void visitImportedNodes(List<ImportedNode> es) {
		for (ImportedNode e : es) {
			visit(e);
		}
	}

	protected void visitContracts(List<Contract> es) {
		for (Contract e : es) {
			visit(e);
		}
	}

	protected void visitKind2Functions(List<Kind2Function> es) {
		for (Kind2Function e : es) {
			visit(e);
		}
	}

	protected void visitNodes(List<Node> es) {
		for (Node e : es) {
			visit(e);
		}
	}

	@Override
	public Void visit(TypeDef e) {
		return null;
	}

	@Override
	public Void visit(VarDecl e) {
		return null;
	}

	@Override
	public Void visit(VarDef e) {
		e.expr.accept(this);
		return null;
	}
}
