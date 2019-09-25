package Utils;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

public class InsTrans {

    public static int andBool(MethodVisitor mv) {
        mv.visitInsn(IAND);
        return 0;
    }

    public static int orBool(MethodVisitor mv) {
        mv.visitInsn(IOR);
        return 0;
    }

    public static int ifEq(MethodVisitor mv, Label lbl) {
        mv.visitJumpInsn(IFEQ, lbl);
        return 0;
    }

    public static int ifLt(MethodVisitor mv, Label lbl) {
        mv.visitJumpInsn(IFLT, lbl);
        return 0;
    }

    public static int ifGt(MethodVisitor mv, Label lbl) {
        mv.visitJumpInsn(IFGT, lbl);
        return 0;
    }

    public static int ifLe(MethodVisitor mv, Label lbl) {
        mv.visitJumpInsn(IFLE, lbl);
        return 0;
    }

    public static int ifGe(MethodVisitor mv, Label lbl) {
        mv.visitJumpInsn(IFGE, lbl);
        return 0;
    }

    public static int cmpLong(MethodVisitor mv) {
        mv.visitInsn(LCMP);
        return 0;
    }

    public static int addLong(MethodVisitor mv) {
        mv.visitInsn(LADD);
        return 0;
    }
    public static int subLong(MethodVisitor mv) {
        mv.visitInsn(LSUB);
        return 0;
    }
    public static int mulLong(MethodVisitor mv) {
        mv.visitInsn(LMUL);
        return 0;
    }
    public static int divLong(MethodVisitor mv) {
        mv.visitInsn(LDIV);
        return 0;
    }

    public static int returnVoid(MethodVisitor mv) {
        mv.visitInsn(RETURN);
        return 0;
    }

    public static int jmpWhenFalse(MethodVisitor mv, Label lbl) {
        mv.visitJumpInsn(IFEQ, lbl);
        return -1;
    }

    public static int jmpWhenTrue(MethodVisitor mv, Label lbl) {
        mv.visitJumpInsn(IFNE, lbl);
        return -1;
    }

    public static int getVarLong(MethodVisitor mv, int varNo) {
        mv.visitVarInsn(LLOAD, varNo);
        return 0;
    }

    public static int getVarBool(MethodVisitor mv, int varNo) {
        mv.visitVarInsn(ILOAD, varNo);
        return 1;
    }

    public static int jmpLabel(MethodVisitor mv, Label lbl) {
        mv.visitJumpInsn(GOTO, lbl);
        return 1;
    }

    public static int setLabel(MethodVisitor mv, Label lbl) {
        mv.visitLabel(lbl);
        return 1;
    }

    public static int pushConst(MethodVisitor mv, int value) {
        if (value > 0) {
            mv.visitInsn(ICONST_1);
        } else {
            mv.visitInsn(ICONST_0);
        }
        return 1;
    }

    public static int pushConst(MethodVisitor mv, long value) {
/*        if (value >= -1 && value <= 5) {
            switch ((int)value) {
                case -1:
                    mv.visitInsn(ICONST_M1);
                    break;
                case 0:
                    mv.visitInsn(ICONST_0);
                    break;
                case 1:
                    mv.visitInsn(ICONST_1);
                    break;
                case 2:
                    mv.visitInsn(ICONST_2);
                    break;
                case 3:
                    mv.visitInsn(ICONST_3);
                    break;
                case 4:
                    mv.visitInsn(ICONST_4);
                    break;
                case 5:
                    mv.visitInsn(ICONST_5);
                    break;
                default:

            }
        } else if (value >= -128 && value < 127) {
            mv.visitIntInsn(BIPUSH, (int)value);
        } else if (value >= -32768 && value < 32767) {
            mv.visitIntInsn(SIPUSH, (int)value);
        } else {*/
        mv.visitLdcInsn(value);
        //}
        return 1;
    }

    public static int storeVarLong(MethodVisitor mv, int varNo) {
        mv.visitVarInsn(LSTORE, varNo);
        return 0;
    }

    public static int storeVarBool(MethodVisitor mv, int varNo) {
        mv.visitVarInsn(ISTORE, varNo);
        return -1;
    }
}
