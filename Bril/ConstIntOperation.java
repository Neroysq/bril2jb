package Bril;

public class ConstIntOperation extends ConstOperation {
    String op;
    public String dest;
    Type type;
    public long value;

    public ConstIntOperation(String opName, String destName, Type type, long value) {
        this.op = opName;
        this.dest = destName;
        this.type = type;
        this.value = value;
    }

    public String display() {
        return op + "::" + dest + "::" + value;
    }
}
