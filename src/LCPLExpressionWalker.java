import java.util.*;

import ro.pub.cs.lcpl.*;

/*
 * Class that walks through expressions.
 */
public class LCPLExpressionWalker {

	private Program p;
	private Feature currentFeature; //method or attribute(for self)
	private LCPLClass currentClass;
	
	Map<String, Map<String, Variable>> attributeSymbols;
	Map<String, Variable> paramsSymbols;
	Map<String, Variable> localSymbols;
	List<Map<String, Variable>> ifAndWhileSymbols;
	
	public LCPLExpressionWalker(Program p, Feature currentFuture, LCPLClass currentClass, 
								Map<String, Map<String, Variable>> attributeSymbols, 
								Map<String, Variable> paramsSymbols, 
								Map<String, Variable> localSymbols) {
		
		this.p 					= p;
		this.currentFeature 	= currentFuture;
		this.currentClass		= currentClass;
		
		this.attributeSymbols 	= attributeSymbols;
		this.paramsSymbols 		= paramsSymbols;
		this.localSymbols		= localSymbols;
		this.ifAndWhileSymbols  = new LinkedList<Map<String, Variable>>();
	}
	
	/*
	 * Method that walks through expression recursively.
	 */
	public Expression walkThroughExpression(Expression expression) throws LCPLException {
		
		if(expression instanceof Block) {
			walkThroughBlock(expression);
		}
		else if(expression instanceof LocalDefinition) {
			walkThroughLocalDefinition(expression);
		}
		else if(expression instanceof BaseDispatch) {
			walkThroughBaseDispatch(expression);
		}
		else if(expression instanceof IfStatement) {
			walkThroughIfStatement(expression);
		}
		else if(expression instanceof WhileStatement) {
			walkThroughWhileStatement(expression);
		}
		else if(expression instanceof Cast) {
			walkThroughCast(expression);
		}
		else if(expression instanceof NewObject) {
			walkThroughNewObject(expression);
		}
		else if(expression instanceof SubString) {
			walkThroughSubString(expression);
		}
		else if(expression instanceof Assignment) {
			walkThroughAssignment(expression);
		}
		else if(expression instanceof Addition) {
			walkThroughAddition(expression);
		}
		else if(expression instanceof Multiplication ||
				expression instanceof Division ||
				expression instanceof Subtraction ||
				expression instanceof LessThan ||
				expression instanceof LessThanEqual) {
			walkThroughBinaryOp(expression);
		}
		else if(expression instanceof UnaryOp) {
			walkThroughUnaryOp(expression);
		}
		else if(expression instanceof EqualComparison) {
			walkThroughEqualComparison(expression);
		}
		else if(expression instanceof Symbol) {
			walkThroughSymbol(expression);
		}
		else if(expression instanceof IntConstant) {
			walkThroughIntConstant(expression);
		}
		else if(expression instanceof StringConstant) {
			walkThroughStringConstant(expression);
		}
		else if(expression instanceof VoidConstant) {
			walkThroughVoidConstant(expression);
		}
			
		return expression; 
	}
	
	private void walkThroughBlock(Expression expression) throws LCPLException {

		Block block = ((Block)expression);
		
		Expression lastExpression = null;
		
		List<Expression> expressions = block.getExpressions();
		for(Expression new_expression : expressions) {
			lastExpression = walkThroughExpression(new_expression);
		}
		
		if(lastExpression == null) {
			block.setType("(none)");
			block.setTypeData(p.getNoType());
		}
		else if(lastExpression instanceof LocalDefinition){
			block.setType(((LocalDefinition)lastExpression).getScope().getType());
			block.setTypeData((((LocalDefinition)lastExpression).getScope().getTypeData()));
		}
		else {
			block.setType(lastExpression.getType());
			block.setTypeData(lastExpression.getTypeData());
		}
	}
	
	private void walkThroughLocalDefinition(Expression expression) throws LCPLException {
		LocalDefinition localDefinition = (LocalDefinition)expression;
		
		Type typeData;
		typeData = lookForType(localDefinition.getType());
		if(typeData == null) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.classNotFoundMessage(localDefinition.getType());
			throw new LCPLException(message, localDefinition);
		}
		
		localDefinition.setVariableType(typeData);
		
		Expression init;
		init = localDefinition.getInit();
		if(init != null) {
			walkThroughExpression(init);
			
			if(!init.getType().equals(localDefinition.getVariableType().getName())) {
				if(init.getType().equals(LCPLConstants.INT) && localDefinition.getType().equals(LCPLConstants.STRING)) {
					Cast cast = new Cast(localDefinition.getLineNumber(), LCPLConstants.STRING, init);
					cast.setTypeData(p.getStringType());
					localDefinition.setInit(cast);
				}
				/* Try make a cast to a parent class */
				else if(isCastToAParent((LCPLClass)typeData, (LCPLClass)init.getTypeData())) {
					Cast cast = new Cast(init.getLineNumber(), typeData.getName(), init);
					cast.setTypeData(typeData);
					
					localDefinition.setInit(cast);
				}
			}
			else {
				localDefinition.setInit(init);
			}
		}
		
		/* save the symbol in current context */
		if(!ifAndWhileSymbols.isEmpty()) {
			Map<String, Variable> rightScope = ifAndWhileSymbols.get(ifAndWhileSymbols.size() - 1);
			rightScope.put(localDefinition.getName(), localDefinition);
		}
		else {
			localSymbols.put(localDefinition.getName(), localDefinition);
		}
		
		/* continue recursively on the scope of the local definition which is an expression  */
		Expression scope = localDefinition.getScope();
		walkThroughExpression(scope);
		
		localDefinition.setTypeData(scope.getTypeData());
	}
	
	private void walkThroughBaseDispatch(Expression expression) throws LCPLException {
		BaseDispatch baseDispatch = (BaseDispatch)expression;
		
		Type classOfObject = null;
		
		Expression object = baseDispatch.getObject();
		/* if is call on a local method (on self)  */
		if(object == null) {
			classOfObject = currentClass;
			
			Symbol newObject = new Symbol(LCPLConstants.ZERO, LCPLConstants.SELF);
			newObject.setType(currentClass.getName());
			newObject.setTypeData(currentClass);

			if(currentFeature instanceof Method) {
				newObject.setVariable(((Method)currentFeature).getSelf());
				newObject.setLineNumber(((Method)currentFeature).getLineNumber());
			}
			else if(currentFeature instanceof Attribute) {
				newObject.setVariable(((Attribute)currentFeature).getAttrInitSelf());
				newObject.setLineNumber(((Attribute)currentFeature).getLineNumber());
			}
			
			baseDispatch.setObject(newObject);
		}
		/* if object is an expression, then evaluate expression */
		else {
			walkThroughExpression(object);
			
			/* if is a static dispatch then set some additional parameters */
			if(baseDispatch instanceof StaticDispatch){
				String type = ((StaticDispatch)baseDispatch).getType();
				Type typeData = lookForType(type);
				if(typeData == null) {
					LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
					String message = eb.classNotFoundMessage(type);
					throw new LCPLException(message, baseDispatch);
				}
				else if(!type.equals(object.getType()) && !isCastToAParent((LCPLClass)typeData, (LCPLClass)object.getTypeData())) {
					LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
					String message = eb.canNotConvertInStaticDispatchMessage(object.getType(), type);
					throw new LCPLException(message, baseDispatch);
				}
				((StaticDispatch)baseDispatch).setSelfType(typeData);
				classOfObject = typeData;
			}
			else {
				classOfObject = object.getTypeData();
			}
		}
		
		Method method = getMethodFromClass(classOfObject, baseDispatch.getName());
		
		/* if method not found */
		if(method == null) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.methodNotFoundMessage(baseDispatch.getName(), classOfObject.getName());
			throw new LCPLException(message, baseDispatch);
		}
		
		/* if not enough arguments for a method call */
		if(baseDispatch.getArguments().size() < method.getParameters().size()) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.notEnoughArgumentsInMethodCall(baseDispatch.getName());
			throw new LCPLException(message, baseDispatch);
		}
		
		/* if too many arguments for a method call */
		if(baseDispatch.getArguments().size() > method.getParameters().size()) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.tooManyArgumentsInMethodCall(baseDispatch.getName());
			throw new LCPLException(message, baseDispatch);
		}
		
		baseDispatch.setMethod(method);
		
		
		/* set the type of the dispatch */
		if(baseDispatch instanceof StaticDispatch) {
			((StaticDispatch)baseDispatch).setType(((StaticDispatch)baseDispatch).getSelfType().getName());
		}
		else {
			baseDispatch.setType(method.getReturnType());
		}
		baseDispatch.setTypeData(method.getReturnTypeData());
		
		/* set arguments, make cast if necessary, or throw exception */
		List<Expression> args = baseDispatch.getArguments();
		Expression argument = null;
		for(int i = 0; i < args.size(); i++) {
			argument = args.get(i);
			walkThroughExpression(argument);
			
			FormalParam methodParam = method.getParameters().get(i);
			if(!argument.getTypeData().getName().equals(methodParam.getType())) {
				if(argument.getTypeData().getName().equals(LCPLConstants.INT) && methodParam.getType().equals(LCPLConstants.STRING)) {
					Cast cast = new Cast(argument.getLineNumber(), LCPLConstants.STRING, argument);
					cast.setTypeData(p.getStringType());
					
					args.remove(i);
					args.add(i, cast);
				}
				else if(argument.getType().equals(LCPLConstants.VOID)) {
					
				}
				/* Try make a cast to a parent class */
				else if(!methodParam.getType().equals(LCPLConstants.INT) &&
						!argument.getType().equals(LCPLConstants.INT) &&
						!argument.getType().equals(LCPLConstants.NONE) &&
						isCastToAParent((LCPLClass)methodParam.getVariableType(), (LCPLClass)argument.getTypeData())) {
					Cast cast = new Cast(argument.getLineNumber(), ((LCPLClass)methodParam.getVariableType()).getName(), argument);
					cast.setTypeData((LCPLClass)methodParam.getVariableType());
					
					args.remove(i);
					args.add(i, cast);
				}
				else {
					LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
					String message = eb.canNotConvertAValueInto(argument.getType(), methodParam.getType());
					throw new LCPLException(message, baseDispatch);
				}
			}
		}
	}
	
	private void walkThroughIfStatement(Expression expression) throws LCPLException {
		IfStatement ifStatement = (IfStatement)expression;
		
		Expression condition = ifStatement.getCondition();
		walkThroughExpression(condition);
		
		/* if condition type is not int, throw an exception */
		if(!condition.getType().equals(LCPLConstants.INT)) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.conditionMustBeInt(LCPLConstants.IF);
			throw new LCPLException(message, ifStatement);
		}
		
		/* every if and while will have its own list of symbols */ 
		Map<String, Variable> listOfSymbolsForIf = new LinkedHashMap<String, Variable>();
		ifAndWhileSymbols.add(listOfSymbolsForIf);
		
		Expression ifExpr = ifStatement.getIfExpr();
		walkThroughExpression(ifExpr);
		
		ifAndWhileSymbols.remove(ifAndWhileSymbols.size() - 1);
		
		Map<String, Variable> listOfSymbolsForThen = new LinkedHashMap<String, Variable>();
		ifAndWhileSymbols.add(listOfSymbolsForThen);
		
		Expression thenExpr = ifStatement.getThenExpr();
		walkThroughExpression(thenExpr);
		
		ifAndWhileSymbols.remove(ifAndWhileSymbols.size() - 1);
		
		Type typeData;
		if(thenExpr == null) {
			typeData = p.getNoType();
		}
		else if(!ifExpr.getType().equals(thenExpr.getType())) {
			
			/* aici facem un mare hack care probabil trebuie corectat !!!!!!!!!!!!!!!!! */
			/* se pare ca nu e hack. Cred ca este exact ce trebuie */
			if(ifExpr.getType().equals(LCPLConstants.VOID)) {
				typeData = thenExpr.getTypeData();
			}
			else if(thenExpr.getType().equals(LCPLConstants.VOID)){
				typeData = ifExpr.getTypeData();
			}
			else if(thenExpr.getType().equals(LCPLConstants.NONE) || ifExpr.getType().equals(LCPLConstants.NONE)) {
				typeData = p.getNoType();
			}
			else if((ifExpr.getType().equals(LCPLConstants.INT) && thenExpr.getType().equals(LCPLConstants.STRING)) ||
					(ifExpr.getType().equals(LCPLConstants.STRING) && thenExpr.getType().equals(LCPLConstants.INT)) ) {
				typeData =p.getNoType();
			}
			else if(isCastToAParent((LCPLClass)ifExpr.getTypeData(), (LCPLClass)thenExpr.getTypeData())) {
				typeData = ifExpr.getTypeData();
			}
			else if(isCastToAParent((LCPLClass)thenExpr.getTypeData(), (LCPLClass)ifExpr.getTypeData())) {
				typeData = thenExpr.getTypeData();
			}
			else {
				typeData = p.getNoType();
			}
		}
		else {
			typeData = ifExpr.getTypeData();
		}
		
		/* set type of if block */ 
		ifStatement.setType(typeData.getName());
		ifStatement.setTypeData(typeData);
	}
	
	private void walkThroughWhileStatement(Expression expression) throws LCPLException {
		WhileStatement whileStatement = (WhileStatement)expression;
		
		Expression condition = whileStatement.getCondition();
		walkThroughExpression(condition);
		
		/* if condition type is not int, throw an exception */
		if(!condition.getType().equals(LCPLConstants.INT)) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.conditionMustBeInt(LCPLConstants.WHILE);
			throw new LCPLException(message, whileStatement);
		}
		
		/* every if and while will have its own list of symbols */ 
		Map<String, Variable> listOfSymbolsForWhile = new LinkedHashMap<String, Variable>();
		ifAndWhileSymbols.add(listOfSymbolsForWhile);
		
		Expression whileExpr = whileStatement.getLoopBody();
		walkThroughExpression(whileExpr);
		
		ifAndWhileSymbols.remove(ifAndWhileSymbols.size() - 1);
	
		/* set type of while block */
		whileStatement.setType(p.getNoType().getName());
		whileStatement.setTypeData(p.getNoType());
	}
	
	private void walkThroughCast(Expression expression) throws LCPLException {
		Cast cast = (Cast)expression;
		
		String type = cast.getType();
		Type typeData;
		typeData = lookForType(type);
		if(typeData == null) {
		}
		else {
			Expression e;
			e = cast.getE1();
			walkThroughExpression(e);
			
			if(!e.getType().equals(type)) {
			}
		}
		
		cast.setType(type);
		cast.setTypeData(typeData);
	}
	
	private void walkThroughNewObject(Expression expression) throws LCPLException {
		NewObject newObject = (NewObject)expression;
		
		String type = newObject.getType();
		Type typeData = lookForType(type);
		if(typeData == null) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.classNotFoundMessage(type);
			throw new LCPLException(message, newObject);
		}
		else if(typeData.getName().equals(LCPLConstants.INT)) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.illegalConstruction(type);
			throw new LCPLException(message, newObject);
		}
		else {
			newObject.setTypeData(typeData);
		}
	}
	
	private void walkThroughSubString(Expression expression) throws LCPLException {
		SubString subString = (SubString)expression;
		
		Expression eString = subString.getStringExpr();
		walkThroughExpression(eString);
		
		Expression eStart = subString.getStartPosition();
		walkThroughExpression(eStart);
		
		Expression eEnd = subString.getEndPosition();
		walkThroughExpression(eEnd);
		
		if(!eStart.getType().equals(LCPLConstants.INT) || !eEnd.getType().equals(LCPLConstants.INT)) {
		}
		
		subString.setType(LCPLConstants.STRING);
		subString.setTypeData(p.getStringType());
	}
	
	private void walkThroughAssignment(Expression expression) throws LCPLException {
		Assignment assignament = (Assignment)expression;
		
		Variable variable;
		/* set the type of the assignment symbol */
		String symbol = assignament.getSymbol();
		String[] strings = symbol.split("\\.");
		if(strings.length == 2) {
			symbol = strings[1];
			assignament.setSymbol(symbol);
			variable = getSymbolFromClass(currentClass, symbol);
		}
		else {
			variable = getSymbol(currentClass, symbol);
		}
		
		
		if(variable == null) {
			String message = "";
			message += "Attribute ";
			message += symbol;
			message += " not found in class ";
			message += currentClass.getName();
			throw new LCPLException(message, assignament);
		}
		else {
			assignament.setSymbolData(variable);
		}
		
		Type typeData = getTypeOfVariable(variable);
		
		/* set the assignment expression */
		Expression e = assignament.getE1();
		walkThroughExpression(e);
		
		if(!e.getType().equals(typeData.getName())) {
			if(e.getType().equals(LCPLConstants.INT) && typeData.getName().equals(LCPLConstants.STRING)) {
				Cast cast = new Cast(assignament.getLineNumber(), LCPLConstants.STRING, e);
				cast.setTypeData(p.getStringType());
				assignament.setE1(cast);
			}
			/* Try make a cast to a parent class */
			else if(isCastToAParent((LCPLClass)typeData, (LCPLClass)e.getTypeData())) {
				Cast cast = new Cast(e.getLineNumber(), typeData.getName(), e);
				cast.setTypeData(typeData);
				
				assignament.setE1(cast);
			}
			else {
				LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
				String message = eb.canNotConvertAValueInto(typeData.getName(), e.getType());
				throw new LCPLException(message, e);
			}
		}
		else {
			e.setType(typeData.getName());
			e.setTypeData(typeData);
		}
		
		assignament.setType(typeData.getName());
		assignament.setTypeData(typeData);
	}
	
	private void walkThroughAddition(Expression expression) throws LCPLException {
		Addition addition = (Addition)expression;
		
		Expression e1 = addition.getE1();
		walkThroughExpression(e1);
		
		Expression e2 = addition.getE2();
		walkThroughExpression(e2);
		
		/* if different types of expressions try to make cast or through an exception */;
		if(!e1.getTypeData().getName().equals(e2.getTypeData().getName())) {
			if(e1.getType().equals(LCPLConstants.INT) && e2.getType().equals(LCPLConstants.STRING)) {
				Cast cast = new Cast(addition.getLineNumber(), LCPLConstants.STRING, e1);
				cast.setTypeData(p.getStringType());
				addition.setE1(cast);
				
				addition.setType(LCPLConstants.STRING);
				addition.setTypeData(p.getStringType());
			}
			else if(e1.getTypeData().getName().equals(LCPLConstants.STRING) && e2.getTypeData().getName().equals(LCPLConstants.INT)) {
				Cast cast = new Cast(addition.getLineNumber(), LCPLConstants.STRING, e2);
				cast.setTypeData(p.getStringType());
				addition.setE2(cast);
				
				addition.setType(LCPLConstants.STRING);
				addition.setTypeData(p.getStringType());
			}
			else if(!e1.getTypeData().getName().equals(LCPLConstants.INT) && !e1.getTypeData().getName().equals(LCPLConstants.STRING)){
				if(e2.getType().equals(LCPLConstants.STRING)) {
					LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
					String message = eb.canNotConvertAValueInto(e1.getType(), LCPLConstants.STRING);
					throw new LCPLException(message, e1);
				}
				else {
					LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
					String message = eb.cannotConvertAdditionExpression();
					throw new LCPLException(message, e1);
				}
			}
			else if(!e2.getTypeData().getName().equals(LCPLConstants.INT) && !e2.getTypeData().getName().equals(LCPLConstants.STRING)){
				if(e1.getType().equals(LCPLConstants.STRING)) {
					LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
					String message = eb.canNotConvertAValueInto(e2.getType(), LCPLConstants.STRING);
					throw new LCPLException(message, e2);
				}
				else {
					LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
					String message = eb.cannotConvertAdditionExpression();
					throw new LCPLException(message, e2);
				}
			}
		}
		/* same types of expressions */ 
		else {
			addition.setType(e1.getType());
			addition.setTypeData(e1.getTypeData());
		}
	}
		
	private void walkThroughBinaryOp(Expression expression) throws LCPLException {
		BinaryOp binaryOp = (BinaryOp)expression;
		
		Expression e1 = binaryOp.getE1();
		walkThroughExpression(e1);
		
		Expression e2 = binaryOp.getE2();
		walkThroughExpression(e2);
		
		if(!e1.getType().equals(LCPLConstants.INT)) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.canNotConvertAValueInto(e1.getType(), LCPLConstants.INT);
			throw new LCPLException(message, binaryOp);
		}
		else if( !e2.getType().equals(LCPLConstants.INT)) {
			LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
			String message = eb.canNotConvertAValueInto(e2.getType(), LCPLConstants.INT);
			throw new LCPLException(message, binaryOp);
		}
		else {
			binaryOp.setType(LCPLConstants.INT);
			binaryOp.setTypeData(p.getIntType());
		}
	}
	
	private void walkThroughUnaryOp(Expression expression) throws LCPLException {
		UnaryOp unaryOp = (UnaryOp)expression;
		
		Expression e = unaryOp.getE1();
		walkThroughExpression(e);
		
		if(!e.getType().equals(LCPLConstants.INT)) {

		}
		else {
			unaryOp.setType(LCPLConstants.INT);
			unaryOp.setTypeData(p.getIntType());
		}
	}
	
	private void walkThroughEqualComparison(Expression expression) throws LCPLException {
		EqualComparison equalComparison = (EqualComparison)expression;
		
		Expression e1 = equalComparison.getE1();
		walkThroughExpression(e1);
		
		Expression e2 = equalComparison.getE2();
		walkThroughExpression(e2);
		
		if(!e1.getType().equals(e2.getType())) {
			if(e1.getType().equals(LCPLConstants.INT) && e2.getType().equals(LCPLConstants.STRING)) {
				Cast cast = new Cast(equalComparison.getLineNumber(), LCPLConstants.STRING, e1);
				cast.setTypeData(p.getStringType());
				equalComparison.setE1(cast);
			}
			else if(e1.getType().equals(LCPLConstants.STRING) && e2.getType().equals(LCPLConstants.INT)) {
				Cast cast = new Cast(equalComparison.getLineNumber(), LCPLConstants.STRING, e2);
				cast.setTypeData(p.getStringType());
				equalComparison.setE2(cast);
			}
			else if(e1.getType().equals(LCPLConstants.INT) || e2.getType().equals(LCPLConstants.INT)) {
				LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
				String message = eb.invalidTypeOfParameters(LCPLConstants.EQUAL);
				throw new LCPLException(message, equalComparison);
			}
			else if(!e1.getType().equals(LCPLConstants.VOID) && isCastToAParent(p.getObjectType(), (LCPLClass)e1.getTypeData())) {
				Cast cast = new Cast(e1.getLineNumber(), p.getObjectType().getName(), e1);
				cast.setTypeData(p.getObjectType());
				
				equalComparison.setE1(cast);
			}
			else if(!e2.getType().equals(LCPLConstants.VOID) && isCastToAParent(p.getObjectType(), (LCPLClass)e2.getTypeData())) {
				Cast cast = new Cast(e2.getLineNumber(), p.getObjectType().getName(), e2);
				cast.setTypeData(p.getObjectType());
				
				equalComparison.setE2(cast);
			}
		}
		
		equalComparison.setType(LCPLConstants.INT);
		equalComparison.setTypeData(p.getIntType());
	}
		
	private void walkThroughSymbol(Expression expression) throws LCPLException {
		Symbol symbol = (Symbol)expression;
		
		Variable variable;
		variable = getSymbol(currentClass, symbol.getName());
		if(variable == null) {
			if(symbol.getName().equals(LCPLConstants.SELF)) {
				symbol.setType(currentClass.getName());
				symbol.setTypeData(currentClass);
				symbol.setVariable(((Method)currentFeature).getSelf());
			}
			else {
				LCPLExceptionsBuilder eb = new LCPLExceptionsBuilder();
				String message = eb.attributeNotFoundInClass(symbol.getName(), currentClass.getName());
				throw new LCPLException(message, symbol);
			}
		}
		else {
			symbol.setVariable(variable);
			
			Type type = getTypeOfVariable(variable);
			symbol.setType(type.getName());
			symbol.setTypeData(type);
		}
	}
	
	private void walkThroughIntConstant(Expression expression) throws LCPLException {
		IntConstant intConstant = (IntConstant)expression;
		intConstant.setType(p.getIntType().getName());
		intConstant.setTypeData(p.getIntType());
	}
	
	private void walkThroughStringConstant(Expression expression) throws LCPLException {
		StringConstant stringConstant = (StringConstant)expression;
		stringConstant.setType(p.getStringType().getName());
		stringConstant.setTypeData(p.getStringType());
	}
	
	private void walkThroughVoidConstant(Expression expression) throws LCPLException {
		VoidConstant voidConstant = (VoidConstant)expression;
		
		voidConstant.setTypeData(p.getNullType());
	}
		
	private Method getMethodFromClass(Type currentClass, String methodName) {
		LCPLClass wantedClass = (LCPLClass)currentClass;
		if(currentClass == null)
			return null;
		
		/* find the class that is looking for */
		/*for(LCPLClass lcplClass : p.getClasses()) {
			if(currentClass.getName().equals(lcplClass.getName())) {
				wantedClass = lcplClass;
				break;
			}
		}*/
		/* look for the wanted future in the wanted class */
		for(Feature feature : wantedClass.getFeatures()) {
			if(feature instanceof Method && ((Method)feature).getName().equals(methodName)) {
				return (Method)feature;
			}
		}
		
		/* if is not in this class then look recursively in his parent class */
		return getMethodFromClass(wantedClass.getParentData(), methodName);
	}
	
	/*
	 * This method gets as parameter a string representing a symbol and searches in the right order for it
	 * in the data structures of symbols. 
	 */
	private Variable getSymbol(LCPLClass lcplClass, String symbol) {
		if(!ifAndWhileSymbols.isEmpty()) {
			for(int i = ifAndWhileSymbols.size() - 1; i >= 0; i--) {
				Map<String, Variable> symbols = ifAndWhileSymbols.get(i);
				if(symbols.containsKey(symbol)) {
					return symbols.get(symbol);
				}
			}
		}
		
		if(localSymbols.containsKey(symbol)) {
			return localSymbols.get(symbol);
		}
		
		if(paramsSymbols.containsKey(symbol)) {
			return paramsSymbols.get(symbol);
		}
			
		return getSymbolFromClass(lcplClass, symbol);
	}
	
	private Variable getSymbolFromClass(LCPLClass lcplClass, String symbol) {
		if(lcplClass.getName().equals(LCPLConstants.OBJECT) || lcplClass.getName().equals(LCPLConstants.IO)) {
			return null;
		}
		
		Map<String, Variable> thisClassAttributes = attributeSymbols.get(lcplClass.getName());
		if(thisClassAttributes.containsKey(symbol)) {
			return thisClassAttributes.get(symbol);
		}
		
		return getSymbolFromClass(lcplClass.getParentData(), symbol);
	}
	
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
	
	private Type getTypeOfVariable(Variable variable) {
		if(variable instanceof Attribute) {
			return ((Attribute)variable).getTypeData();
		}
		else if(variable instanceof FormalParam) {
			return ((FormalParam)variable).getVariableType();
		}
		else if(variable instanceof LocalDefinition) {
			return ((LocalDefinition)variable).getVariableType();
		}
		
		return null;
	}
	
	private boolean isCastToAParent(LCPLClass parrent, LCPLClass child) {
		if(parrent == null || child.getParent() == null) {
			return false;
		}

		if(child.getParent().equals(parrent.getName())) {
			return true;
		}
		
		return isCastToAParent(parrent, child.getParentData());
	}
}
