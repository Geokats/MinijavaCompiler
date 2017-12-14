import java.util.*;

class MethodInfo{
  public String type;
  public LinkedList<String> parameterTypes;

  public MethodInfo(String type){
    this.type = type;
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
}
