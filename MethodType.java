import java.util.*;
import syntaxtree.*;

// each method has name , type, parameters and variables
// offset is metadata for p3
// OverrideFlag used for final offset printing. Every method has offset even if there is overriding.
// In offset printing we ignore override methods, but every method has an offset value
public class MethodType{
	int offset;
	String name;
	String type;
	boolean OverrideFlag;
	ArrayList<VariableType> params;
	LinkedHashMap<String, VariableType> variables;

	public MethodType(String name, String type, int offset){
		this.name = name;
		this.type = type;
		this.variables = new LinkedHashMap<String, VariableType>();
		this.params = new ArrayList<VariableType>();
		this.offset = offset;
		this.OverrideFlag = false;
	}

	public String getName(){
		return this.name;	
	} 

	public String getType(){
		return this.type;	
	} 

	public int getOffset(){
		return this.offset;
	}

	public boolean getOverrideFlag(){
		return this.OverrideFlag;
	}

	public VariableType getVar(String name){
		return variables.get(name);
	}

	public ArrayList<VariableType> getParams(){
		return params;
	} 

	public VariableType getParam(String name) {
		for (VariableType tmpvar : params) {
			if (tmpvar.getName().equals(name))
				return tmpvar;
		}
		return null;
	}

	public boolean ContainsVar(String name){
		return (variables.containsKey(name) || ContainsParam(name));
	}
	
	public boolean ContainsParam(String name){
		for (VariableType vari : params){
			if (vari.getName().equals(name))
				return true;
		}

		return false;
	}

	public boolean addVar(String name, String type){
		if (ContainsVar(name))
			return false;
		VariableType tmpvar = new VariableType(name, type);
		variables.put(name, tmpvar);
		// System.out.println("MethodType::"+this.name+" Inserting "+name+" variable with type "+type);
		return true;
	}
	
	public boolean addParam(String name, String type){
		if (ContainsParam(name))
			return false;
		VariableType tmpvar = new VariableType(name,type);
		params.add(tmpvar);
		// System.out.println("MethodType::"+this.name+" Inserting "+name+" param with type "+type);
		return true;
	}

	public VariableType findVar(String name){
		VariableType tmpvar;
		if ((tmpvar = this.getVar(name)) != null)
			return tmpvar;

		if ((tmpvar = this.getParam(name)) != null)
			return tmpvar;

		return null;
	}

	public boolean setOverrideFlag(){
		this.OverrideFlag = true;
		return true;
	}

	public boolean setOffset(int offset){
		this.offset = offset;
		// System.out.println("Setting offset for method "+this.name+" to "+this.offset);
		return true;
	}
}