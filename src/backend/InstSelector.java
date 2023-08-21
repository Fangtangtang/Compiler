package backend;

import asm.*;
import asm.instruction.*;
import asm.operand.*;
import asm.section.Bss;
import asm.section.Data;
import asm.section.Rodata;
import ir.*;
import ir.entity.Entity;
import ir.entity.constant.*;
import ir.entity.var.LocalTmpVar;
import ir.entity.var.Ptr;
import ir.function.*;
import ir.irType.IRType;
import ir.irType.IntType;
import ir.stmt.instruction.*;
import ir.stmt.terminal.*;
import org.antlr.v4.codegen.target.Python2Target;
import utility.Pair;
import utility.error.InternalException;
import utility.type.Type;

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

    int virtualRegSpace = 0;

    int maxParamCnt = 0;

    HashMap<String, VirtualRegister> toReg;

    Imm zero = new Imm(0);

    //共用的重命名规则
    private String renameBlock(String blockName) {
        return ".L" + blockName + "_" + currentFunc.name;
    }

    /**
     * 找到对应的virtual register
     * IR上entity映射到asm的VirtualRegister
     *
     * @param entity IR entity
     * @return register
     */
    private VirtualRegister getVirtualRegister(Entity entity) {
        String name = entity.toString();
        IRType type;
        if (entity instanceof Ptr ptr) {
            type = ptr.storage.type;
        } else if (entity instanceof LocalTmpVar tmpVar) {
            type = tmpVar.type;
        } else {
            throw new InternalException("get virtual register of unexpected entity");
        }
        boolean flag = type instanceof IntType intType &&
                (intType.typeName.equals(IntType.TypeName.BOOL)
                        || intType.typeName.equals(IntType.TypeName.TMP_BOOL));
        if (toReg.containsKey(name)) {
            return toReg.get(name);
        }
        VirtualRegister register = newVirtualReg(flag);
        toReg.put(name, register);
        return register;
    }

    private VirtualRegister newVirtualReg() {
        virtualRegSpace += 4;
        return new VirtualRegister(virtualRegSpace, 4);
    }

    private VirtualRegister newVirtualReg(boolean isBool) {
        ++virtualRegSpace;
        return new VirtualRegister(virtualRegSpace, 1);
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
        return new Pair<>(t0, zero);
    }

    /**
     * %3 -> a0
     *
     * @param tmp 存结果的临时寄存器
     * @param des mem被加载的virtual register
     */
    private void loadVirtualRegister(PhysicalRegister tmp, VirtualRegister des) {
        Pair<Register, Imm> pair = getRegAddress(des);
        currentBlock.pushBack(
                new LoadInst(tmp, pair.getFirst(), pair.getSecond())
        );
    }

    /**
     * a0 -> %3
     *
     * @param tmp 存结果的临时寄存器
     * @param des 存入mem的virtual register
     */
    private void storeVirtualRegister(PhysicalRegister tmp, VirtualRegister des) {
        Pair<Register, Imm> pair = getRegAddress(des);
        currentBlock.pushBack(
                new StoreInst(tmp, pair.getFirst(), pair.getSecond())
        );
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
        virtualRegSpace = 8;
        maxParamCnt = 0;
        toReg = new HashMap<>();
        //顺序访问每一个block
        for (Map.Entry<String, BasicBlock> entry : function.blockMap.entrySet()) {
            BasicBlock block = entry.getValue();
            visit(block);
        }
        //exit function
        currentFunc.basicSpace = virtualRegSpace;
        currentFunc.extraParamCnt = (maxParamCnt > 8 ? maxParamCnt - 8 : 0);
        int stackSize = currentFunc.basicSpace
                + (currentFunc.extraParamCnt << 2);
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
     * 创建virtual register
     * 在栈上获得alloca对应的空间
     *
     * @param stmt 局部变量空间申请
     */
    @Override
    public void visit(Alloca stmt) {
        getVirtualRegister(stmt.result);
    }

    /**
     * 将int型数值转化为对应的操作数
     * 利用physical register a6计算
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
            PhysicalRegister a7 = registerMap.getReg("a7");
            currentBlock.pushBack(
                    new LuiInst(a7, new Imm((num >> 12)))
            );
            if ((num & 0xFFF) != 0) {
                currentBlock.pushBack(
                        new ImmBinaryInst(
                                a7,
                                new Imm(num & 0xFFF),
                                a7,
                                ImmBinaryInst.Opcode.addi
                        )
                );
            }
            return a7;
        }
    }

    //TODO:超过int的str(value:long long?)

    /**
     * @param constant int\bool
     * @return imm\phyReg
     */
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
        VirtualRegister result = getVirtualRegister(stmt.result);
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
            Operand operand = number2operand(ans);//imm\phyReg
            if (operand instanceof Imm imm) {
                PhysicalRegister t1 = registerMap.getReg("t1");
                currentBlock.pushBack(
                        new LiInst(t1, imm)
                );
                storeVirtualRegister(t1, result);
            }
            //寄存器赋值
            else {
                storeVirtualRegister((PhysicalRegister) operand, result);
            }
            return;
        }
        Operand operand1, operand2;
        if (stmt.op1 instanceof ConstInt c1) {
            operand1 = number2operand(Integer.parseInt(c1.value));
        } else {
            operand1 = registerMap.getReg("a0");
            loadVirtualRegister((PhysicalRegister) operand1, getVirtualRegister(stmt.op1));
        }
        if (stmt.op2 instanceof ConstInt c2) {
            operand2 = number2operand(Integer.parseInt(c2.value));
        } else {
            operand2 = registerMap.getReg("a1");
            loadVirtualRegister((PhysicalRegister) operand2, getVirtualRegister(stmt.op2));
        }
        PhysicalRegister a2 = registerMap.getReg("a2");
        if (operand1 instanceof Imm imm) {
            currentBlock.pushBack(
                    new ImmBinaryInst(a2, imm, (Register) operand2, stmt.operator)
            );
        } else if (operand2 instanceof Imm imm) {
            currentBlock.pushBack(
                    new ImmBinaryInst(a2, imm, (Register) operand1, stmt.operator)
            );
        } else {
            currentBlock.pushBack(
                    new BinaryInst(a2, (Register) operand1, (Register) operand2, stmt.operator)
            );
        }
        storeVirtualRegister(a2, result);
    }

    /**
     * TODO
     *
     * @param stmt
     */
    @Override
    public void visit(Call stmt) {

    }

    /**
     * TODO
     *
     * @param stmt
     */
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


    /**
     * Icmp
     * 大小比较
     * 两个imm(int\bool)比较，直接处理
     *
     * @param stmt icmp
     */
    @Override
    public void visit(Icmp stmt) {
        VirtualRegister result = getVirtualRegister(stmt.result);
        //两个常量
        if (stmt.op1 instanceof Constant c1
                && stmt.op2 instanceof Constant c2) {
            boolean ans;
            //bool
            if (c1 instanceof ConstBool) {
                boolean op1 = ((ConstBool) c1).value;
                boolean op2 = ((ConstBool) c2).value;
                if (stmt.cond.equals(Icmp.Cond.eq)) {
                    ans = op1 == op2;
                } else {
                    ans = op1 != op2;
                }
            } else if (c1 instanceof ConstInt) {
                int op1 = Integer.parseInt(((ConstInt) c1).value);
                int op2 = Integer.parseInt(((ConstInt) c2).value);
                switch (stmt.cond) {
                    case slt -> ans = op1 < op2;
                    case sgt -> ans = op1 > op2;
                    case sle -> ans = op1 <= op2;
                    case sge -> ans = op1 >= op2;
                    case eq -> ans = op1 == op2;
                    case ne -> ans = op1 != op2;
                    default -> throw new InternalException("unexpected operator in Icmp instruction");
                }
            } else {
                throw new InternalException("unexpected const operand in icmp");
            }
            PhysicalRegister t1 = registerMap.getReg("t1");
            currentBlock.pushBack(
                    new LiInst(t1, new Imm(ans))
            );
            storeVirtualRegister(t1, result);
            return;
        }
        //其余
        Operand operand1, operand2;
        if (stmt.op1 instanceof Constant c1) {
            operand1 = const2operand(c1);
        } else {
            operand1 = registerMap.getReg("a0");
            loadVirtualRegister((PhysicalRegister) operand1, getVirtualRegister(stmt.op1));
        }
        if (stmt.op2 instanceof Constant c2) {
            operand2 = const2operand(c2);
        } else {
            operand2 = registerMap.getReg("a1");
            loadVirtualRegister((PhysicalRegister) operand2, getVirtualRegister(stmt.op2));
        }
        PhysicalRegister a2 = registerMap.getReg("a2");
        if (operand1 instanceof Imm imm) {
            currentBlock.pushBack(
                    new ImmCmpInst(a2, imm, (Register) operand2, stmt.cond)
            );
        } else if (operand2 instanceof Imm imm) {
            currentBlock.pushBack(
                    new ImmCmpInst(a2, imm, (Register) operand1, stmt.cond)
            );
        } else {
            currentBlock.pushBack(
                    new CmpInst(a2, (Register) operand1, (Register) operand2, stmt.cond)
            );
        }
        storeVirtualRegister(a2, result);
    }


    /**
     * Load
     * %3 = load i32, ptr %1
     *
     * @param stmt load
     */
    @Override
    public void visit(Load stmt) {
        VirtualRegister result = getVirtualRegister(stmt.result);//加载到result
        PhysicalRegister a0 = registerMap.getReg("a0");
        loadVirtualRegister(a0, getVirtualRegister(stmt.pointer));
        storeVirtualRegister(a0, result);
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
        VirtualRegister result = getVirtualRegister(stmt.pointer);
        PhysicalRegister a0 = registerMap.getReg("a0");
        if (stmt.value instanceof Constant constant) {
            Operand operand = const2operand(constant);
            if (operand instanceof Imm imm) {
                currentBlock.pushBack(
                        new LiInst(a0, imm)
                );
                storeVirtualRegister(a0, result);
            } else {
                storeVirtualRegister((PhysicalRegister) operand, result);
            }
            return;
        } else {
            loadVirtualRegister(a0, getVirtualRegister(stmt.value));
            storeVirtualRegister(a0, result);
        }
    }


    /**
     * ---------------------------------
     * |br i1 %11, label %12, label %13
     * |
     * |	lbu	a0, -21(s0)
     * |	andi	a0, a0, 1
     * |	beqz	a0, .LBB0_2         # false
     * |	j	.LBB0_1
     * ------------------------------------
     *
     * @param stmt branch
     */
    @Override
    public void visit(Branch stmt) {
        PhysicalRegister a0 = registerMap.getReg("a0");
        loadVirtualRegister(a0, getVirtualRegister(stmt.condition));
        currentBlock.pushBack(
                new ImmBinaryInst(a0, new Imm(1), a0, ImmBinaryInst.Opcode.andi)
        );
        currentBlock.pushBack(
                new BranchInst(a0, renameBlock(stmt.falseBranch))
        );
        currentBlock.pushBack(
                new JumpInst(renameBlock(stmt.trueBranch))
        );
    }

    /**
     * Jump
     * 函数内跳转，block重命名
     *
     * @param stmt jump
     */
    @Override
    public void visit(Jump stmt) {
        currentBlock.pushBack(
                new JumpInst(renameBlock(stmt.targetName))
        );
    }

    /**
     * 每个函数仅有一个Return
     * 在回收栈之后直接在函数访问中添加
     */
    @Override
    public void visit(Return stmt) {
    }

    /**
     * ir中将i8转变为i1
     * asm中将entity名对应到同一个virtual register
     *
     * @param stmt trunc
     */
    @Override
    public void visit(Trunc stmt) {
        toReg.put(stmt.result.toString(), getVirtualRegister(stmt.value));
    }

    /**
     * ir中将i1转变为i8
     * asm中将entity名对应到同一个virtual register
     *
     * @param stmt zero ext
     */
    @Override
    public void visit(Zext stmt) {
        toReg.put(stmt.result.toString(), getVirtualRegister(stmt.value));
    }

    /**
     * Phi
     *
     *
     * @param stmt phi
     */
    @Override
    public void visit(Phi stmt) {

    }

}
