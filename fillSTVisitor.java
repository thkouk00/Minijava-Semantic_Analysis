import syntaxtree.*;
import visitor.*;
import java.util.*;

// 1st visitor, fills up symbol table, we dont care about statements at the moment only for declarations
// curclass and curmethod are used in order to know where we currently are and catch errors, helps in  this-keyword for the current object + for setting up offsets
// Visitor used is GJNoArguDepthFirst because we dont need arguments to store anything, like we need in second visitor for typechecking
public class fillSTVisitor extends GJNoArguDepthFirst<String>{
	public SymbolTable symtable;
	ClassType curclass;
	MethodType curmethod;



	// constructor
	public fillSTVisitor(){
		this.symtable = new SymbolTable();
    // helper variables in order to know current scope
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
	public String visit(MainClass n) throws Exception {
		String classname = n.f1.f0.toString();

		if (symtable.addClass(classname, null) == false)
			throw new Exception("Class is already declared");
		
    // no need to check the returned class
		curclass = symtable.getClass(classname);
			
		String methodtype = n.f5.toString();
		String methodname = n.f6.toString();

		if (curclass.addMethod(methodname, methodtype) == false)
			throw new Exception("Method is already declared");

    curmethod = curclass.getMethod(methodname);
    String param = n.f11.f0.toString();
    curmethod.addParam(param,"String[]");

		n.f14.accept(this);
    
    // exit from main class, initialize variables
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
  public String visit(ClassDeclaration n) throws Exception {
    String classname = n.f1.f0.toString();
    if (symtable.addClass(classname,null) == false)
      throw new Exception("Class with same name already exists");

    // start of class declaration, update current class in order to know class scope
    curclass = symtable.getClass(classname); 

    // fill ST with vars and methods
    n.f3.accept(this);
    n.f4.accept(this);

    // end of class declaration , initialize values
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
  public String visit(ClassExtendsDeclaration n) throws Exception {
    // no need to check for cyclic inheritance , because every parent class must be declared
    
    String classname = n.f1.f0.toString();
    String parentName = n.f3.f0.toString();
    
    if (parentName != null){
      if (symtable.addClassExt(classname,parentName) == false)
        throw new Exception("Class exists or Parent Class has not been declared");
    }

    // start of class declaration, update current class in order to know where i am
    curclass = symtable.getClass(classname);
    n.f5.accept(this);
    n.f6.accept(this);

    // end of class declaration , initialize values
    curclass = null;
    curmethod = null;
    return null;
  }

  /**
  * f0 -> Type()
  * f1 -> Identifier()
  * f2 -> ";"
  */
  public String visit(VarDeclaration n) throws Exception {
    String type = n.f0.accept(this);

    // if not in method scope calc offsets
    if (curmethod == null){ 
      int varoffset;
      if (type.equals("boolean"))
        varoffset = 1;
      else if (type.equals("int"))
        varoffset = 4;
      else
        varoffset = 8;
      
      if (curclass.addVar(n.f1.f0.toString(), type, varoffset) == false)
        throw new Exception("Variable already declared in class");

    }
    else{
      if (curmethod.addVar(n.f1.f0.toString(),type) == false)
        throw new Exception("Variable already declared in method");
    }

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
  public String visit(MethodDeclaration n) throws Exception {
   boolean  overrideFlag = false;

    String methodtype = n.f1.accept(this);
    String methodname = n.f2.accept(this);

    if (curclass.addMethod(methodname, methodtype) == false)
      throw new Exception("Method already declared in class");

    // start method declaration, update current method in order to know where i am and catch errors
    curmethod = curclass.getMethod(methodname);
    n.f4.accept(this);
    
    // check for overloading , only overriding accepted
    String parentName = curclass.getParentName();
    while (parentName != null){
      ClassType tmpClass = symtable.getClass(parentName);
      MethodType tmpMethod = tmpClass.getMethod(methodname);
      if (tmpMethod != null){
        ArrayList<VariableType> tmpParams = tmpMethod.getParams();
        ArrayList<VariableType> curParams = curmethod.getParams();

        // parameter size must be equal        
        if (tmpParams.size() != curParams.size())
          throw new Exception("Method cannot be overloaded");
      
        // every parameter must match up with method's signature
        VariableType var1;
        VariableType var2;
        for (int i=0; i<tmpParams.size();i++){
            if (!tmpParams.get(i).getType().equals(curParams.get(i).getType()))
              throw new Exception("Method cannot be overloaded");
        }

        // current method's offset same as parent's method because of overriding
        overrideFlag = true;
        int parentOffset = tmpMethod.getOffset();
        curmethod.setOffset(parentOffset);
        curmethod.setOverrideFlag();
        break;  
      }
      parentName = tmpClass.getParentName();
    }
    
    if (!overrideFlag){
      int curOffset = curclass.getMethodOffset();
      curmethod.setOffset(curOffset);
      curclass.setMethodOffset(8);
    }

    n.f7.accept(this);

    // end of method declaration, initialize value
    curmethod = null;
    return null;
  }

  /**
   * f0 -> Type()
   * f1 -> Identifier()
   */
  public String visit(FormalParameter n) throws Exception {
    if (curmethod.addParam(n.f1.accept(this), n.f0.accept(this)) == false)
      throw new Exception("Parameter has already been declared");

    return null;
  }

  /**
  * f0 -> "int"
  * f1 -> "["
  * f2 -> "]"
  */
  public String visit(ArrayType n) throws Exception {
    return "int[]" ;
  }
  /**
  * f0 -> "boolean"
  */
  public String visit(BooleanType n) throws Exception {
    return n.f0.toString();
  }

  /**
  * f0 -> "int"
  */
  public String visit(IntegerType n) throws Exception {
    return n.f0.toString();
  }

  /**
  * f0 -> <IDENTIFIER>
  */
  public String visit(Identifier n) throws Exception {
    return n.f0.toString();
  }


}