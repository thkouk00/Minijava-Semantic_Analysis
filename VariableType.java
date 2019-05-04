import syntaxtree.*;
import java.util.*;

// each variable has name and type
// offset is metadata for p3
public class VariableType{
	int offset;
	String name;
	String type;

	public VariableType(String name, String type){
		this.name = name;
		this.type = type;
		this.offset = 0;
	}

	public VariableType(String name, String type, int offset){
		this.name = name;
		this.type = type;
		this.offset = offset;
	}

	public String getName() {
		return this.name;
	}

	public String getType(){
		return this.type;
	}

	public int getOffset(){
		return this.offset;
	}
}