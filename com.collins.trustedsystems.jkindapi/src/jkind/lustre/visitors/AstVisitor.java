package jkind.lustre.visitors;

import jkind.lustre.Assume;
import jkind.lustre.Constant;
import jkind.lustre.Contract;
import jkind.lustre.ContractBody;
import jkind.lustre.ContractImport;
import jkind.lustre.Equation;
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

public interface AstVisitor<T, S extends T> extends ExprVisitor<S> {
	public T visit(Assume assumption);

	public T visit(Constant constant);

	public T visit(Contract contract);

	public T visit(ContractBody contractBody);

	public T visit(ContractImport contractImport);

	public T visit(Equation equation);

	public T visit(Function function);

	public T visit(Guarantee guarantee);

	public T visit(ImportedFunction importedFunction);

	public T visit(ImportedNode importedNode);

	public T visit(Kind2Function kind2Function);

	public T visit(Mode mode);

	public T visit(Node node);

	public T visit(Program program);

	public T visit(TypeDef typeDef);

	public T visit(VarDecl varDecl);

	public T visit(VarDef varDef);
}
