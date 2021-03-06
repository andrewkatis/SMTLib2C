package parsing.smtlib;

import jkind.StdErr;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.tree.TerminalNode;
import parsing.smtlib.SMTLIB2Parser.*;



import skolem.*;
import skolem.BinaryExpr;
import skolem.BinaryOp;
import skolem.BoolExpr;
import skolem.CastExpr;
import skolem.Equation;
import skolem.Expr;
import skolem.IdExpr;
import skolem.IfThenElseExpr;
import skolem.IntExpr;
import skolem.Location;
import skolem.NamedType;
import skolem.RealExpr;
import skolem.Type;
import skolem.UnaryExpr;
import skolem.UnaryOp;
import skolem.VarDecl;

public class SMTLIB2ToAstVisitor extends SMTLIB2BaseVisitor<Object> {

    Map<String, List<Expr>> rngNames = new HashMap<>();
    List<Expr> finalbody = new ArrayList<>();

    public Scratch scratch(ScratchContext ctx) {
        List<String> insprops = realizabilityInputs(ctx.inputs());
        List<SkolemContext> skolemctx = ctx.skolem();
        List<VarDecl> inputs = inputvars(skolemctx, insprops);
        insprops.addAll(properties(ctx.properties()));
        List<VarDecl> outputs = outputvars(skolemctx, insprops);
        List<Skolem> skolems = skolems(skolemctx, insprops);
        return new Scratch(loc(ctx), inputs, outputs, skolems, rngNames);
    }

    private List<String> realizabilityInputs(List<InputsContext> inputsctx) {
        List<String> ids = new ArrayList<>();
        for (InputsContext ictx : inputsctx) {
            for (TerminalNode idctx : ictx.ID()) {
                ids.add(idctx.getText());
            }
        }
        return ids;
    }

    private List<String> properties(List<PropertiesContext> propertiesctx) {
        List<String> ids = new ArrayList<>();
        for (PropertiesContext pctx : propertiesctx) {
            for (TerminalNode idctx : pctx.ID()) {
                ids.add(idctx.getText());
            }
        }
        return ids;
    }

    //it may be enough to use only the 0-th skolem
    private List<VarDecl> inputvars(List<SkolemContext> skolems, List<String> inputs) {

        List<VarDecl> decls = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (SkolemContext skolem : skolems) {
            List<DeclareContext> declares = skolem.declare();
            for (DeclareContext declare : declares) {
                String id = removeDelimiters(declare.ID().getText());
//                String prefix = id;
//                if (id.contains(".")) {
//                    prefix = id.substring(0, id.indexOf("."));
//                }
//                if (ids.contains(prefix) && !(names.contains(id))) {
//                    Type type = type(declare.type());
//                    //replacingSymbols should happen after receing the IR, to support multiple outputs
//                    decls.add(new VarDecl(loc(declare), replaceSymbols(id), type));
//                    names.add(id);
//                }
                if (inputs.contains(id) && !(names.contains(id))) {
                    Type type = type(declare.type().get(declare.type().size() -1));
                    //replacingSymbols should happen after receing the IR, to support multiple outputs
                    decls.add(new VarDecl(loc(declare), replaceSymbols(id), type));
                    names.add(id);
                }
            }
        }

        return decls;
    }



    private String replaceSymbols(String str) {
        return str.replaceAll("[~.]","_");
    }

    private String removeDelimiters(String str) {
        if (str.startsWith("$")) {
            str = str.substring(str.indexOf("$")+1, str.lastIndexOf("$"));
        }
        return str;
    }

//    private List<VarDecl> inputvars(List<SkolemContext> ctxs) {
//        List<VarDecl> decls = new ArrayList<>();
//        List<String> names = new ArrayList<>();
//        int skolem_index = 0;
//        if (ctxs == null) {
//            return decls;
//        }
//        for (SkolemContext ctx : ctxs) {
//            List<DeclareContext> dctxs = ctx.declare();
//            for (DeclareContext dctx : dctxs) {
//                String id = dctx.ID().getText();
//                if (id.endsWith("$"+Integer.toString(skolem_index))) {
//                    Type type = type(dctx.type());
//                    boolean contains = false;
//                    for (String name : names) {
//                        if (id.substring(0,id.length()-2).equals(name.substring(0,name.length()-2))) {
//                            contains = true;
//                            break;
//                        }
//                    }
//                    if (contains) {
//                        continue;
//                    } else {
//                        names.add(id);
//                        decls.add(new VarDecl(loc(dctx), rename(id), type));
//                    }
//
//                }
//            }
//            skolem_index++;
//        }
//        return decls;
//    }

    private List<VarDecl> outputvars(List<SkolemContext> skolems, List<String> insprops) {

        List<VarDecl> decls = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (SkolemContext skolem : skolems) {
            List<DeclareContext> declares = skolem.declare();
            for (DeclareContext declare : declares) {
                String id = removeDelimiters(declare.ID().getText());
//                String prefix = id;
//                if (id.contains(".")) {
//                    prefix = id.substring(0, id.indexOf("."));
//                }
//                if (ids.contains(prefix) && !(names.contains(id))) {
//                    Type type = type(declare.type());
//                    //replacingSymbols should happen after receing the IR, to support multiple outputs
//                    decls.add(new VarDecl(loc(declare), replaceSymbols(id), type));
//                    names.add(id);
//                }
                if (!insprops.contains(id) && !(names.contains(id)) && !id.equals("%init")) {
                    Type type = type(declare.type().get(declare.type().size() - 1));
                    //replacingSymbols should happen after receing the IR, to support multiple outputs
                    decls.add(new VarDecl(loc(declare), replaceSymbols(id), type));
                    names.add(id);
                }
            }
        }

        return decls;
    }

//    private List<VarDecl> outputvars(List<SkolemContext> ctxs) {
//        List<VarDecl> decls = new ArrayList<>();
//        List<String> names = new ArrayList<>();
//        int skolem_index = 0;
//        if (ctxs == null) {
//            return decls;
//        }
//        for (SkolemContext ctx : ctxs) {
//            List<DeclareContext> dctxs = ctx.declare();
//            for (DeclareContext dctx : dctxs) {
//                String id = dctx.ID().getText();
//                if((id.endsWith("$"+Integer.toString(skolem_index+2)) ||
//                        id.endsWith("$~1"))) {
//                    Type type = type(dctx.type());
//                    String[] trunc = id.split("[$]");
//                    boolean contains = false;
//                    for (String name : names) {
//                        String[] truncname = name.split("[$]");
//                        if (trunc[1].equals(truncname[1])) {
//                            contains = true;
//                            break;
//                        }
//                    }
//                    if (contains) {
//                        continue;
//                    } else {
//                        names.add(id);
//                        decls.add(new VarDecl(loc(dctx), rename(id), type));
//                    }
//
//                }
//            }
//        }
//        return decls;
//    }


        private List<Skolem> skolems(List<SkolemContext> ctxs, List<String> insprops) {
        List<Skolem> skolems = new ArrayList<>();
        for (SkolemContext ctx : ctxs) {
            skolems.add(skolem(ctx, insprops));
        }
        return skolems;
    }

    public Skolem skolem(SkolemContext ctx, List<String> insprops) {
        Map<String, Expr> fastlocals = new HashMap<>();
        List<Equation> locals = new ArrayList<>();
        if (ctx.letexp() !=null) {
            fastlocals.putAll(fastlocals(ctx.letexp()));
            locals.addAll(locals(ctx.letexp()));
            List<Expr> body = body(ctx.letexp().body());
            body = inlinelocalstoexprs(body,locals);
            body = convertEqualitiesToAssignments(body, insprops);
            body = convertIfThenElsesToTernary(body);
            body = convertBooleanValuesToExitExprs(body);
            body = foldConstantsinExprs(body);
            collectRngNamesFromExprs(body);
            if (rngNames.isEmpty()) {
                StdErr.warning("-rngvalues option was given, but skolem does not contain nondeterministic assignments. A deterministic implementation will be created.");
            }
            body = addRNGfromExprs(body);
            return new Skolem(loc(ctx), locals, body);
        } else {
            List<Expr> body = new ArrayList<>();
            body.add(expr(ctx.expr()));
            body = convertEqualitiesToAssignments(body, insprops);
            body = convertIfThenElsesToTernary(body);
            body = convertBooleanValuesToExitExprs(body);
            collectRngNamesFromExprs(body);
            body = addRNGfromExprs(body);
            return new Skolem(loc(ctx), locals, body);
        }
    }

    private List<Expr> addRNGfromExprs(List<Expr> tempbody) {
        for (Expr expr : tempbody) {
            if (expr instanceof IfThenElseExpr) {
                IfThenElseExpr iteexpr = (IfThenElseExpr) expr;
                List<Expr> newThenExprs = new ArrayList<>();
                for (Expr thenExpr : iteexpr.thenExpr) {
//                    if (thenExpr instanceof AssignExpr) {
                        newThenExprs.addAll(addDisequalityBlock(thenExpr));
//                    } else {
//                        newThenExprs.add(addRNGfromExpr(thenExpr));
//                    }
                }
                List<Expr> newElseExprs = new ArrayList<>();
                for (Expr elseExpr : iteexpr.elseExpr) {
//                    newElseExprs.add(addRNGfromExpr(elseExpr));
//                    if (elseExpr instanceof AssignExpr) {
                        newElseExprs.addAll(addDisequalityBlock(elseExpr));
//                    } else {
//                        newElseExprs.add(addRNGfromExpr(elseExpr));
//                    }
                }

                finalbody.add((new IfThenElseExpr(iteexpr.cond, newThenExprs, newElseExprs)));
            } else {
                finalbody.add(addRNGfromExpr(expr));
            }
        }
        return finalbody;
    }

    private List<Expr> addDisequalityBlock(Expr expr) {
        List<Expr> exprs = new ArrayList<>();
        if (expr instanceof  AssignExpr) {
            AssignExpr aexpr = (AssignExpr) expr;
            exprs.add(addRNGfromExpr(aexpr));
            if (aexpr.expr instanceof FunAppExpr) {
                FunAppExpr fexpr = (FunAppExpr) aexpr.expr;
                if (fexpr.funArgExprs.size() > 4) {
                    BinaryExpr condExpr = new BinaryExpr(aexpr.lhs, BinaryOp.EQUAL, fexpr.funArgExprs.get(0));
                    for (int i = 1; i < fexpr.funArgExprs.size() - 4; i++) {
                        condExpr = new BinaryExpr(condExpr, BinaryOp.OR,
                                new BinaryExpr(aexpr.lhs, BinaryOp.EQUAL, fexpr.funArgExprs.get(i)));
                    }
                    exprs.add(new WhileExpr(condExpr, addRNGfromExpr(aexpr)));
                }
            }
            return exprs;
        } else if (expr instanceof IfThenElseExpr) {
            IfThenElseExpr iteExpr = (IfThenElseExpr) expr;
            List<Expr> newThenExprs = new ArrayList<>();
            List<Expr> newElseExprs = new ArrayList<>();
            for (Expr thenExpr : iteExpr.thenExpr) {
                newThenExprs.addAll(addDisequalityBlock(thenExpr));
            }
            for (Expr elseExpr : iteExpr.elseExpr) {
                newElseExprs.addAll(addDisequalityBlock(elseExpr));
            }
            exprs.add((new IfThenElseExpr(addRNGfromExpr(iteExpr.cond), newThenExprs, newElseExprs)));
        } else {
            exprs.add(addRNGfromExpr(expr));
        }
        return exprs;
    }

    private void collectRngNamesFromExprs(List<Expr> exprs) {
        for (Expr expr : exprs) {
            if (expr instanceof IfThenElseExpr) {
                IfThenElseExpr iteexpr = (IfThenElseExpr) expr;
                List<Expr> newThenExprs = new ArrayList<>();
                collectRngNamesFromExpr(iteexpr.cond);
                for (Expr thenExpr : iteexpr.thenExpr) {
                    collectRngNamesFromExpr(thenExpr);
                }
                List<Expr> newElseExprs = new ArrayList<>();
                for (Expr elseExpr : iteexpr.elseExpr) {
                    collectRngNamesFromExpr(elseExpr);
                }

            } else {
                collectRngNamesFromExpr(expr);
            }
        }
    }
    private void collectRngNamesFromExpr(Expr expr) {
        if (expr instanceof AssignExpr) {
            AssignExpr aExpr = (AssignExpr) expr;
            collectRngNamesFromExpr(aExpr.expr);
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binExpr = (BinaryExpr) expr;
            collectRngNamesFromExpr(binExpr.left);
            collectRngNamesFromExpr(binExpr.right);
        } else if (expr instanceof FunAppExpr) {
            FunAppExpr funExpr = (FunAppExpr) expr;
            List<Expr> updatedFunArgs = new ArrayList<>();
            for (Expr arg : funExpr.funArgExprs) {
                collectRngNamesFromExpr(arg);
                updatedFunArgs.add(arg);
            }
            if (!rngNames.containsKey(funExpr.funNameExpr.id)) {
                rngNames.put(funExpr.funNameExpr.id, updatedFunArgs);
            }
        } else if (expr instanceof IfThenElseExpr) {
            IfThenElseExpr iteexpr = (IfThenElseExpr) expr;
            List<Expr> newThenExprs = new ArrayList<>();
            List<Expr> newElseExprs = new ArrayList<>();
            collectRngNamesFromExpr(iteexpr.cond);
            for (Expr texpr : iteexpr.thenExpr) {
                collectRngNamesFromExpr(texpr);
            }
            for (Expr eexpr : iteexpr.elseExpr) {
                collectRngNamesFromExpr(eexpr);
            }
        } else if (expr instanceof UnaryExpr) {
            UnaryExpr uexpr = (UnaryExpr) expr;
            collectRngNamesFromExpr(uexpr.expr);
        }
    }
    private Expr addRNGfromExpr(Expr expr) {
        if (expr instanceof AssignExpr) {
            AssignExpr aExpr = (AssignExpr) expr;
            return new AssignExpr(aExpr.lhs, addRNGfromExpr(aExpr.expr));
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binExpr = (BinaryExpr) expr;
            return new BinaryExpr(addRNGfromExpr(binExpr.left), binExpr.op, addRNGfromExpr(binExpr.right));
        } else if (expr instanceof FunAppExpr) {
            FunAppExpr funExpr = (FunAppExpr) expr;
            List<Expr> updatedFunArgs = new ArrayList<>();
            for (Expr arg : funExpr.funArgExprs) {
                updatedFunArgs.add(addRNGfromExpr(arg));
            }
            if (rngNames.containsKey(funExpr.funNameExpr.id)) {
//                if (funExpr.funNameExpr.id.contains("randneq")) {
//                    if (updatedFunArgs.size() > 5) {
////                        finalbody.add(new AssignExpr(funExpr.funNameExpr, new FunAppExpr(new IdExpr("generateRandomValueExcl" + (updatedFunArgs.size() - 4)),
////                                updatedFunArgs)));
//                        return new FunAppExpr(new IdExpr("generateRandomValueExcl" + (updatedFunArgs.size() - 4)),
//                                updatedFunArgs);
//                    } else {
////                        finalbody.add(new AssignExpr(funExpr.funNameExpr, new FunAppExpr(new IdExpr("generateRandomValueExcl"),
////                                updatedFunArgs)));
//                        return new FunAppExpr(new IdExpr("generateRandomValueExcl"),
//                                updatedFunArgs);
//                    }
//                } else {
//                    finalbody.add(new AssignExpr(funExpr.funNameExpr, new FunAppExpr(new IdExpr("generateRandomValue"), updatedFunArgs)));
                    return new FunAppExpr(new IdExpr("generateRandomValue"), updatedFunArgs.subList(updatedFunArgs.size() - 4, updatedFunArgs.size()));
//                }
//                Expr assertExpr = addAssertionExpr(funExpr.funNameExpr, updatedFunArgs);
//                if (assertExpr != null) {
//                    finalbody.add(assertExpr);
//                }
            }
            return new IdExpr(funExpr.funNameExpr.id);
        } else if (expr instanceof IfThenElseExpr) {
            IfThenElseExpr iteexpr = (IfThenElseExpr) expr;
            List<Expr> newThenExprs = new ArrayList<>();
            List<Expr> newElseExprs = new ArrayList<>();
            for (Expr texpr : iteexpr.thenExpr) {
                newThenExprs.add(addRNGfromExpr(texpr));
            }
            for (Expr eexpr : iteexpr.elseExpr) {
                newElseExprs.add(addRNGfromExpr(eexpr));
            }
            return new IfThenElseExpr(addRNGfromExpr(iteexpr.cond), newThenExprs, newElseExprs);
        } else if (expr instanceof UnaryExpr) {
            UnaryExpr uexpr = (UnaryExpr) expr;
            return new UnaryExpr(uexpr.op, addRNGfromExpr(uexpr.expr));
        } else if (expr instanceof TernaryExpr) {
            TernaryExpr ternExpr = (TernaryExpr) expr;
            List<Expr> newThenExprs = new ArrayList<>();
            List<Expr> newElseExprs = new ArrayList<>();
            for (Expr texpr : ternExpr.thenExpr) {
                newThenExprs.add(addRNGfromExpr(texpr));
            }
            for (Expr eexpr : ternExpr.elseExpr) {
                newElseExprs.add(addRNGfromExpr(eexpr));
            }
            return new TernaryExpr(addRNGfromExpr(ternExpr.cond), newThenExprs, newElseExprs);
        } else {
            return expr;
        }
    }

    private Expr addAssertionExpr(IdExpr idexpr, List<Expr> funArgs) {
        if (funArgs.get(0) instanceof BoolExpr && funArgs.get(1) instanceof BoolExpr) {
            BoolExpr lflag = (BoolExpr) funArgs.get(0);
            BoolExpr uflag = (BoolExpr) funArgs.get(1);
            BinaryExpr lboundExpr;
            BinaryExpr uboundExpr;
            if (lflag.value || uflag.value) {
                if (lflag.value) {
                    lboundExpr = new BinaryExpr(funArgs.get(2), BinaryOp.LESSEQUAL, idexpr);
                } else {
                    lboundExpr = new BinaryExpr(funArgs.get(2), BinaryOp.LESS, idexpr);
                }
                if (uflag.value) {
                    uboundExpr = new BinaryExpr(idexpr, BinaryOp.LESSEQUAL, funArgs.get(3));
                } else {
                    uboundExpr = new BinaryExpr(idexpr, BinaryOp.LESS, funArgs.get(3));
                }
                return new AssertExpr(new BinaryExpr(lboundExpr, BinaryOp.AND, uboundExpr));
            } else {
                if (funArgs.get(2) instanceof IntExpr && funArgs.get(3) instanceof IntExpr) {
                    IntExpr lint = (IntExpr) funArgs.get(2);
                    IntExpr uint = (IntExpr) funArgs.get(3);
                    if (lint.value.intValue() == 0 && uint.value.equals(lint.value)) {
                        return new AssertExpr(new BinaryExpr(new IntExpr(BigInteger.valueOf(0)),
                                BinaryOp.LESS, idexpr));
                    } else {
                        lboundExpr = new BinaryExpr(lint, BinaryOp.LESS, idexpr);
                        uboundExpr = new BinaryExpr(idexpr, BinaryOp.LESS, uint);
                        return new AssertExpr(new BinaryExpr(lboundExpr, BinaryOp.AND, uboundExpr));
                    }
                } else if (funArgs.get(2) instanceof RealExpr && funArgs.get(3) instanceof RealExpr) {
                    RealExpr lreal = (RealExpr) funArgs.get(2);
                    RealExpr ureal = (RealExpr) funArgs.get(3);

                    if (lreal.value.intValue() == 0 && ureal.value.equals(lreal.value)) {
                        return new AssertExpr(new BinaryExpr(new RealExpr(BigDecimal.valueOf(0)),
                                BinaryOp.LESS, idexpr));
                    } else {
                        lboundExpr = new BinaryExpr(lreal, BinaryOp.LESS, idexpr);
                        uboundExpr = new BinaryExpr(idexpr, BinaryOp.LESS, ureal);
                        return new AssertExpr(new BinaryExpr(lboundExpr, BinaryOp.AND, uboundExpr));
                    }
                } else {
                    lboundExpr = new BinaryExpr(funArgs.get(2), BinaryOp.LESS, idexpr);
                    uboundExpr = new BinaryExpr(idexpr, BinaryOp.LESS, funArgs.get(3));
                    return new AssertExpr(new BinaryExpr(lboundExpr, BinaryOp.AND, uboundExpr));
                }
            }
        } else {
            return null;
        }
    }

    private List<Expr> foldConstantsinExprs(List<Expr> tempbody) {
        List<Expr> folded = new ArrayList<>();
        for (Expr expr : tempbody) {
            if (expr instanceof IfThenElseExpr) {
                IfThenElseExpr iteexpr = (IfThenElseExpr) expr;
                Expr foldedcond = foldConstantsinExpr(iteexpr.cond);
                if (foldedcond instanceof BoolExpr) {
                    BoolExpr blexpr = (BoolExpr) iteexpr.cond;
                    if (blexpr.value) {
                        folded.addAll(foldConstantsinExprs(iteexpr.thenExpr));
                    } else {
                        folded.addAll(foldConstantsinExprs(iteexpr.elseExpr));
                    }
                } else {
                    IfThenElseExpr iteconv = new IfThenElseExpr(foldedcond,
                            foldConstantsinExprs(iteexpr.thenExpr),foldConstantsinExprs(iteexpr.elseExpr));
                    folded.add(iteconv);
                }

            } else {
                folded.add(foldConstantsinExpr(expr));
            }
        }
        return folded;
    }

    private Expr foldConstantsinExpr(Expr expr) {
        if (expr instanceof BinaryExpr) {
            BinaryExpr binexp = (BinaryExpr) expr;
            Expr foldedleft = foldConstantsinExpr(binexp.left);
            Expr foldedright = foldConstantsinExpr(binexp.right);
            if ((foldedleft instanceof IntExpr) && (foldedright instanceof IntExpr)) {
                int leftval = ((IntExpr) foldedleft).value.intValue();
                int rightval = ((IntExpr) foldedright).value.intValue();

                if (binexp.op.name().equals(BinaryOp.EQUAL.name())) {
                    return new BoolExpr(leftval == rightval);
                } else if (binexp.op.name().equals(BinaryOp.NOTEQUAL.name())) {
                    return new BoolExpr(leftval != rightval);
                } else if (binexp.op.name().equals(BinaryOp.GREATER.name())) {
                    return new BoolExpr(leftval > rightval);
                } else if (binexp.op.name().equals(BinaryOp.GREATEREQUAL.name())) {
                    return new BoolExpr(leftval >= rightval);
                } else if (binexp.op.name().equals(BinaryOp.LESS.name())) {
                    return new BoolExpr(leftval < rightval);
                } else if (binexp.op.name().equals(BinaryOp.LESSEQUAL.name())) {
                    return new BoolExpr(leftval <= rightval);
                } else if (binexp.op.name().equals(BinaryOp.PLUS.name())) {
                    return new IntExpr(binexp.location, new BigInteger (Integer.toString(leftval + rightval)));
                } else if (binexp.op.name().equals(BinaryOp.MINUS.name())) {
                    return new IntExpr(binexp.location, new BigInteger (Integer.toString(leftval - rightval)));
                } else if (binexp.op.name().equals(BinaryOp.MULTIPLY.name())) {
                    return new IntExpr(binexp.location, new BigInteger (Integer.toString(leftval * rightval)));
                } else if (binexp.op.name().equals(BinaryOp.DIVIDE.name()) || binexp.op.name().equals(BinaryOp.INT_DIVIDE.name())) {
                    return new IntExpr(binexp.location, new BigInteger (Integer.toString(leftval / rightval)));
                } else if (binexp.op.name().equals(BinaryOp.MODULUS.name())) {
                    //assuming non-negative integers in mod
                    return new IntExpr(binexp.location, new BigInteger (Integer.toString(leftval % rightval)));
                } else {
                    return new BinaryExpr(foldedleft,binexp.op,foldedright);
                }
            } else if ((foldedleft instanceof IntExpr) && !(foldedright instanceof IntExpr)) {
                int leftval = ((IntExpr) foldedleft).value.intValue();
                if (leftval == 0) {
                    if (binexp.op.name().equals(BinaryOp.PLUS.name())) {
                        return foldedright;
                    } else if (binexp.op.name().equals(BinaryOp.MINUS.name())) {
                        return new UnaryExpr(UnaryOp.NEGATIVE,foldedright);
                    } else if (binexp.op.name().equals(BinaryOp.MULTIPLY.name())) {
                        return new IntExpr(binexp.location, BigInteger.valueOf(0));
                    } else if (binexp.op.name().equals(BinaryOp.DIVIDE.name())) {
                        return new IntExpr(binexp.location, BigInteger.valueOf(0));
                    } else if (binexp.op.name().equals(BinaryOp.MODULUS.name())) {
                        return new IntExpr(binexp.location, BigInteger.valueOf(0));
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                } else if (leftval == 1) {
                    if (binexp.op.name().equals(BinaryOp.MULTIPLY.name())) {
                        return foldedright;
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                } else {
                    return new BinaryExpr(foldedleft,binexp.op,foldedright);
                }
            } else if (!(foldedleft instanceof IntExpr) && (foldedright instanceof IntExpr)) {
                int rightval = ((IntExpr) foldedright).value.intValue();
                if (rightval == 0) {
                    if (binexp.op.name().equals(BinaryOp.PLUS.name())) {
                        return foldedleft;
                    } else if (binexp.op.name().equals(BinaryOp.MINUS.name())) {
                        return foldedleft;
                    } else if (binexp.op.name().equals(BinaryOp.MULTIPLY.name())) {
                        return new IntExpr(binexp.location, BigInteger.valueOf(0));
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                } else if (rightval == 1) {
                    if (binexp.op.name().equals(BinaryOp.MULTIPLY.name())) {
                        return foldedleft;
                    } else if (binexp.op.name().equals(BinaryOp.DIVIDE.name()) || binexp.op.name().equals(BinaryOp.INT_DIVIDE.name())) {
                        return foldedleft;
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                } else {
                    return new BinaryExpr(foldedleft,binexp.op,foldedright);
                }
            } else if ((foldedleft instanceof RealExpr) && (foldedright instanceof RealExpr)) {
                double leftval = ((RealExpr) foldedleft).value.doubleValue();
                double rightval = ((RealExpr) foldedright).value.doubleValue();

                if (binexp.op.name().equals(BinaryOp.EQUAL.name())) {
                    return new BoolExpr(leftval == rightval);
                } else if (binexp.op.name().equals(BinaryOp.NOTEQUAL.name())) {
                    return new BoolExpr(leftval != rightval);
                } else if (binexp.op.name().equals(BinaryOp.GREATER.name())) {
                    return new BoolExpr(leftval > rightval);
                } else if (binexp.op.name().equals(BinaryOp.GREATEREQUAL.name())) {
                    return new BoolExpr(leftval >= rightval);
                } else if (binexp.op.name().equals(BinaryOp.LESS.name())) {
                    return new BoolExpr(leftval < rightval);
                } else if (binexp.op.name().equals(BinaryOp.LESSEQUAL.name())) {
                    return new BoolExpr(leftval <= rightval);
                } else if (binexp.op.name().equals(BinaryOp.PLUS.name())) {
                    return new RealExpr(binexp.location, new BigDecimal (Double.toString(leftval + rightval)));
                } else if (binexp.op.name().equals(BinaryOp.MINUS.name())) {
                    return new RealExpr(binexp.location, new BigDecimal (Double.toString(leftval - rightval)));
                } else if (binexp.op.name().equals(BinaryOp.MULTIPLY.name())) {
                    return new RealExpr(binexp.location, new BigDecimal (Double.toString(leftval * rightval)));
                } else if (binexp.op.name().equals(BinaryOp.DIVIDE.name()) || binexp.op.name().equals(BinaryOp.INT_DIVIDE.name())) {
                    return new RealExpr(binexp.location, new BigDecimal (Double.toString(leftval / rightval)));
                } else if (binexp.op.name().equals(BinaryOp.MODULUS.name())) {
                    //assuming non-negative integers in mod
                    return new RealExpr(binexp.location, new BigDecimal (Double.toString(leftval % rightval)));
                } else {
                    return new BinaryExpr(foldedleft,binexp.op,foldedright);
                }
            } else if ((foldedleft instanceof RealExpr) && !(foldedright instanceof RealExpr)) {
                double leftval = ((RealExpr) foldedleft).value.doubleValue();
                if (leftval == 0) {
                    if (binexp.op.name().equals(BinaryOp.PLUS.name())) {
                        return foldedright;
                    } else if (binexp.op.name().equals(BinaryOp.MINUS.name())) {
                        return new UnaryExpr(UnaryOp.NEGATIVE,foldedright);
                    } else if (binexp.op.name().equals(BinaryOp.MULTIPLY.name())) {
                        return new RealExpr(binexp.location, BigDecimal.valueOf(0));
                    } else if (binexp.op.name().equals(BinaryOp.DIVIDE.name())) {
                        return new RealExpr(binexp.location, BigDecimal.valueOf(0));
                    } else if (binexp.op.name().equals(BinaryOp.MODULUS.name())) {
                        return new RealExpr(binexp.location, BigDecimal.valueOf(0));
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                } else if (leftval == 1) {
                    if (binexp.op.name().equals(BinaryOp.MULTIPLY.name())) {
                        return foldedright;
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                } else {
                    return new BinaryExpr(foldedleft,binexp.op,foldedright);
                }
            } else if (!(foldedleft instanceof RealExpr) && (foldedright instanceof RealExpr)) {
                double rightval = ((RealExpr) foldedright).value.doubleValue();
                if (rightval == 0) {
                    if (binexp.op.name().equals(BinaryOp.PLUS.name())) {
                        return foldedleft;
                    } else if (binexp.op.name().equals(BinaryOp.MINUS.name())) {
                        return foldedleft;
                    } else if (binexp.op.name().equals(BinaryOp.MULTIPLY.name())) {
                        return new RealExpr(binexp.location, BigDecimal.valueOf(0));
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                } else if (rightval == 1) {
                    if (binexp.op.name().equals(BinaryOp.MULTIPLY.name())) {
                        return foldedleft;
                    } else if (binexp.op.name().equals(BinaryOp.DIVIDE.name())) {
                        return foldedleft;
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                } else {
                    return new BinaryExpr(foldedleft,binexp.op,foldedright);
                }
            } else if ((foldedleft instanceof BoolExpr) && (foldedright instanceof BoolExpr)) {
                boolean leftval = ((BoolExpr) foldedleft).value;
                boolean rightval = ((BoolExpr) foldedright).value;
                if (binexp.op.name().equals(BinaryOp.EQUAL.name())) {
                    return new BoolExpr(leftval == rightval);
                } else if (binexp.op.name().equals(BinaryOp.NOTEQUAL.name())) {
                    return new BoolExpr(leftval != rightval);
                } else if (binexp.op.name().equals(BinaryOp.AND.name())) {
                    return new BoolExpr(leftval && rightval);
                } else if (binexp.op.name().equals(BinaryOp.OR.name())) {
                    return new BoolExpr(leftval || rightval);
                } else if (binexp.op.name().equals(BinaryOp.XOR.name())) {
                    return new BoolExpr(leftval ^ rightval);
                } else if (binexp.op.name().equals(BinaryOp.IMPLIES.name())) {
                    return new BoolExpr((!leftval || rightval));
                } else {
                    return new BinaryExpr(foldedleft,binexp.op,foldedright);
                }
            } else if ((foldedleft instanceof BoolExpr) && !(foldedright instanceof BoolExpr)) {
                BoolExpr leftbool = (BoolExpr) foldedleft;
                if (leftbool.value) {
                    if (binexp.op.name().equals(BinaryOp.AND.name())) {
                        return foldedright;
                    } else if (binexp.op.name().equals(BinaryOp.OR.name())) {
                        return foldedleft;
                    } else if (binexp.op.name().equals(BinaryOp.IMPLIES.name())) {
                        return foldedright;
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                } else {
                    if (binexp.op.name().equals(BinaryOp.AND.name())) {
                        return foldedleft;
                    } else if (binexp.op.name().equals(BinaryOp.OR.name())) {
                        return foldedright;
                    } else if (binexp.op.name().equals(BinaryOp.IMPLIES.name())) {
                        return new BoolExpr(true);
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                }
            } else if (!(foldedleft instanceof BoolExpr) && (foldedright instanceof BoolExpr)) {
                BoolExpr rightbool = (BoolExpr) foldedright;
                if (rightbool.value) {
                    if (binexp.op.name().equals(BinaryOp.AND.name())) {
                        return foldedleft;
                    } else if (binexp.op.name().equals(BinaryOp.OR.name())) {
                        return foldedright;
                    } else if (binexp.op.name().equals(BinaryOp.IMPLIES.name())) {
                        return foldedright;
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                } else {
                    if (binexp.op.name().equals(BinaryOp.AND.name())) {
                        return foldedright;
                    } else if (binexp.op.name().equals(BinaryOp.OR.name())) {
                        return foldedleft;
                    } else if (binexp.op.name().equals(BinaryOp.IMPLIES.name())) {
                        return new UnaryExpr(UnaryOp.NOT, foldedleft);
                    } else {
                        return new BinaryExpr(foldedleft,binexp.op,foldedright);
                    }
                }
            } else {
                return new BinaryExpr(foldedleft,binexp.op,foldedright);
            }
        } else if (expr instanceof UnaryExpr) {
            UnaryExpr uexpr = (UnaryExpr) expr;
            Expr foldedsubexpr = foldConstantsinExpr(uexpr.expr);
            if (uexpr.op.name().equals(UnaryOp.NOT.name()) && (foldedsubexpr instanceof BoolExpr)) {
                BoolExpr boolexpr = (BoolExpr) foldedsubexpr;
                return new BoolExpr(!boolexpr.value);
            } else if (uexpr.op.name().equals(UnaryOp.NEGATIVE) && (foldedsubexpr instanceof UnaryExpr)) {
                UnaryExpr subunexpr = (UnaryExpr) foldedsubexpr;
                if (subunexpr.op.name().endsWith(UnaryOp.NEGATIVE.name())) {
                    return foldedsubexpr;
                } else {
                    return new UnaryExpr(uexpr.op,foldedsubexpr);
                }
            } else {
                return new UnaryExpr(uexpr.op,foldedsubexpr);
            }
        } else if (expr instanceof AssignExpr) {
            AssignExpr aexpr = (AssignExpr) expr;
            return new AssignExpr(aexpr.lhs, foldConstantsinExpr(aexpr.expr));
        } else if (expr instanceof TernaryExpr) {
            TernaryExpr texpr = (TernaryExpr) expr;
            Expr foldedcond = foldConstantsinExpr(texpr.cond);
            if (foldedcond instanceof BoolExpr) {
                BoolExpr blexpr = (BoolExpr) foldedcond;
                if (blexpr.value) {
                    return (foldConstantsinExprs(texpr.thenExpr).get(0));
                } else {
                    return (foldConstantsinExprs(texpr.elseExpr).get(0));
                }
            } else {
                return new TernaryExpr(foldedcond,
                        foldConstantsinExprs(texpr.thenExpr),foldConstantsinExprs(texpr.elseExpr));
            }
        } else {
            return expr;
        }
    }

    private List<Expr> convertBooleanValuesToExitExprs(List<Expr> tempbody) {
        List<Expr> converted = new ArrayList<>();
        for (Expr expr : tempbody) {
            if (expr instanceof IfThenElseExpr) {
                IfThenElseExpr iteexpr = (IfThenElseExpr) expr;
                IfThenElseExpr iteconv = new IfThenElseExpr(iteexpr.cond,
                        iteexpr.thenExpr, convertBooleanValuesToExitExprs(iteexpr.elseExpr));
                converted.add(iteconv);
            } else {
                converted.add(convertBooleanValueToExitExpr(expr));
            }
        }
        return converted;
    }

    private Expr convertBooleanValueToExitExpr(Expr expr) {
        if (expr instanceof BoolExpr) {
            return new ExitExpr();
        } else {
            return expr;
        }
    }

    private List<Expr> convertIfThenElsesToTernary(List<Expr> tempbody) {
        List<Expr> converted = new ArrayList<>();
        for (Expr expr : tempbody) {
            if (expr instanceof IfThenElseExpr) {
                IfThenElseExpr itebody = (IfThenElseExpr) expr;
                Expr condexp = itebody.cond;
                List<Expr> thenexp = itebody.thenExpr;
                List<Expr> elsexp = itebody.elseExpr;
                IfThenElseExpr iteconv = new IfThenElseExpr(convertIfThenElseToTernaryExpr(condexp),
                        convertIfThenElsesToTernaryExprs(thenexp), convertIfThenElsesToTernaryExprs(elsexp));
                converted.add(iteconv);
            } else {
                converted.add(convertIfThenElseToTernaryExpr(expr));
            }
        }
        return converted;
    }

    private List<Expr> convertIfThenElsesToTernaryExprs(List<Expr> exprs) {
        List<Expr> converted = new ArrayList<>();
        for (Expr expr : exprs) {
            if (expr instanceof AssignExpr) {
                AssignExpr aexpr = (AssignExpr) expr;
                AssignExpr aconv = new AssignExpr(aexpr.lhs, convertIfThenElseToTernaryExpr(aexpr.expr));
                converted.add(aconv);
            }  else if (expr instanceof IfThenElseExpr) {
                IfThenElseExpr itexp = (IfThenElseExpr) expr;
                IfThenElseExpr iteconv = new IfThenElseExpr(convertIfThenElseToTernaryExpr(itexp.cond),
                        convertIfThenElsesToTernaryExprs(itexp.thenExpr), convertIfThenElsesToTernaryExprs(itexp.elseExpr));
                converted.add(iteconv);
            } else {
                converted.add(convertIfThenElseToTernaryExpr(expr));
            }
        }
        return converted;
    }

    private Expr convertIfThenElseToTernaryExpr(Expr expr) {
        if (expr instanceof IfThenElseExpr) {
            IfThenElseExpr iteexpr = (IfThenElseExpr) expr;
            if (iteexpr.thenExpr.size() > 1 || iteexpr.elseExpr.size() > 1) {
                if (iteexpr.thenExpr.size() > 1) {
                    Expr thenBinExp = rewriteOperandstoBinaryExpr(BinaryOp.AND, iteexpr.thenExpr);
                    iteexpr.thenExpr.clear();
                    iteexpr.thenExpr.add(thenBinExp);
                }
                if (iteexpr.elseExpr.size() > 1) {
                    Expr elseBinExp = rewriteOperandstoBinaryExpr(BinaryOp.AND, iteexpr.elseExpr);
                    iteexpr.elseExpr.clear();
                    iteexpr.elseExpr.add(elseBinExp);
                }
            }
            return new TernaryExpr(convertIfThenElseToTernaryExpr(iteexpr.cond), convertNestedIfThenElsesToTernaryExprs(iteexpr.thenExpr), convertNestedIfThenElsesToTernaryExprs(iteexpr.elseExpr));
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binexp = (BinaryExpr) expr;
            return new BinaryExpr(convertIfThenElseToTernaryExpr(binexp.left),
                    binexp.op, convertIfThenElseToTernaryExpr(binexp.right));
        } else if (expr instanceof UnaryExpr) {
            UnaryExpr unexp = (UnaryExpr) expr;
            return new UnaryExpr(unexp.op, convertIfThenElseToTernaryExpr(unexp.expr));
        } else if (expr instanceof CastExpr) {
            CastExpr castexp = (CastExpr) expr;
            return new CastExpr(castexp.type, convertIfThenElseToTernaryExpr(castexp.expr));
        } else if (expr instanceof AssignExpr) {
            AssignExpr assignexp = (AssignExpr) expr;
            return new AssignExpr(assignexp.lhs, convertIfThenElseToTernaryExpr(assignexp.expr));
        } else if (expr instanceof FunAppExpr) {
            FunAppExpr fexp = (FunAppExpr) expr;
            return new FunAppExpr(fexp.funNameExpr, convertNestedIfThenElsesToTernaryExprs(fexp.funArgExprs));
        } else {
            return expr;
        }
    }

    private List<Expr> convertNestedIfThenElsesToTernaryExprs(List<Expr> exprs) {
        List<Expr> converted = new ArrayList<>();
        for (Expr expr : exprs) {
            if (expr instanceof IfThenElseExpr) {
                IfThenElseExpr itexp = (IfThenElseExpr) expr;
                TernaryExpr iteconv = new TernaryExpr(convertIfThenElseToTernaryExpr(itexp.cond),
                        convertNestedIfThenElsesToTernaryExprs(itexp.thenExpr), convertNestedIfThenElsesToTernaryExprs(itexp.elseExpr));
                converted.add(iteconv);
            } else {
                converted.add(convertIfThenElseToTernaryExpr(expr));
            }
        }
        return converted;
    }

    private List<Expr> convertEqualitiesToAssignments(List<Expr> tempbody, List<String> insprops) {
        List<Expr> converted = new ArrayList<>();
        for (Expr expr : tempbody) {
            if (expr instanceof IfThenElseExpr) {
                IfThenElseExpr itebody = (IfThenElseExpr) expr;
                List<Expr> thenexp = itebody.thenExpr;
                List<Expr> elsexp = itebody.elseExpr;
                IfThenElseExpr iteconv = new IfThenElseExpr(itebody.cond,
                        convertAssignmentsinExprs(thenexp, insprops), convertAssignmentsinExprs(elsexp, insprops));
                converted.add(iteconv);
            } else {
                converted.addAll(convertAssignmentinExpr(expr, insprops));
            }
        }
        return converted;
    }

    private List<Expr> convertAssignmentsinExprs(List<Expr> exprs, List<String> insprops) {
        List<Expr> converted = new ArrayList<>();
        for (Expr expr : exprs) {
            if (expr instanceof BinaryExpr) {
                BinaryExpr binexp = (BinaryExpr) expr;
                if (binexp.op.name().equals(BinaryOp.EQUAL.name())) {
                    if (binexp.left instanceof IdExpr) {
                        IdExpr leftexp = (IdExpr) binexp.left;
                        String id = removeDelimiters(leftexp.id);
                        if (!insprops.contains(id)) {
                            converted.add(new AssignExpr(binexp.left, binexp.right));
                        }
                    }
                } else {
                    converted.addAll(convertAssignmentinExpr(binexp.left, insprops));
                    converted.addAll(convertAssignmentinExpr(binexp.right, insprops));
                }
            } else if (expr instanceof IfThenElseExpr) {
                IfThenElseExpr itexp = (IfThenElseExpr) expr;
                IfThenElseExpr iteconv = new IfThenElseExpr(itexp.cond,
                        convertAssignmentsinExprs(itexp.thenExpr, insprops), convertAssignmentsinExprs(itexp.elseExpr, insprops));
                converted.add(iteconv);
            } else {
                converted.add(expr);
            }
        }
        return converted;
    }

    private List<Expr> convertAssignmentinExpr(Expr expr, List<String> insprops) {
        List<Expr> converted = new ArrayList<>();
        if (expr instanceof BinaryExpr) {
            BinaryExpr binexp = (BinaryExpr) expr;
            if (binexp.op.name().equals(BinaryOp.EQUAL.name())) {
                if (binexp.left instanceof IdExpr) {
                    IdExpr leftexp = (IdExpr) binexp.left;
                    String id = removeDelimiters(leftexp.id);
                    if (!insprops.contains(id)) {
                        converted.add(new AssignExpr(binexp.left, binexp.right));
                    }
                }
            } else {
                converted.addAll(convertAssignmentinExpr(binexp.left, insprops));
                converted.addAll(convertAssignmentinExpr(binexp.right, insprops));
            }
        }  else if (expr instanceof IfThenElseExpr) {
            IfThenElseExpr itexpr = (IfThenElseExpr) expr;
            converted.add(new IfThenElseExpr(itexpr.cond, convertAssignmentsinExprs(itexpr.thenExpr, insprops),
                    convertAssignmentsinExprs(itexpr.elseExpr, insprops)));
        }
        return converted;
    }

    private List<Expr> inlinelocalstoexprs(List<Expr> exprs, List<Equation> lookup) {
        List<Expr> inlined = new ArrayList<>();
        for (Expr exp : exprs) {
            inlined.addAll(inlinelocalstoexp(exp, lookup));
        }
        return inlined;
    }

    private List<Expr> inlinelocalstoexp(Expr expr, List<Equation> lookup) {
        List<Expr> exprs = new ArrayList<>();
        if (expr instanceof IdExpr) {
            IdExpr idexp = (IdExpr) expr;
            if (idexp.id.startsWith("a!")) {
                for (Equation loc : lookup) {
                    if (idexp.id.toString().equals(loc.lhs.id.toString())) {
                        exprs.addAll((inlinelocalstoexp(loc.expr, lookup)));
                    }
                }
            } else {
                exprs.add(expr);
            }
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binexp = (BinaryExpr) expr;
            exprs.add(new BinaryExpr(inlinelocalstoexp(binexp.left, lookup).get(0), binexp.op, inlinelocalstoexp(binexp.right, lookup).get(0)));
        } else if (expr instanceof UnaryExpr) {
            UnaryExpr unexp = (UnaryExpr) expr;
            exprs.add(new UnaryExpr(unexp.op, inlinelocalstoexp(unexp.expr, lookup).get(0)));

        } else if (expr instanceof CastExpr) {
            CastExpr castexp = (CastExpr) expr;
            exprs.add(new CastExpr(castexp.type, inlinelocalstoexp(castexp.expr, lookup).get(0)));
        } else if (expr instanceof IfThenElseExpr) {
            IfThenElseExpr itexp = (IfThenElseExpr) expr;
            exprs.add(new IfThenElseExpr(inlinelocalstoexp(itexp.cond, lookup).get(0),
                    inlinelocalstoexprs(itexp.thenExpr, lookup),
                    inlinelocalstoexprs(itexp.elseExpr, lookup)));
        } else if (expr instanceof FunAppExpr) {
            FunAppExpr fexp = (FunAppExpr) expr;
            exprs.add(new FunAppExpr(fexp.funNameExpr, inlinelocalstoexprs(fexp.funArgExprs, lookup)));
        } else {
            exprs.add(expr);
        }
        return exprs;
    }


    private Map<String, Expr> fastlocals(LetexpContext letCtx) {
        Map<String, Expr> fastlocs = new HashMap<>();
        List<Equation> locs = new ArrayList<>();
        if (letCtx == null) {
            return fastlocs;
        }
        for (LocalContext loc : letCtx.local()) {
            ExprContext locctx = loc.expr();
            locs.add(new Equation(loc(loc), new IdExpr(loc(loc),loc.ID().getText()), expr(locctx)));
            fastlocs.put(loc.ID().getText(), expr(locctx));
        }
        if (letCtx.body().letexp() !=null) {
            locs.addAll(locals(letCtx.body().letexp()));
            fastlocs.putAll(fastlocals(letCtx.body().letexp()));
        }
        return fastlocs;
    }

    private List<Equation> locals(LetexpContext letCtx) {
        List<Equation> locs = new ArrayList<>();
        if (letCtx == null) {
            return locs;
        }
        for (LocalContext loc : letCtx.local()) {
            ExprContext locctx = loc.expr();
            locs.add(new Equation(loc(loc), new IdExpr(loc(loc),loc.ID().getText()), expr(locctx)));
        }
        if (letCtx.body().letexp() !=null) {
            locs.addAll(locals(letCtx.body().letexp()));
        }
        return locs;
    }

    private List<Expr> body(BodyContext letCtx) {
        List<Expr> assignments = new ArrayList<>();
        if (letCtx.letexp() != null) {
            return body(letCtx.letexp().body());
        } else {
            ExprContext ctx = letCtx.expr();
            if (ctx instanceof BinaryExprContext) {
                BinaryExprContext bctx = (BinaryExprContext) ctx;
                for (ExprContext ectx : bctx.expr()) {
                    assignments.add(expr(ectx));
                }
            } else if (ctx instanceof BoolExprContext) {
                assignments.add(new ExitExpr());
            } else {
                assignments.add(expr(letCtx.expr()));
            }
            return assignments;
        }
    }


    private Type type(TypeContext ctx) {
        return (Type) ctx.accept(this);
    }

    @Override
    public Type visitIntType(IntTypeContext ctx) {
        return NamedType.INT;
    }

    @Override
    public Type visitBoolType(BoolTypeContext ctx) {
        return NamedType.BOOL;
    }

    @Override
    public Type visitRealType(RealTypeContext ctx) {
        return NamedType.REAL;
    }

    public Expr expr(ExprContext ctx) {
        return (Expr) ctx.accept(this);
    }

    @Override
    public Expr visitIdExpr(IdExprContext ctx) {
        if (ctx.ID().getText().equals("%init")) {
            return new BinaryExpr(loc(ctx), new IdExpr(loc(ctx), "init"), BinaryOp.EQUAL, new IntExpr(loc(ctx), BigInteger.valueOf(0)));
        } else {
            return new IdExpr(loc(ctx), ctx.ID().getText());
        }

    }

    @Override
    public Expr visitIntExpr(IntExprContext ctx) {
        return new IntExpr(loc(ctx), new BigInteger(ctx.INT().getText()));
    }

    @Override
    public Expr visitRealExpr(RealExprContext ctx) {
        return new RealExpr(loc(ctx), new BigDecimal(ctx.REAL().getText()));
    }

    @Override
    public Expr visitBoolExpr(BoolExprContext ctx) {
        return new BoolExpr(loc(ctx), ctx.BOOL().getText().equals("true"));
    }

    @Override
    public Expr visitCastExpr(CastExprContext ctx) {
        return new CastExpr(loc(ctx), getCastType(ctx.op.getText()), expr(ctx.expr()));
    }

    private Type getCastType(String cast) {
        switch (cast) {
            case "to_real":
                return NamedType.REAL;
            case "to_int":
                return NamedType.INT;
            default:
                throw new IllegalArgumentException("Unknown cast: " + cast);
        }
    }

    @Override
    public Expr visitNotExpr(NotExprContext ctx) {
        return new UnaryExpr(loc(ctx), UnaryOp.NOT, expr(ctx.expr()));
    }

    @Override
    public Expr visitNegateExpr(NegateExprContext ctx) {return new UnaryExpr(loc(ctx),UnaryOp.NEGATIVE, expr(ctx.expr()));}

    @Override
    public Expr visitBinaryExpr(BinaryExprContext ctx) {
        String op = ctx.op.getText();
        Expr left = expr(ctx.expr(0));
        List<Expr> right = new ArrayList<>();
        for (ExprContext ectx : ctx.expr().subList(1,ctx.expr().size())) {
            right.add(expr(ectx));
        }
        return new BinaryExpr(loc(ctx.op), left, BinaryOp.fromString(op), rewriteOperandstoBinaryExpr(BinaryOp.fromString(op), right));
    }

    private Expr rewriteOperandstoBinaryExpr(BinaryOp binaryOp, List<Expr> right) {
        if (right.size() > 1) {
            return new BinaryExpr(right.get(0), binaryOp, rewriteOperandstoBinaryExpr(binaryOp, right.subList(1, right.size())));
        } else {
            return right.get(0);
        }
    }

    @Override
    public Expr visitFunAppExpr(FunAppExprContext ctx) {
        List<Expr> args = new ArrayList<>();
        for (ExprContext ectx : ctx.expr()) {
            args.add(expr(ectx));
        }
        return new FunAppExpr(loc(ctx), new IdExpr(ctx.ID().getText()), args);
    }

    @Override
    public Expr visitIfThenElseExpr(IfThenElseExprContext pctx) {
        List<Expr> thenbd = new ArrayList<>();
        List<Expr> elsebd = new ArrayList<>();
        ExprContext thenectx = pctx.expr(1);
        ExprContext elseectx = pctx.expr(2);
        if (thenectx instanceof BinaryExprContext) {
            BinaryExprContext tbctx = (BinaryExprContext) thenectx;
            if (tbctx.op.getText().equals("and")) {
                if (tbctx.expr().get(0) instanceof BinaryExprContext) {
                    BinaryExprContext bctx = (BinaryExprContext) tbctx.expr().get(0);
                    if(bctx.op.getText().equals("=")) {
                        for (ExprContext ctxs : tbctx.expr()) {
                            thenbd.add(expr(ctxs));
                        }
                    } else {
                        thenbd.add(expr(tbctx));
                    }
                } else {
                    thenbd.add(expr(tbctx));
                }
            } else {
                thenbd.add(expr(tbctx));
            }
        } else {
            thenbd.add(expr(thenectx));
        }
        if (elseectx instanceof BinaryExprContext) {
            BinaryExprContext tbctx = (BinaryExprContext) elseectx;
            if (tbctx.op.getText().equals("and")) {
                if (tbctx.expr().get(0) instanceof BinaryExprContext) {
                    BinaryExprContext bctx = (BinaryExprContext) tbctx.expr().get(0);
                    if(bctx.op.getText().equals("=")) {
                        for (ExprContext ctxs : tbctx.expr()) {
                            elsebd.add(expr(ctxs));
                        }
                    } else {
                        elsebd.add(expr(tbctx));
                    }
                } else {
                    elsebd.add(expr(tbctx));
                }
            } else {
                elsebd.add(expr(tbctx));
            }
        } else {
            elsebd.add(expr(elseectx));
        }
        return new IfThenElseExpr(loc(pctx), expr(pctx.expr(0)), thenbd, elsebd);
    }

    private static Location loc(ParserRuleContext ctx) {
        return loc(ctx.getStart());
    }

    private static Location loc(Token token) {
        return new Location(token.getLine(), token.getCharPositionInLine());
    }

    private static String rename(String id) {
        if (id.startsWith("$")) {
            String renamed = id.substring(1).replaceAll("[~.]","_");
            return renamed;
        } else {
            String renamed = id.replaceAll("[%~.]","_");
            return renamed;
        }
    }
}