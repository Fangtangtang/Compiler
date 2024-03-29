grammar Mx;

program:
    declarations*
    EOF
    ;

declarations:
    funcDefStatement
    | declarationStatement
    | classDeclaration
    ;
/*
   STATEMENT
   ---------------------------------------------------------------------------------------------------------------------
 */
statement:
    suite                       #blockStmt
    | selectionStatement        #ifStmt
    | whileStatement            #whileStmt
    | forStatement              #forStmt
    | returnStatement           #returnStmt
    | breakStatement            #breakStmt
    | continueStatement         #continueStmt
    | declarationStatement      #varDefStmt
    | expressionStatement       #exprStmt
    | funcDefStatement          #funcDefStmt
    | constructFuncDefStatement #constructorStmt
    | Semicolon                 #emptyStmt
    ;

suite:
    LeftCurlyBrace statement* RightCurlyBrace;

declarationStatement:
    variableType
        (variableDeclaration (Comma variableDeclaration)*)?
    Semicolon
    ;

variableDeclaration:
    Identifier (Assign initExpression=expression)?;

selectionStatement:
    If LeftRoundBracket conditionExpression=expression RightRoundBracket
        trueStatement=statement
    (Else falseStatement=statement)*
    ;

//循环语句
//  while循环
//  for循环
whileStatement:
    While LeftRoundBracket
            conditionExpression=expression
          RightRoundBracket statement
    ;

forStatement:
    For LeftRoundBracket
            initializationStatement=statement
            (forConditionExpression=expression)? Semicolon
            (stepExpression=expression)?
        RightRoundBracket
        statement
    ;

//跳转语句
//包括 return，break，continue 三种语句
returnStatement:
    Return expression? Semicolon;

breakStatement:
    Break Semicolon;

continueStatement:
    Continue Semicolon;

//表达式语句直接由一个表达式加 ; 组成
expressionStatement:
    expression (Comma expression)* Semicolon;

parameterList:
    expression (Comma expression)*;

//函数
funcDefStatement:
    returnType Identifier
    LeftRoundBracket funcParameterList? RightRoundBracket
    functionBody=suite
    ;

funcParameterList:
    parameterDeclaration (Comma parameterDeclaration)* ;

parameterDeclaration:
    variableType Identifier (Assign initExpression=expression)?;

constructFuncDefStatement:
    Identifier LeftRoundBracket RightRoundBracket suite;

/*
   EXPRESSION
   ---------------------------------------------------------------------------------------------------------------------
   基础表达式
        包括单独出现的常量，变量和 this 指针，函数调用，对象成员访问，数组访问，new 表达式。
   算数表达式
        单目运算表达式
        双目运算表达式
        三目运算表达式
   赋值表达式

   !!
    - ANTLR处理直接左递归
    - 处理优先级
 */
expression:
    //0 改优先级
    LeftRoundBracket expression RightRoundBracket                   #parenthesisExpr

    | literal                                                       #constant
    | Identifier                                                    #variableName
    | This                                                          #pointer
    //2 从左到右关联
    | expression LeftRoundBracket parameterList? RightRoundBracket  #functionCallExpr
    | expression Dot expression                                     #memberVisExpr
    | expression (LeftSquareBracket expression RightSquareBracket)+ #arrayVisExpr

    | expression operator = (PlusPlus | MinusMinus)                 #suffixExpr
    //3 从右到左关联
    | <assoc=right> New construction                                #newExpr
    | <assoc=right> operator = (PlusPlus | MinusMinus) expression   #prefixExpr
    | <assoc=right> operator = (LogicNot | Not | Minus) expression  #prefixExpr

    //5 从左到右关联
    | expression operator=(Multiply | Divide | Mod) expression                      #binaryExpr
    //6 从左到右关联
    | expression operator=(Plus | Minus) expression                                 #binaryExpr
    //7 从左到右关联
    | expression operator=(LeftShift | RightShift) expression                       #binaryExpr
    //8 从左到右关联
    | expression operator=(Less | LessEqual | Greater | GreaterEqual) expression    #cmpExpr
    //9-14 从左到右关联
    | expression operator=(Equal | NotEqual) expression                             #cmpExpr
    | expression operator=And expression                                            #binaryExpr
    | expression operator=Xor expression                                            #binaryExpr
    | expression operator=Or expression                                             #binaryExpr
    | expression operator=AndAnd expression                                         #logicExpr
    | expression operator=OrOr expression                                           #logicExpr

    //15 从右到左关联
    | <assoc=right> expression Question expression Colon expression                 #ternaryExpr

    | <assoc=right> expression Assign expression                                    #assignExpr
    ;


//new的对象
//  数组：int[][][]=new int[1][][]正确（至少有一个size确定）
//  对象： new <Type>() 或 new <Type>
construction:
    unitVariableType
            (arrayUnit)+                                            #arrayConstruction
    | unitVariableType LeftRoundBracket RightRoundBracket           #varConstruction
    | unitVariableType                                              #varSimpleConstruction
    ;

arrayUnit:
    LeftSquareBracket expression? RightSquareBracket;

returnType:
    Void
    | buildInVariableType
    | Identifier
    | arrayIdentifier
    ;

//变量类型
//（用于变量创建）
//  内置变量类型
//  自定义类
//  数组
//  类（定义时构造实例）
variableType:
    buildInVariableType
    | Identifier
    | arrayIdentifier
    ;

//用于new的变量名
unitVariableType:
    buildInVariableType
    | Identifier
    ;

buildInVariableType:
    Bool
    | Int
    | String
    ;

arrayIdentifier:
    (
    buildInVariableType
    | Identifier
    )
    (LeftSquareBracket RightSquareBracket)+ ;

classDeclaration:
    Class Identifier LeftCurlyBrace
        (funcDefStatement | declarationStatement)*
        constructFuncDefStatement?
        (funcDefStatement | declarationStatement)*
    RightCurlyBrace Semicolon
    ;

//各种常量
literal:
    True | False
    | IntegerLiteral
    | StringLiteral
    | Null
    ;


/*
   LEXER
   =====================================================================================================================
 */


/*
   OPERATOR
   ---------------------------------------------------------------------------------------------------------------------
 */

//标准运算符
Plus: '+';
Minus: '-';
Multiply: '*';
Divide: '/';
Mod: '%';

//关系运算符
Greater: '>';
Less: '<';
GreaterEqual: '>=';
LessEqual: '<=';
NotEqual: '!=';
Equal: '==';

//逻辑运算符
AndAnd: '&&';
OrOr: '||';
LogicNot: '!';

//位运算符
RightShift: '>>';
LeftShift: '<<';
And: '&';
Or: '|';
Xor: '^';
Not: '~';

//赋值运算符
Assign: '=';

//自增自减运算符
PlusPlus: '++';
MinusMinus: '--';

//分量运算符
Dot: '.';

//下标运算符
LeftSquareBracket: '[';
RightSquareBracket: ']';

//优先级运算符
LeftRoundBracket: '(';
RightRoundBracket: ')';

//三目运算符
Question: '?';
Colon: ':';

//分隔符
Semicolon: ';';
Comma: ',';
LeftCurlyBrace: '{';
RightCurlyBrace: '}';

/*
   KEYWORD
   ---------------------------------------------------------------------------------------------------------------------
 */
Void: 'void';
Bool: 'bool';
Int: 'int';
String: 'string';
New: 'new';
Class: 'class';
Null: 'null';
True: 'true';
False: 'false';
This: 'this';
If: 'if';
Else: 'else';
For: 'for';
While: 'while';
Break: 'break';
Continue: 'continue';
Return: 'return';

/*
   CONSTANT
   ---------------------------------------------------------------------------------------------------------------------
 */

//整数常量
// 以十进制表示
// 不设负数，负数可以由正数取负号得到
// 去除前导0
IntegerLiteral:
    NonZeroDigit Digit*
    | '0'
    ;

StringLiteral:
    '"' StrChar* '"';

/*
   SKIP
 -----------------------------------------------------------------------------------------------------------------------
   空白符：空白符、制表符、换行符
   注释：行注释、块注释
 */

//空白符
WhiteSpace:
    [ \t]+ -> skip;

//换行符
LineBreak:
    (
     '\r\n'
    | '\n'
    | '\r'
    ) -> skip
    ;

//注释
LineComment:
    '//' ~[\r\n]* -> skip;

//匹配包括换行符在内的任意字符
//?表示进行非贪婪或最小匹配
BlockComment:
    '/*'  (.|'\n')*? '*/' ->skip;

//标识符
// （包括变量标识符、函数标识符、类对象标识符）
// 26 个小写英语字母，26 个大写英语字母，数码 0 到 9，下划线 _
Identifier:
    FrontChar Char*;

/*
   FRAGMENT
 -----------------------------------------------------------------------------------------------------------------------
   最基础的token组成部分
   有效避免产生过多token，导致性能下降
 */

//首字母：不为字母或下划线
fragment FrontChar: [a-zA-Z];

//非数字字符
fragment NonDigit: [a-zA-Z_];

//数字字符
fragment NonZeroDigit: [1-9];
fragment Digit: [0-9];

//任意字符
fragment Char: [a-zA-Z_0-9];

//string的组成字符
//不允许出现：" \ 换行
//转义字符有三种,其余出现在 C++ 语言中的转义字符是未定义的。
fragment StrChar:
    ~["\\\n\r]
    | EscapeCharacter
    ;

//转义字符
// （in string）
//三种：\n 表示换行符，\\ 表示反斜杠，\" 表示双引号。
fragment EscapeCharacter:
    '\\n'
    | '\\\\'
    | '\\"'
    ;
