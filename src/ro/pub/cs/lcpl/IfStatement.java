package ro.pub.cs.lcpl;

/** if <i>expression</i> then <i>expression</i> else <i>expression</i> end; */
public class IfStatement extends Expression {
	/** Conditional expression : if <i>expression</i> then */
	private Expression condition;
	
	/** Expression evaluated if condition is true */
	private Expression ifExpr;

	/** Expression evaluated if condition is false; or null if the else branch is missing. */
	private Expression thenExpr;
	
	public Expression getCondition() {
		return condition;
	}
	public void setCondition(Expression condition) {
		this.condition = condition;
	}
	public Expression getIfExpr() {
		return ifExpr;
	}
	public void setIfExpr(Expression ifExpr) {
		this.ifExpr = ifExpr;
	}
	public Expression getThenExpr() {
		return thenExpr;
	}
	public void setThenExpr(Expression thenExpr) {
		this.thenExpr = thenExpr;
	}
	public IfStatement(int lineNumber, Expression condition, Expression ifExpr,
			Expression thenExpr) {
		super(lineNumber);
		this.condition = condition;
		this.ifExpr = ifExpr;
		this.thenExpr = thenExpr;
	}
	public IfStatement() {}

}
