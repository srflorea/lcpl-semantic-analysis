package ro.pub.cs.lcpl;

import java.util.List;

/** A method call with a know static type
 * [<i>object</i>.<i>type</i>.<i>name</i> <i>expression</i> , ... ]
 */
public class StaticDispatch extends BaseDispatch {
	private String type;
	private Type selfType;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Type getSelfType() {
		return selfType;
	}
	public void setSelfType(Type selfType) {
		this.selfType = selfType;
	}
	public StaticDispatch(int lineNumber, Expression object, String type,
			String name, List<Expression> arguments) {
		setLineNumber(lineNumber);
		setObject(object);
		setType(type);
		setName(name);
		setArguments(arguments);
	}
	public StaticDispatch() {}
	

}
