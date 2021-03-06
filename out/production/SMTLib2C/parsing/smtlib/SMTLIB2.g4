grammar SMTLIB2;

scratch: inputs* properties* (skolem | check)* EOF;

skolem: declare+ '(' 'assert' (letexp | expr) ')';

check: CS;

declare: '(' 'declare-fun' ID '(' type* ')' type ')';

letexp: '(' 'let' '(' local* ')' body ')';

body: (letexp | expr);

local: '(' ID expr ')';

type: 'Int'                                              # intType
    | 'Bool'                                             # boolType
    | 'Real'                                             # realType
    ;


inputs: ';-- INPUTS: ' (ID (',' ID)*)? ;

properties: ';-- PROPERTIES: ' (ID (',' ID)*)? ;


expr: ID                                                               # idExpr
    | INT                                                              # intExpr
    | REAL                                                             # realExpr
    | BOOL                                                             # boolExpr
    | '(' op=('to_real' | 'to_int') expr ')'                           # castExpr
    | '(' op=('/' | 'div' | 'mod') expr expr ')'                       # binaryExpr
    | '(' op=('*' | '+' | '-') expr expr+ ')'                          # binaryExpr
    | '(' op=('<' | '<=' | '>' | '>=' | '=' ) expr expr ')'            # binaryExpr
    | '(' op='and' expr expr+ ')'                                      # binaryExpr
    | '(' op=('or' | 'xor') expr expr+ ')'                             # binaryExpr
    | '(' 'not' expr ')'                                               # notExpr
    | '(' '-' expr ')'                                                 # negateExpr
    | <assoc=right> '(' op='=>' expr expr ')'                          # binaryExpr
    | <assoc=right> '(' op='->' expr expr ')'                          # binaryExpr
    | '(' 'ite' expr expr expr ')'                                     # ifThenElseExpr
    | '(' ID expr+ ')'                                                 # funAppExpr
    ;

REAL: INT '.' INT;

BOOL: 'true' | 'false';
INT: [0-9]+;


ID: ([a-zA-Z_$!~.%] | '|' | '[' | ']') ([a-zA-Z_0-9$!.~%] | '|' | '[' | ']')*;

WS: [ \t\n\r\f]+ -> skip;

SL_COMMENT: ';' (~[-\n\r] ~[-\n\r]* | /* empty */) ('\r'? '\n')? -> skip;

CS: '(check-sat)' -> skip;

ERROR: .;
