import java.util.*;

import ro.pub.cs.lcpl.*;

public class LCPLFuturesWalker {

	private Program p;
	private LCPLClass lcplClass;
	
	private Map<String, Map<String, Variable>> attributeSymbols;
	private Map<String, Variable> paramsSymbols;
	private Map<String, Variable> localSymbols;
	
	private Map<String, Method> methods;
	
	public LCPLFuturesWalker(Program p, LCPLClass lcplClass, Map<String, Map<String, Variable>> attributeSymbols) {
		this.p 				  = p;
		this.lcplClass 		  = lcplClass;
		
		this.attributeSymbols = attributeSymbols;
		paramsSymbols		  = new LinkedHashMap<String, Variable>();
		localSymbols		  = new LinkedHashMap<String, Variable>();
		
		methods = new LinkedHashMap<String, Method>();
	}
	
	/*
	 * Method that make a superficial walking through the futures. It sets only the returning types and the parameters in order to use
	 * them in the second walking.
	 */
	public void makeSuperficialWalking() throws LCPLException {
		makeASuperficialWalkThroughMethods();
		makeASuperficialWalkThroughAttributes();
	}
	
	/*
	 * Second walking through the futures.
	 * It sets up the attributes initializing and the methods bodies.
	 */
	public void walkthroughFutures() throws LCPLException {
		walkThroughAttributes();
		walkThroughMethodsBody();
	}
	
	private void makeASuperficialWalkThroughMethods() throws LCPLException {
		List<Feature> features = lcplClass.getFeatures();
		for(Feature feature : features) {
			if(feature instanceof Method) {
				Method method = ((Method)feature);
				
				if(methods.containsKey(method.getName())) {
					LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
					String message = eb.methodWithTheSameNameExists(method.getName(), lcplClass.getName());
					throw new LCPLException(message, method);
				}
				else {
					methods.put(method.getName(), method);
				}
				
				method.setParent(lcplClass);
				
				/* self variable */
				FormalParam self = new FormalParam(LCPLConstants.SELF, lcplClass.getName());
				self.setVariableType(lcplClass);
				method.setSelf(self);
				
				/* parameters */
				List<FormalParam> parameters;
				parameters = method.getParameters();
				for(FormalParam param : parameters) {
					Type type = lookForType(param.getType());
					if(type == null) {
						LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
						String message = eb.classNotFoundMessage(param.getType());
						throw new LCPLException(message, param);
					}
					else {
						param.setVariableType(type);
					}
				}
				
				/* return type */
				String returnType = method.getReturnType();
				if(returnType.equals(LCPLConstants.VOID)) {
					method.setReturnTypeData(p.getNoType());
				}
				else {
					Type returnTypeData = lookForType(returnType);
					if(returnTypeData == null) {
					}
					else {
						method.setReturnTypeData(returnTypeData);
					}
				}
			}
		}
	}
	
	private void walkThroughMethodsBody() throws LCPLException {
		List<Feature> features = lcplClass.getFeatures();
		for(Feature feature : features) {
			if(feature instanceof Method) {
				Method method = ((Method)feature);
				
				List<FormalParam> parameters;
				parameters = method.getParameters();
				paramsSymbols = new LinkedHashMap<String, Variable>();
				for(FormalParam param : parameters) {
					paramsSymbols.put(param.getName(), param);
				}
				
				/* verify if is an overload method and if is in the correct form */
				Method overloadedMethod;
				overloadedMethod = getOverloadedMethod(method.getName(), method.getParent().getParentData());
				if(overloadedMethod != null) {
					List<FormalParam> overloadedParameters = overloadedMethod.getParameters();
					if(parameters.size() != overloadedParameters.size()) {
						LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
						String message = eb.overloadedMethodHasDifferentNumberOfParameters();
						throw new LCPLException(message, method);
					}
					else if(!method.getReturnType().equals(overloadedMethod.getReturnType())){
						LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
						String message = eb.returnTypeChangedInOverloadedMethod();
						throw new LCPLException(message, method);
					}
					else {
						for(int i = 0; i < parameters.size(); i ++) {
							if(!parameters.get(i).getType().equals(overloadedParameters.get(i).getType())) {
								LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
								String message = eb.parameterHasDifferentTypeInOverloadedMethod(parameters.get(i).getName());
								throw new LCPLException(message, method);
							}
						}
					}
				}
				
				/* walk through body method */
				Expression body = method.getBody();
				
				localSymbols = new LinkedHashMap<String, Variable>();
				LCPLExpressionWalker ew = new LCPLExpressionWalker(p, method, lcplClass,
												attributeSymbols, paramsSymbols, localSymbols);
				ew.walkThroughExpression(body);
				
				if(!body.getTypeData().getName().equals(method.getReturnType()) &&
						!body.getTypeData().getName().equals(LCPLConstants.VOID) && 
						!method.getReturnType().equals(LCPLConstants.VOID)) {
					if(body.getType().equals(LCPLConstants.INT) && method.getReturnType().equals(LCPLConstants.STRING)) {
						Cast cast = new Cast(method.getLineNumber(), LCPLConstants.STRING, body);
						cast.setTypeData(p.getStringType());
						method.setBody(cast);
					}
					/* Try make a cast to a parent class */
					else if(!method.getReturnType().equals(LCPLConstants.INT) && !body.getType().equals(LCPLConstants.INT)
							&& isCastToAParent((LCPLClass)method.getReturnTypeData(), (LCPLClass)body.getTypeData())) {
						Cast cast = new Cast(body.getLineNumber(), method.getReturnType(), body);
						cast.setTypeData(method.getReturnTypeData());
						method.setBody(cast);
					}
					else {
						LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
						String message = eb.canNotConvertAValueInto(body.getType(), method.getReturnType());
						throw new LCPLException(message, body);
					}
				}
			}
		}
	}
	
	private void makeASuperficialWalkThroughAttributes() throws LCPLException {
		List<Feature> features = lcplClass.getFeatures();
		for(Feature feature : features) {
			if(feature instanceof Attribute) {
				Attribute attribute = (Attribute)feature;
				
				Type typeData;
				typeData = lookForType(attribute.getType());
				if(typeData == null) {
					LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
					String message = eb.classNotFoundMessage(attribute.getType());
					throw new LCPLException(message, attribute);
				}
				else {
					attribute.setTypeData(typeData);
				}
				
				/* add attribute or throw exception if allready exists */
				Map<String, Variable> thisClassAttributes = attributeSymbols.get(lcplClass.getName());
				if(thisClassAttributes.containsKey(attribute.getName())) {
					String message = "";
					message += "An attribute with the same name already exists in class ";
					message += lcplClass.getName();
					message += " : ";
					message += attribute.getName();
					throw new LCPLException(message, attribute);
				}
				thisClassAttributes.put(attribute.getName(), attribute);
			}
		}
	}
	
	private void walkThroughAttributes() throws LCPLException {
		List<Feature> features = lcplClass.getFeatures();
		for(Feature feature : features) {
			if(feature instanceof Attribute) {
				Attribute attribute = (Attribute)feature;
					
				if(isRedefined(attribute.getName(), lcplClass.getParentData())) {
					String message = "";
					message += "Attribute ";
					message += attribute.getName();
					message += " is redefined.";
					throw new LCPLException(message, lcplClass);
				}
				
				Expression init = attribute.getInit();

				if(init != null) {
					FormalParam attrInitSelf = new FormalParam(LCPLConstants.SELF, attribute.getType());
					attrInitSelf.setVariableType(lcplClass);
					attribute.setAttrInitSelf(attrInitSelf);
					
					LCPLExpressionWalker ew = new LCPLExpressionWalker(p, attribute, lcplClass, 
							attributeSymbols, paramsSymbols, localSymbols);
					Expression result = ew.walkThroughExpression(init);
					
					Type typeData = attribute.getTypeData();

					if(!result.getType().equals(attribute.getType())) {
						
						/* try to make cast from Int to String */
						if(result.getType().equals(LCPLConstants.INT) && attribute.getType().equals(LCPLConstants.STRING)) {
							Cast cast = new Cast(attribute.getLineNumber(), LCPLConstants.STRING, result);
							cast.setTypeData(p.getStringType());
							attribute.setInit(cast);
						}
						/* Try make a cast to a parent class */
						else if(isCastToAParent((LCPLClass)typeData, (LCPLClass)init.getTypeData())) {
							Cast cast = new Cast(init.getLineNumber(), typeData.getName(), init);
							cast.setTypeData(typeData);
							
							attribute.setInit(cast);
						}
					}
					else {
						attribute.setInit(result);
					}
				}
			}
		}
	}
	
	/*
	 * Method that searches for a certain type and returns it or null. 
	 */
	private Type lookForType(String type) {
		if(type.equals(p.getIntType().getName())) {
			return p.getIntType();
		}
		
		for(LCPLClass lcplClass : p.getClasses()) {
			if(lcplClass.getName().equals(type)) {
				return lcplClass;
			}
		}
		
		return null;
	}
	
	/*
	 * Method that verifies if between two objects can be done a cast.
	 */
	private boolean isCastToAParent(LCPLClass variable, LCPLClass expression) {
		if(expression.getName().equals(LCPLConstants.OBJECT)) {
			return false;
		}

		if(expression.getParent().equals(variable.getName())) {
			return true;
		}
		
		return isCastToAParent(variable, expression.getParentData());
	}

	/*
	 * Methods that verifies if an attribute is redefined in a child class.
	 */
	private boolean isRedefined(String attributeName, LCPLClass lcplCLass) {
		if(lcplCLass.getName().equals(LCPLConstants.OBJECT ) || 
				lcplCLass.getName().equals(LCPLConstants.IO)) {
			return false;
		}
		
		Map<String, Variable> attributes = attributeSymbols.get(lcplCLass.getName());
		if(attributes.containsKey(attributeName)) {
			return true;
		}
		
		return isRedefined(attributeName, lcplCLass.getParentData());
	}

	/*
	 * Method that verifies if a method is overloaded and returns it.
	 */
	private Method getOverloadedMethod(String methodName, LCPLClass parrentClass) {
		if(parrentClass == null) {
			return null;
		}
		
		for(Feature feature : parrentClass.getFeatures()) {
			if(feature instanceof Method) { 
				if(((Method) feature).getName().equals(methodName)) {
					return (Method)feature;
				}
			}
		}
		
		return getOverloadedMethod(methodName, parrentClass.getParentData());
	}
	
}

