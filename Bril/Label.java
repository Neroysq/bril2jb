package Bril;

public class Label extends Instruction {
    public String labelName;
    public Label(String labelName) {
        this.labelName = labelName;
    }

    public String display() {
        return "label::" + labelName;
    }
}
