lexer grammar SysYLexer;
CONST : 'const';

INT : 'int';

VOID : 'void';

IF : 'if';

ELSE : 'else';

WHILE : 'while';

BREAK : 'break';

CONTINUE : 'continue';

RETURN : 'return';

PLUS : '+';

MINUS : '-';

MUL : '*';

DIV : '/';

MOD : '%';

ASSIGN : '=';

EQ : '==';

NEQ : '!=';

LT : '<';

GT : '>';

LE : '<=';

GE : '>=';

NOT : '!';

AND : '&&';

OR : '||';

L_PAREN : '(';

R_PAREN : ')';

L_BRACE : '{';

R_BRACE : '}';

L_BRACKT : '[';

R_BRACKT : ']';

COMMA : ',';

SEMICOLON : ';';

IDENT : ('_'|LETTER)(DIGIT|LETTER|'_')*;//以下划线或字母开头，仅包含下划线、英文字母大小写、阿拉伯数字

INTEGER_CONST : [1-9]DIGIT*|'0'(('X'|'x')[1-9A-Fa-f](DIGIT|[A-Fa-f])*|[1-7][0-7]*);//数字常量，包含十进制数，0开头的八进制数，0x或0X开头的十六进制数



WS : [ \r\n\t]+ ->skip;

LINE_COMMENT : '//' .*? '\n' ->skip;

MULTILINE_COMMENT : '/*' .*? '*/' ->skip;

fragment DIGIT:[0-9];
fragment LETTER:[A-Za-z];