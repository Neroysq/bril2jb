package Bril;

public class ConstBoolOperation extends ConstOperation {

    String op;
    public String dest;
    Type type;
    public boolean value;

    public ConstBoolOperation(String opName, String destName, Type type, boolean value) {
        this.op = opName;
        this.dest = destName;
        this.type = type;
        this.value = value;
    }

    public String display() {
        return op + "::" + dest + "::" + value;
    }
}
