import syntaxtree.*;
import visitor.*;
import java.util.*;

// second visitor in order to perform typechecking
// symbol table is already filled by the first visitor
// continue at same pattern with 1st visitor using the symboltable, curclass and curmethod 
public class TCVisitor extends GJDepthFirst<String, StoreTypes>{
	SymbolTable symtable;
	ClassType curclass;
	MethodType curmethod;

	public TCVisitor(SymbolTable symtable){
		this.symtable = symtable;
		this.curclass = null;
		this.curmethod = null;
	}

	/**
	* f0 -> "class"
	* f1 -> Identifier()
	* f2 -> "{"
	* f3 -> "public"
	* f4 -> "static"
	* f5 -> "void"
	* f6 -> "main"
	* f7 -> "("
	* f8 -> "String"
	* f9 -> "["
	* f10 -> "]"
	* f11 -> Identifier()
	* f12 -> ")"
	* f13 -> "{"
	* f14 -> ( VarDeclaration() )*
	* f15 -> ( Statement() )*
	* f16 -> "}"
	* f17 -> "}"
	*/
	public String visit(MainClass n, StoreTypes argu) throws Exception {
		curclass = symtable.getClass(n.f1.accept(this,argu));
		curmethod = curclass.getMethod(n.f6.toString());

		n.f11.accept(this, argu);
		n.f14.accept(this, argu);
		n.f15.accept(this, argu);

		// end of declaration, initialize variables
		curclass = null;
		curmethod = null;
		return null;
	}


	/**
	* f0 -> "class"
	* f1 -> Identifier()
	* f2 -> "{"
	* f3 -> ( VarDeclaration() )*
	* f4 -> ( MethodDeclaration() )*
	* f5 -> "}"
	*/
	public String visit(ClassDeclaration n, StoreTypes argu) throws Exception {
		curclass = symtable.getClass(n.f1.accept(this, argu));
		n.f3.accept(this, argu);
		n.f4.accept(this, argu);
		
		// end of declaration, initialize variables
		curclass = null;
		curmethod = null;
		return null;
	}

	/**
	* f0 -> "class"
	* f1 -> Identifier()
	* f2 -> "extends"
	* f3 -> Identifier()
	* f4 -> "{"
	* f5 -> ( VarDeclaration() )*
	* f6 -> ( MethodDeclaration() )*
	* f7 -> "}"
	*/
	public String visit(ClassExtendsDeclaration n, StoreTypes argu) throws Exception {
		curclass = symtable.getClass(n.f1.accept(this, argu));
		
		n.f5.accept(this, argu);
		n.f6.accept(this, argu);
		
		// end of declaration, initialize variables
		curclass = null;
		curmethod = null;
		return null;
	}

	/**
	* f0 -> "public"
	* f1 -> Type()
	* f2 -> Identifier()
	* f3 -> "("
	* f4 -> ( FormalParameterList() )?
	* f5 -> ")"
	* f6 -> "{"
	* f7 -> ( VarDeclaration() )*
	* f8 -> ( Statement() )*
	* f9 -> "return"
	* f10 -> Expression()
	* f11 -> ";"
	* f12 -> "}"
	*/
	public String visit(MethodDeclaration n, StoreTypes argu) throws Exception {
		// update curmethod
		curmethod = curclass.getMethod(n.f2.accept(this, argu));
		n.f4.accept(this, argu);
		n.f7.accept(this, argu);
		n.f8.accept(this, argu);
		// check return type to match with methods type
		String retType = n.f10.accept(this, argu);
		boolean typefound = false;
		String methodtype = curmethod.getType();
		if (methodtype.equals(retType))
			typefound = true;
		else{
			ClassType tmpClass;
			tmpClass = symtable.getClass(retType);
			if (tmpClass != null){
				String parentName = tmpClass.getParentName();
				while (parentName != null){
					if (methodtype.equals(parentName)){
						typefound = true;
						break;
					}
					tmpClass = symtable.getClass(tmpClass.parentName);
					parentName = tmpClass.getParentName();
				}
			}
		}
		
		if (!typefound)
			throw new Exception("Return type doesn't match with function's return type");
		// end of method decl, initialize variable
		curmethod = null;

		return null;
	}

	/**
	* f0 -> Type()
	* f1 -> Identifier()
	* f2 -> ";"
	*/
	public String visit(VarDeclaration n, StoreTypes argu) throws Exception {
		// check for var decls slipped from first visitor, eg forward declaration
		String type = n.f0.accept(this, argu);
		if (!type.equals("int") && !type.equals("int[]") && !type.equals("boolean") && !type.equals(curclass.getName())){
			if (symtable.getClass(type) == null)
				throw new Exception("VarDecl::Variable doesn't have a declared type");
		}
		
		return null;
	}


	/**
	* f0 -> "System.out.println"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> ";"
	*/
	public String visit(PrintStatement n, StoreTypes argu) throws Exception {
		String type = n.f2.accept(this, argu);
		
		// type must be only int or boolean
		if (!type.equals("int") && !type.equals("boolean"))
			throw new Exception("PrintStatement::Can only have Int or Boolean type");
		return null;
	}

	
	/**
	* f0 -> Identifier()
	* f1 -> "="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public String visit(AssignmentStatement n, StoreTypes argu) throws Exception {
		String name = n.f0.accept(this, argu);
		
		// check to identify variable
		VariableType tmpvar;
		boolean variableFound = false;
		if ((tmpvar = curmethod.findVar(name)) != null)
			variableFound = true;
		else{
			if ((tmpvar = curclass.getVar(name)) != null){
				variableFound = true;
			}
			else{
				String parentName = curclass.getParentName();
				ClassType tmpClass;
				while (parentName != null){
					tmpClass = symtable.getClass(parentName);
					if ((tmpvar = tmpClass.getVar(name)) != null){
						variableFound = true;
						break;
					}
					parentName = tmpClass.getParentName();
				}
			}
		}

		if (!variableFound)
			throw new Exception("Assignment::Variable doesn't have a declared type");
		
		// check if types are same
		if (symtable.checkForType(tmpvar.getType(), n.f2.accept(this, argu)) == false)
			throw new Exception("Types dont match");


		// String type1 = tmpvar.getType();
		// String type2 = n.f2.accept(this, argu);
		// boolean typeFound = false;
		// if (type1.equals(type2))
		// 	typeFound = true;
		// else{
		// 	ClassType tmpclass = symtable.getClass(type2);
		// 	if (tmpclass != null){
		// 		String parentName = tmpclass.getParentName();
		// 		while (parentName != null){
		// 			if (type1.equals(parentName)){
		// 				typeFound = true;
		// 				break;
		// 			}
		// 			tmpclass = symtable.getClass(parentName);
		// 			parentName = tmpclass.getParentName();
		// 		}
		// 	}
		// }

		// if (!typeFound)
		// 	throw new Exception("Types dont match");

		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "["
	* f2 -> Expression()
	* f3 -> "]"
	* f4 -> "="
	* f5 -> Expression()
	* f6 -> ";"
	*/
	public String visit(ArrayAssignmentStatement n, StoreTypes argu) throws Exception {
		// MiniJava supports only int arrays
		String name = n.f0.f0.toString();
		VariableType tmpvar;

		// check for variable
		boolean variableFound = false;
		if ((tmpvar = curmethod.findVar(name)) != null)
			variableFound = true;
		else{
			if ((tmpvar = curclass.getVar(name)) != null)
				variableFound = true;
			else{
				String parentName = curclass.getParentName();
				ClassType tmpClass;
				while (parentName != null){
					tmpClass = symtable.getClass(parentName);
					if ((tmpvar = tmpClass.getVar(name)) != null){
						variableFound = true;
						break;
					}
					parentName = tmpClass.getParentName();
				}
			}
		} 

		if (!variableFound)
			throw new Exception("Variable doesn't exist");

		if (!tmpvar.getType().equals("int[]"))
			throw new Exception("Int Arrays only allowed");

		if (!n.f2.accept(this, argu).equals("int"))
			throw new Exception("Int Arrays only allowed");
		
		if (!n.f5.accept(this, argu).equals("int"))
			throw new Exception("Int Arrays only allowed");
	
		return null;
	}

	/**
	* f0 -> "if"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> Statement()
	* f5 -> "else"
	* f6 -> Statement()
	*/
	public String visit(IfStatement n, StoreTypes argu) throws Exception {
		if (!n.f2.accept(this, argu).equals("boolean"))
			throw new Exception("IfStatement::Expression type must be Boolean");
		n.f4.accept(this, argu);
		n.f6.accept(this, argu);
		return null;
	}

	/**
	* f0 -> "while"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> Statement()
	*/
	public String visit(WhileStatement n, StoreTypes argu) throws Exception {
		if (!n.f2.accept(this, argu).equals("boolean"))
			throw new Exception("WhileStatement::Expression type must be Boolean");
		n.f4.accept(this, argu);
		return null;
	}

	// /**
	// * f0 -> AndExpression()
	// *       | CompareExpression()
	// *       | PlusExpression()
	// *       | MinusExpression()
	// *       | TimesExpression()
	// *       | ArrayLookup()
	// *       | ArrayLength()
	// *       | MessageSend()
	// *       | Clause()
	// */
	// public String visit(Expression n, StoreTypes argu) {
	// 	return n.f0.accept(this, argu);
	// }

	/**
	* f0 -> Clause()
	* f1 -> "&&"
	* f2 -> Clause()
	*/
	public String visit(AndExpression n, StoreTypes argu) throws Exception {
		if (!n.f0.accept(this, argu).equals("boolean") || !n.f2.accept(this, argu).equals("boolean"))
			throw new Exception("AndExpression::Error, type must be boolean");
		return "boolean";
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "<"
	* f2 -> PrimaryExpression()
	*/
	public String visit(CompareExpression n, StoreTypes argu) throws Exception {
		String type1 = n.f0.accept(this, argu);
		if (!type1.equals("int") || !type1.equals(n.f2.accept(this, argu)))
			throw new Exception("CompareExpression::Error, type must be int");
			
		return "boolean";
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "+"
	* f2 -> PrimaryExpression()
	*/
	public String visit(PlusExpression n, StoreTypes argu) throws Exception {
		String type1 = n.f0.accept(this, argu);
		if (!type1.equals("int") || !type1.equals(n.f2.accept(this, argu)))
			throw new Exception("PlusExpression::Error, type must be int");
		
		return "int";
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "-"
	* f2 -> PrimaryExpression()
	*/
	public String visit(MinusExpression n, StoreTypes argu) throws Exception {
		String type1 = n.f0.accept(this, argu);
		if (!type1.equals("int") || !type1.equals(n.f2.accept(this, argu)))
			throw new Exception("MinusExpression::Error, type must be int");

		return "int";
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "*"
	* f2 -> PrimaryExpression()
	*/
	public String visit(TimesExpression n, StoreTypes argu) throws Exception {
		String type1 = n.f0.accept(this, argu);
		if (!type1.equals("int") || !type1.equals(n.f2.accept(this, argu)))
			throw new Exception("TimesExpression::Error, type must be int");

		return "int";
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "["
	* f2 -> PrimaryExpression()
	* f3 -> "]"
	*/
	public String visit(ArrayLookup n, StoreTypes argu) throws Exception {
		if (!n.f0.accept(this, argu).equals("int[]") || !n.f2.accept(this, argu).equals("int"))
			throw new Exception("ArrayLookup::Only Int Arrays allowed");

		return "int";
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "."
	* f2 -> "length"
	*/
	public String visit(ArrayLength n, StoreTypes argu) throws Exception {
		if (!n.f0.accept(this, argu).equals("int[]"))
			throw new Exception("ArrayLength::Only Int Arrays can use length");
		
		return "int";
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "."
	* f2 -> Identifier()
	* f3 -> "("
	* f4 -> ( ExpressionList() )?
	* f5 -> ")"
	*/
	public String visit(MessageSend n, StoreTypes argu) throws Exception {
		ClassType tmpclass;
		MethodType tmpMethod;
		String classname = n.f0.accept(this, argu);
		if ((tmpclass = symtable.getClass(classname)) == null)
			throw new Exception("Class has not been declared");

		String methodname = n.f2.accept(this, argu);
		if ((tmpMethod = tmpclass.getMethod(methodname)) == null){
			// check if exist in parentclass
			if (tmpclass.getParentName() != null)
			{
				tmpclass = symtable.getClass(tmpclass.getParentName());
				boolean methodFound = false;
				if ((tmpMethod = tmpclass.getMethod(methodname)) != null)
					methodFound = true;
				else{
					// check every parent until there is noone left
					String parentName = tmpclass.getParentName();
					while (parentName != null){
						tmpclass = symtable.getClass(parentName);
						if ((tmpMethod = tmpclass.getMethod(methodname)) != null){
							methodFound = true;
							break;
						}
						parentName = tmpclass.getParentName();
					}
				}
				if (!methodFound)
					throw new Exception("Method has not been declared");
			}
			else
				throw new Exception("Method has not been declared");

		}
		
		// edw isws prepei na tsekarw an einai present, an to tsekarw xanei thn periptwsh na thelei orismata alla na min deinei kanena
		// px na thelei foo(int k) kai na dinw foo() , tha perasei enw den prepei
		
		// if (n.f4.present())
		// {
			// create an arraylist to store types from expression() 
			argu = new StoreTypes();
			n.f4.accept(this, argu);
			if (tmpMethod.getParams().size() != argu.types.size())
				throw new Exception("Number of Parameters doesn't match with declaration");

			ArrayList<VariableType> methodparams = tmpMethod.getParams();
			String type1;
			String type2;
			ClassType tmpClass;
			boolean Found = false;
			// check every parameter
			for (int i=0; i<tmpMethod.getParams().size(); i++){
				type1 = methodparams.get(i).getType();
				type2 = argu.types.get(i);

				if (type1.equals(type2))
					continue;
				tmpClass = symtable.getClass(type2);
				if (tmpclass != null){
					while (tmpClass.parentName != null){
						if (type1.equals(tmpClass.parentName)){
							Found = true;
							break;
						}
						tmpClass = symtable.getClass(tmpClass.parentName);
					}
				}
				if (Found == false)
					throw new Exception("Parameter type doesn't match");
			}
		return tmpMethod.getType();
	}

	/**
	* f0 -> Expression()
	* f1 -> ExpressionTail()
	*/
	public String visit(ExpressionList n, StoreTypes argu) throws Exception {
		// add parameter type to list 
		argu.addType(n.f0.accept(this, argu));
		n.f1.accept(this, argu);
		return null;
	}

	/**
	* f0 -> ( ExpressionTerm() )*
	*/
	public String visit(ExpressionTail n, StoreTypes argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> ","
	* f1 -> Expression()
	*/
	public String visit(ExpressionTerm n, StoreTypes argu) throws Exception {
		// add parameter type to list 
		argu.addType(n.f1.accept(this, argu));
		return null;
	}

	// /**
	// * f0 -> NotExpression()
	// *       | PrimaryExpression()
	// */
	// public String visit(Clause n, StoreTypes argu) {
	// 	return n.f0.accept(this, argu);
	// }
	
	/**
	* f0 -> IntegerLiteral()
	*       | TrueLiteral()
	*       | FalseLiteral()
	*       | Identifier()
	*       | ThisExpression()
	*       | ArrayAllocationExpression()
	*       | AllocationExpression()
	*       | BracketExpression()
	*/
	public String visit(PrimaryExpression n, StoreTypes argu) throws Exception {
		String name = n.f0.accept(this, argu);		
		// which -> find which rule was chosen
		// 3 is Identifier(), only here must check for type
		// every other rule returns type
		if (n.f0.which == 3){
			VariableType tmpvar;
			boolean variableFound = false;
			if ((tmpvar = curmethod.findVar(name)) != null)
				variableFound = true;
			else{
				if ((tmpvar = curclass.getVar(name)) != null)
					variableFound = true;
				else{
					String parentName = curclass.getParentName();
					ClassType tmpClass;
					while (parentName != null){
						tmpClass = symtable.getClass(parentName);
						if ((tmpvar = tmpClass.getVar(name)) != null){
							variableFound = true;
							break;
						}
						parentName = tmpClass.getParentName();
					}
				}
			}

			if (!variableFound)
				throw new Exception("PrimExpr::Variable doesn't have a declared type");

			return tmpvar.getType();
		}
		return name;
	}

	/**
	* f0 -> <INTEGER_LITERAL>
	*/
	public String visit(IntegerLiteral n, StoreTypes argu) throws Exception {
		return "int";
	}

	/**
	* f0 -> "true"
	*/
	public String visit(TrueLiteral n, StoreTypes argu) throws Exception {
		return "boolean";
	}

	/**
	* f0 -> "false"
	*/
	public String visit(FalseLiteral n, StoreTypes argu) throws Exception {
		return "boolean";
	}

	/**
	* f0 -> <IDENTIFIER>
	*/
	public String visit(Identifier n, StoreTypes argu) throws Exception {
		return n.f0.toString();
	}

	/**
	* f0 -> "this"
	*/
	public String visit(ThisExpression n, StoreTypes argu) throws Exception {
		return curclass.getName();
	}

	/**
	* f0 -> "new"
	* f1 -> "int"
	* f2 -> "["
	* f3 -> Expression()
	* f4 -> "]"
	*/
	public String visit(ArrayAllocationExpression n, StoreTypes argu) throws Exception {
		String type = n.f3.accept(this, argu);
		if (!type.equals("int"))
			throw new Exception("ArrayAllocation::Int Arrays only allowed");
		return "int[]";
	}

	/**
	* f0 -> "new"
	* f1 -> Identifier()
	* f2 -> "("
	* f3 -> ")"
	*/
	public String visit(AllocationExpression n, StoreTypes argu) throws Exception {
		String classname = n.f1.f0.toString();
		if (symtable.getClass(classname) == null)
			throw new Exception("AllocationExpression::Class doesn't exist");

		return classname;
	}

	/**
	* f0 -> "!"
	* f1 -> Clause()
	*/
	public String visit(NotExpression n, StoreTypes argu) throws Exception {
		String type = n.f1.accept(this, argu);
		if (!type.equals("boolean"))
			throw new Exception("NotExpression::Type must be boolean");
		return type;
	}

	/**
	* f0 -> "("
	* f1 -> Expression()
	* f2 -> ")"
	*/
	public String visit(BracketExpression n, StoreTypes argu) throws Exception {
		String type = n.f1.accept(this, argu);
		return type;
	}

	/**
	* f0 -> "boolean"
	*/
	public String visit(BooleanType n, StoreTypes argu) throws Exception {
		return "boolean";
	}

	/**
	* f0 -> "int"
	*/
	public String visit(IntegerType n, StoreTypes argu) throws Exception {
		return "int";
	}


	/**
	* f0 -> "int"
	* f1 -> "["
	* f2 -> "]"
	*/
	public String visit(ArrayType n, StoreTypes argu) throws Exception {
		return "int[]";
	}


}

