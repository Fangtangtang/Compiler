package backend;

import asm.*;
import asm.instruction.*;
import asm.operand.*;
import asm.section.Bss;
import asm.section.Data;
import asm.section.Rodata;
import ir.*;
import ir.entity.constant.*;
import ir.function.*;
import ir.stmt.instruction.*;
import ir.stmt.terminal.*;
import utility.Pair;
import utility.error.InternalException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author F
 * 遍历IR，转化为ASM指令
 */
public class InstSelector implements IRVisitor {
    //使用的所用物理寄存器
    PhysicalRegMap registerMap = new PhysicalRegMap();

    Program program;

    Func currentFunc;

    Block currentBlock;

    int virtualRegCnt = 0;

    int maxParamCnt = 0;

    HashMap<String, VirtualRegister> toReg;

    Imm zero = new Imm(0);

    //共用的重命名规则
    private String renameBlock(String blockName) {
        return ".L" + blockName + "_" + currentFunc.name;
    }

    /**
     * IR上entity映射到asm的VirtualRegister
     *
     * @param entityName IR entity
     * @return register
     */
    private VirtualRegister getVirtualRegister(String entityName) {
        if (toReg.containsKey(entityName)) {
            return toReg.get(entityName);
        }
        VirtualRegister register = newVirtualReg();
        toReg.put(entityName, register);
        return register;
    }

    private VirtualRegister newVirtualReg() {
        ++virtualRegCnt;
        return new VirtualRegister((virtualRegCnt + currentFunc.allocaCnt) << 2);
    }

    /**
     * 获得load、store需要的register的位置
     * - offset不超过12位，直接fp(值为address)+offset
     * - 超过12位，直接寻址(计算出直接地址，存到临时量reg(t0))
     * TODO:t0安全？
     *
     * @param register 栈上虚拟寄存器
     * @return <baseReg,offset>
     */
    private Pair<Register, Imm> getRegAddress(VirtualRegister register) {
        if (register.offset < (1 << 11)) {
            return new Pair<>(registerMap.getReg("fp"), new Imm(register.offset));
        }
        PhysicalRegister t0 = registerMap.getReg("t0");
        //原offset的基地址
        currentBlock.pushBack(
                new LoadAddrInst(t0, registerMap.getReg("fp"))
        );
        //offset
        Register num = (Register) number2operand(register.offset);
        //计算真正地址
        currentBlock.pushBack(
                new BinaryInst(t0, num, t0, BinaryInst.Opcode.add)
        );
        return new Pair<>(t0, new Imm(0));
    }

    public InstSelector(Program program) {
        this.program = program;
    }

    /**
     * IRRoot
     * 访问全局变量的定义（Global指令转为全局变量表示）
     * 初始化函数
     * 一一访问函数
     * TODO:内建函数是否需要声明？怎么声明？
     *
     * @param root Mx的根
     */
    @Override
    public void visit(IRRoot root) {
        //全局变量、字符串常量
        root.globalVarDefBlock.accept(this);
        //全局变量的初始化函数
        root.globalVarInitFunction.accept(this);
        //函数
        for (Map.Entry<String, Function> function : root.funcDef.entrySet()) {
            function.getValue().accept(this);
        }
    }

    /**
     * 每个函数按block翻译
     * block用函数名重命名，确保名字不相同
     * 统计每个函数需要的栈空间
     * - 局部变量alloca
     * - 临时变量
     * - reg放不下的参数传递
     *
     * @param function 所有函数
     */
    @Override
    public void visit(Function function) {
        //enter function
        currentFunc = new Func(function.funcName);
        program.text.functions.add(currentFunc);
        virtualRegCnt = 0;
        maxParamCnt = 0;
        toReg = new HashMap<>();
        //顺序访问每一个block
        for (Map.Entry<String, BasicBlock> entry : function.blockMap.entrySet()) {
            BasicBlock block = entry.getValue();
            visit(block);
        }
        //exit function
        currentFunc.localTmpVarCnt = virtualRegCnt;
        currentFunc.extraParamCnt = (maxParamCnt > 8 ? maxParamCnt - 8 : 0);
        int stackSize = currentFunc.localTmpVarCnt
                + currentFunc.extraParamCnt
                + currentFunc.allocaCnt;
        stackSize = stackSize << 2;//字节数
        //TODO：在函数首尾添加开栈、回收指令

    }

    @Override
    public void visit(BasicBlock basicBlock) {
        currentBlock = new Block(renameBlock(basicBlock.label));
        currentFunc.funcBlocks.add(currentBlock);
        //访问block内的每一个语句
        basicBlock.statements.forEach(
                stmt -> stmt.accept(this)
        );
    }

    /**
     * Alloca
     * %1 = alloca i32
     * 在栈上获得alloca对应的空间
     *
     * @param stmt 局部变量空间申请
     */
    @Override
    public void visit(Alloca stmt) {
        VirtualRegister register = getVirtualRegister(stmt.result.identity);
    }

    /**
     * 将int型数值转化为对应的操作数
     *
     * @param num 数值
     * @return operand
     */
    Operand number2operand(int num) {
        //直接用imm
        if (num < (1 << 11)) {
            return new Imm(num);
        }
        //先lui，如果低位非0，addi
        else {
            VirtualRegister rd = newVirtualReg();
            currentBlock.pushBack(
                    new LuiInst(rd, new Imm((num >> 12)))
            );
            if ((num & 0xFFF) == 0) {
                return rd;
            } else {
                VirtualRegister result = newVirtualReg();
                currentBlock.pushBack(
                        new ImmBinaryInst(
                                rd,
                                new Imm(num & 0xFFF),
                                result,
                                ImmBinaryInst.Opcode.addi
                        )
                );
                return result;
            }
        }
    }

    //TODO:超过int的str(value:long long?)
    Operand const2operand(Constant constant) {
        if (constant instanceof ConstInt constInt) {
            return number2operand(Integer.parseInt(constInt.value));
        } else if (constant instanceof ConstBool constBool) {
            if (constBool.value) {
                return new Imm(1);
            } else {
                return new Imm(0);
            }
        } else {
            throw new InternalException("can not resolve constant");
        }
    }

    /**
     * Binary
     *
     * @param stmt 二元运算
     */
    @Override
    public void visit(Binary stmt) {
        VirtualRegister result = getVirtualRegister(stmt.result.toString());
        if (stmt.op1 instanceof ConstInt c1 && stmt.op2 instanceof ConstInt c2) {
            //两个常量运算
            int op1 = Integer.parseInt(c1.value);
            int op2 = Integer.parseInt(c2.value);
            int ans;
            switch (stmt.operator) {
                case add -> ans = op1 + op2;
                case sub -> ans = op1 - op2;
                case mul -> ans = op1 * op2;
                case sdiv -> ans = op1 / op2;
                case srem -> ans = op1 % op2;
                case shl -> ans = op1 << op2;
                case ashr -> ans = op1 >> op2;
                case and -> ans = op1 & op2;
                case xor -> ans = op1 ^ op2;
                case or -> ans = op1 | op2;
                default -> throw new InternalException("unexpected operator in Binary instruction");
            }
            Operand operand = number2operand(ans);
            if (operand instanceof Imm imm) {
                currentBlock.pushBack(
                        new LiInst(result, imm)
                );
            }
            //寄存器赋值
            else {
                currentBlock.pushBack(
                        new StoreInst(result, (Register) operand, zero)
                );
            }
            return;
        }
        Operand operand1, operand2;
        if (stmt.op1 instanceof ConstInt c1) {
            operand1 = number2operand(Integer.parseInt(c1.value));
        } else {
            operand1 = getVirtualRegister(stmt.op1.toString());
        }
        if (stmt.op2 instanceof ConstInt c2) {
            operand2 = number2operand(Integer.parseInt(c2.value));
        } else {
            operand2 = getVirtualRegister(stmt.op2.toString());
        }
        if (operand1 instanceof Imm imm) {
            currentBlock.pushBack(
                    new ImmBinaryInst(result, imm, (Register) operand2, stmt.operator)
            );
        } else if (operand2 instanceof Imm imm) {
            currentBlock.pushBack(
                    new ImmBinaryInst(result, imm, (Register) operand1, stmt.operator)
            );
        } else {
            currentBlock.pushBack(
                    new BinaryInst(result, (Register) operand1, (Register) operand2, stmt.operator)
            );
        }
    }

    @Override
    public void visit(Call stmt) {

    }

    @Override
    public void visit(GetElementPtr stmt) {

    }

    /**
     * Global
     * - 变量（int,bool全部作为被常数初始化的）
     * - 字符串常量
     *
     * @param stmt 全局变量的定义
     */
    @Override
    public void visit(Global stmt) {
        //字符串常量
        if (stmt.result.storage instanceof ConstString str) {
            program.globalDefs.add(
                    new Rodata(stmt.result.identity, str.value)
            );
            return;
        }
        //int
        if (stmt.result.storage instanceof ConstInt constInt) {
            program.globalDefs.add(
                    new Data(stmt.result.identity, constInt.value)
            );
            return;
        }
        //bool
        if (stmt.result.storage instanceof ConstBool constBool) {
            String bool = constBool.value ? "1" : "0";
            program.globalDefs.add(
                    new Data(stmt.result.identity, bool, true)
            );
            return;
        }
        //数组、类、字符串
        program.globalDefs.add(
                new Bss(stmt.result.identity)
        );
    }

    @Override
    public void visit(Icmp stmt) {

    }

    @Override
    public void visit(Load stmt) {

    }

    /**
     * Store
     * -----------------------------------------------------
     * store i32 1, ptr %1
     * li	a0, 1
     * sw	a0, -12(s0)
     * ----
     * store i32 %5, ptr %1
     * sw	a0, -12(s0)
     * -----------------------------------------------------
     *
     * @param stmt store
     */
    @Override
    public void visit(Store stmt) {
        VirtualRegister result = getVirtualRegister(stmt.pointer.toString());
        if (stmt.value instanceof Constant constant) {
            Operand operand = const2operand(constant);
            if (operand instanceof Imm imm) {
                VirtualRegister register = newVirtualReg();
                currentBlock.pushBack(
                        new LiInst(register, imm)
                );
                currentBlock.pushBack(
                        new StoreInst(result, register, zero)
                );
            } else {
                currentBlock.pushBack(
                        new StoreInst(result, (Register) operand, zero)
                );
            }
        } else {
            VirtualRegister register = getVirtualRegister(stmt.value.toString());
            currentBlock.pushBack(
                    new StoreInst(result, register, zero)
            );
        }
    }

    @Override
    public void visit(Branch stmt) {

    }

    @Override
    public void visit(Jump stmt) {

    }

    @Override
    public void visit(Return stmt) {

    }

    @Override
    public void visit(Trunc stmt) {

    }

    @Override
    public void visit(Zext stmt) {

    }

    @Override
    public void visit(Phi stmt) {

    }

}
