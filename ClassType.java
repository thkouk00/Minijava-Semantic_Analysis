import java.util.*;
import syntaxtree.*;

// each class has name, parentname(if there is inheritance), methods and variables
// offset vars used in order to know Next valid offset for every method/variable
public class ClassType{
	// shows what value next method/variable must have , eg last variable entry was boolean with offset 0, so varOffsetBase (next entry) will have value = 1
	int varOffsetBase;
	int methodOffsetBase;
	String name;
	String parentName;
	LinkedHashMap<String, MethodType> methods;
	LinkedHashMap<String, VariableType> variables;

	public ClassType(String name, String pName){
		this.name = name;
		this.parentName = pName;
		this.methods = new LinkedHashMap<String, MethodType>();
		this.variables = new LinkedHashMap<String, VariableType>();
		this.varOffsetBase = 0;
		this.methodOffsetBase = 0;
	}

	// if class is extended take parents offsetBase
	public ClassType(String name, String pName, int varoffset, int methodoffset){
		this.name = name;
		this.parentName = pName;
		this.methods = new LinkedHashMap<String, MethodType>();
		this.variables = new LinkedHashMap<String, VariableType>();
		this.varOffsetBase = varoffset;
		this.methodOffsetBase = methodoffset;
	}

	public String getName(){
		return this.name;
	}

	public String getParentName(){
		return this.parentName;
	}

	public int getVarOffset(){
		return this.varOffsetBase;
	}

	public int getMethodOffset(){
		return this.methodOffsetBase;
	}

	public MethodType getMethod(String name){
		return methods.get(name);
	}

	public LinkedHashMap<String, MethodType> getMethods(){
		return this.methods;
	}

	public LinkedHashMap<String, VariableType> getVars(){
		return this.variables;
	}

	public VariableType getVar(String name){
		return variables.get(name);
	}

	public boolean setVarOffset(int offset){
		this.varOffsetBase += offset;
		return true;
	}

	public boolean setMethodOffset(int offset){
		this.methodOffsetBase += offset;
		return true;
	}

	public boolean ContainsMethod(String name) {
		return methods.containsKey(name);
	}

	public boolean ContainsVar(String name){
		return variables.containsKey(name);
	}

	public boolean addMethod(String name, String type) {
		if (ContainsMethod(name))
			return false;
		
		MethodType tmpMethod = new MethodType(name,type, this.methodOffsetBase);
		methods.put(name,tmpMethod);
		// System.out.println("ClassType::"+this.name+" Inserting "+name+" method with type "+type);
		return true;
	}


	public boolean addVar(String name, String type, int offset){
		if (ContainsVar(name))
			return false;

		VariableType tmpvar = new VariableType(name, type, this.varOffsetBase);
		variables.put(name, tmpvar);
		// update varOffsetBase
		this.varOffsetBase += offset;
		// System.out.println("ClassType::"+this.name+" Inserting "+name+" variable with type "+type+" and offset "+varOffsetBase);

		return true;
	}

}