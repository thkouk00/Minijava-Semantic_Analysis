import java.util.*;

// structure to hold variable types in Expressions()
public class StoreTypes{
	public ArrayList<String> types;

	public StoreTypes(){
		this.types = new ArrayList<String>();
	}

	public void addType(String type){
		this.types.add(type);
	}
}