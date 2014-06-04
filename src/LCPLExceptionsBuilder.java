
/*
 * Class which contains methods definitions for every possible types of LCPLException exceptions
 */
public class LCPLExceptionsBuilder {

	public String attributeNotFoundInClass(String attribute, String className) {
		String message = "";
		message += "Attribute ";
		message += attribute;
		message += " not found in class ";
		message += className;
	
		return message;
	}
	
	public String canNotConvertAValueInto(String type, String into) {
		String message = "";
		message += "Cannot convert a value of type ";
		message += type;
		message += " into ";
		message += into;
		
		return message;
	}
	
	public String methodNotFoundMessage(String methodName, String classOfObject) {
		String message = "";
		message += "Method ";
		message += methodName;
		message += " not found in class ";
		message += classOfObject;
		
		return message;
	}
	
	public String classNotFoundMessage(String className) {
		String message = "";
		message += "Class ";
		message += className;
		message += " not found.";
		
		return message;
	}
	
	public String canNotConvertInStaticDispatchMessage(String fromClass, String toClass) {
		String message = "";
		message += "Cannot convert from ";
		message += fromClass;
		message += " to ";
		message += toClass;
		message += " in StaticDispatch";
		
		return message;
	}
	
	public String classAlreadyExistsMessage(String className) {
		String message = "";
		message += "A class with the same name already exists : ";
		message += className;
		
		return message;
	}

	public String methodWithTheSameNameExists(String methodName, String className) {
		String message = "";
		message += "A method with the same name already exists in class ";
		message += className;
		message += " : ";
		message += methodName;
		
		return message;
	}

	public String parameterHasDifferentTypeInOverloadedMethod(String parameter) {
		String message = "";
		message += "Parameter ";
		message += parameter;
		message += " has a different type in overloaded method.";
		
		return message;
	}

	public String overloadedMethodHasDifferentNumberOfParameters() {
		String message = "";
		message += "Overloaded method has a different number of parameters";
		
		return message;
	}

	public String returnTypeChangedInOverloadedMethod() {
		String message = "";
		message += "Return type changed in overloaded method.";
		
		return message;
	}

	public String illegalConstruction(String type) {
		String message = "";
		message += "Illegal construction : new ";
		message += type;
		
		return message;
	}

	public String invalidTypeOfParameters(String operator) {
		String message = "";
		message += "Invalid type of parameters for ";
		message += operator;
		message += " expression";
		
		return message;
	}
	
	public String cannotConvertAdditionExpression() {
		String message = "";
		message += "Cannot convert '+' expression to Int or String";
		
		return message;
	}

	public String notEnoughArgumentsInMethodCall(String methodName) {
		String message = "";
		message += "Not enough arguments in method call ";
		message += methodName;
		
		return message;
	}

	public String tooManyArgumentsInMethodCall(String methodName) {
		String message = "";
		message += "Too many arguments in method call ";
		message += methodName;
		
		return message;
	}

	public String classRecursivelyInheritsItself(String className) {
		String message = "";
		message += "Class ";
		message += className;
		message += " recursively inherits itself.";
		
		return message;
	}

	public String aClassCanNotInheritAString() {
		String message = "";
		message += "A class cannot inherit a String";
		
		return message;
	}

	public String conditionMustBeInt(String statement) {
		String message = "";
		message += statement;
		message += " condition must be Int";
		
		return message;
	}
	
}
