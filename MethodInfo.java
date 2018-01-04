import java.util.*;

class MethodInfo{
  private String name;
  private String type; //The return type of the method
  private ClassInfo defClass; //The class in which the method is defined
  private LinkedList<String> parameterTypes;

  public MethodInfo(String name, String type, ClassInfo defClass){
    this.name = name;
    this.type = type;
    this.defClass = defClass;
    this.parameterTypes = new LinkedList<String>();
  }

  public void addParameter(String type){
    this.parameterTypes.add(type);
  }

  public int parameterCount(){
    return this.parameterTypes.size();
  }

  public String getParameter(int index){
    return this.parameterTypes.get(index);
  }

  public String getName(){
    return this.name;
  }

  public String getType(){
    return this.type;
  }

  public ClassInfo getDefClass(){
    return this.defClass;
  }
}
