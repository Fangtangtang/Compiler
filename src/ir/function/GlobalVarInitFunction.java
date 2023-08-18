package ir.function;

import ir.BasicBlock;
import ir.irType.VoidType;

/**
 * @author F
 * 全局变量初始化函数
 * 特殊函数，零散遍布全局
 * 没有return，可能有跳转（三目运算初始化）
 */
public class GlobalVarInitFunction extends Function {
    public BasicBlock currentBlock;

    public Integer tmpCounter = -1;

    public GlobalVarInitFunction() {
        super(new VoidType(),
                "global_var_init",
                new BasicBlock("global_var_init")
        );
        currentBlock = this.entry;
    }
}
