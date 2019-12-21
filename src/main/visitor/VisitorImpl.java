package main.visitor;
import javafx.util.Pair;
import main.ast.node.*;
import main.ast.node.Program;
import main.ast.node.declaration.*;
import main.ast.node.declaration.handler.*;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.operators.BinaryOperator;
import main.ast.node.expression.operators.UnaryOperator;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.node.statement.*;
import main.ast.type.Type;
import main.ast.type.actorType.ActorType;
import main.ast.type.arrayType.ArrayType;
import main.ast.type.noType.NoType;
import main.ast.type.primitiveType.BooleanType;
import main.ast.type.primitiveType.IntType;
import main.ast.type.primitiveType.StringType;
import main.symbolTable.SymbolTable;
import main.symbolTable.SymbolTableActorItem;
import main.symbolTable.SymbolTableHandlerItem;
import main.symbolTable.SymbolTableItem;
import main.symbolTable.itemException.ItemNotFoundException;
import main.symbolTable.symbolTableVariableItem.SymbolTableLocalVariableItem;
import main.symbolTable.symbolTableVariableItem.SymbolTableVariableItem;
import main.visitor.nameAnalyser.TypeScope;

import java.util.ArrayList;

public class VisitorImpl implements Visitor {

    TypeScope currentScop;

    SymbolTable currentActorSt;
    SymbolTable currentHandlerSt;

    boolean isStatement = false;
    boolean isMsgHandlerCall = false;
    boolean inActorInt = false;

    protected void visitStatement( Statement stat )
    {
        if( stat == null )
            return;
        else if( stat instanceof MsgHandlerCall )
            this.visit( ( MsgHandlerCall ) stat );
        else if( stat instanceof Block )
            this.visit( ( Block ) stat );
        else if( stat instanceof Conditional )
            this.visit( ( Conditional ) stat );
        else if( stat instanceof For )
            this.visit( ( For ) stat );
        else if( stat instanceof Break )
            this.visit( ( Break ) stat );
        else if( stat instanceof Continue )
            this.visit( ( Continue ) stat );
        else if( stat instanceof Print )
            this.visit( ( Print ) stat );
        else if( stat instanceof Assign )
            this.visit( ( Assign ) stat );
    }

    protected void visitExpr( Expression expr )
    {
        if( expr == null )
            return;
        else if( expr instanceof UnaryExpression )
            this.visit( ( UnaryExpression ) expr );
        else if( expr instanceof BinaryExpression )
            this.visit( ( BinaryExpression ) expr );
        else if( expr instanceof ArrayCall )
            this.visit( ( ArrayCall ) expr );
        else if( expr instanceof ActorVarAccess )
            this.visit( ( ActorVarAccess ) expr );
        else if( expr instanceof Identifier )
            this.visit( ( Identifier ) expr );
        else if( expr instanceof Self )
            this.visit( ( Self ) expr );
        else if( expr instanceof Sender )
            this.visit( ( Sender ) expr );
        else if( expr instanceof BooleanValue )
            this.visit( ( BooleanValue ) expr );
        else if( expr instanceof IntValue )
            this.visit( ( IntValue ) expr );
        else if( expr instanceof StringValue )
            this.visit( ( StringValue ) expr );
    }
    public SymbolTableItem checkExistenceOfKey(SymbolTable st, String key){
        try {
            return  st.get(key);
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    private Pair<Type, String> getTypeOfIdentifier(Identifier name) {
        SymbolTableItem var1 =
                checkExistenceOfKey(currentHandlerSt, SymbolTableLocalVariableItem.STARTKEY + name.getName());

        String error = null;
        if (var1 == null)
            var1 = checkExistenceOfKey(currentActorSt, SymbolTableLocalVariableItem.STARTKEY + name.getName());
        if (var1 == null)
            error = "Line:" + name.getLine() + ":variable " + name.getName() + " is not declared";
        else {
            if (var1 instanceof SymbolTableVariableItem)
                return new Pair<Type, String>(((SymbolTableVariableItem) var1).getType(), error);
            else if (var1 instanceof SymbolTableLocalVariableItem)
                return new Pair<Type, String>(((SymbolTableLocalVariableItem) var1).getType(), error);
        }
        return new Pair<Type, String>(new NoType(), error);
    }

    @Override
    public void visit(Program program) {
        //TODO: implement appropriate visit functionality
        for(ActorDeclaration actorDeclaration : program.getActors()){
            actorDeclaration.accept(this);
        }
        program.getMain().accept(this);
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        //TODO: implement appropriate visit functionality
        try {
            SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY + actorDeclaration.getName().getName());
            currentActorSt = actorItem.getActorSymbolTable();
        } catch (ItemNotFoundException ignored) {
        }
        visitExpr(actorDeclaration.getName());

        Identifier parent = actorDeclaration.getParentName();
        visitExpr(parent);
        if (parent != null) {
            String key = SymbolTableActorItem.STARTKEY + parent.getName();
            SymbolTableActorItem a = (SymbolTableActorItem) SymbolTable.root.getSymbolTableItems().get(key);
            if (a == null)
                System.out.println("Line:" + parent.getLine() + ":actor " + parent.getName() + " is not declared");
        }
        for(VarDeclaration varDeclaration: actorDeclaration.getKnownActors())
            varDeclaration.accept(this);

        for(VarDeclaration actorVar : actorDeclaration.getKnownActors()) {
            String key = SymbolTableActorItem.STARTKEY + actorVar.getType().toString();
            SymbolTableActorItem a = (SymbolTableActorItem) SymbolTable.root.getSymbolTableItems().get(key);
            if (a == null)
                System.out.println("Line:" + actorVar.getLine() + ":actor " + actorVar.getType().toString() + " is not declared");
        }

        for(VarDeclaration varDeclaration: actorDeclaration.getActorVars())
            varDeclaration.accept(this);

        if(actorDeclaration.getInitHandler() != null)
            actorDeclaration.getInitHandler().accept(this);

        for(MsgHandlerDeclaration msgHandlerDeclaration: actorDeclaration.getMsgHandlers())
            msgHandlerDeclaration.accept(this);
    }

    @Override
    public void visit(HandlerDeclaration handlerDeclaration) {
        try {
            String handlerKey = SymbolTableHandlerItem.STARTKEY + handlerDeclaration.getName().getName();
            SymbolTableHandlerItem hItem = (SymbolTableHandlerItem) currentActorSt.get(handlerKey);
            currentHandlerSt = hItem.getHandlerSymbolTable();
        } catch (ItemNotFoundException ignored) {
        }
        visitExpr(handlerDeclaration.getName());
        for(VarDeclaration argDeclaration: handlerDeclaration.getArgs())
            argDeclaration.accept(this);
        for(VarDeclaration localVariable: handlerDeclaration.getLocalVars())
            localVariable.accept(this);
        isStatement = true;
        for(Statement statement : handlerDeclaration.getBody())
            visitStatement(statement);
        isStatement = false;
    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
        //TODO: implement appropriate visit functionality
        visitExpr(varDeclaration.getIdentifier());
    }

    @Override
    public void visit(Main mainActors) {
        if(mainActors == null)
            return;
        for(ActorInstantiation mainActor : mainActors.getMainActors())
            mainActor.accept(this);

        for(ActorInstantiation mainActor : mainActors.getMainActors()) {
            String key = SymbolTableActorItem.STARTKEY + mainActor.getType().toString();

            SymbolTableActorItem a = (SymbolTableActorItem) SymbolTable.root.getSymbolTableItems().get(key);

            if (a == null)
                System.out.println("Line:" + mainActor.getLine() + ":actor " + mainActor.getType().toString() + " is not declared");
        }
    }

//    private boolean checkTypeCast(Identifier right, Identifier left){
//        while (true) {
//            if (right.getClass().getName().equals(left.getClass().getName()))
//                return  true;
//            try {
//                SymbolTableActorItem right_item = (SymbolTableActorItem) SymbolTable.root.get(
//                        SymbolTableActorItem.STARTKEY + ((Identifier) right).getName());
//                right_item = (SymbolTableActorItem) SymbolTable.root.get(
//                        SymbolTableActorItem.STARTKEY + (right_item.getParentName()));
//                right = right_item.getActorDeclaration().getName();
//            } catch (ItemNotFoundException ex) {
//                return false;
//            }
//        }
//    }

    @Override
    public void visit(ActorInstantiation actorInstantiation) {
        if(actorInstantiation == null)
            return;

        visitExpr(actorInstantiation.getIdentifier());
        isStatement = true;
        for(Identifier knownActor : actorInstantiation.getKnownActors())
            visitExpr(knownActor);
        isStatement = false;

        ArrayList<Identifier>  x1 = actorInstantiation.getKnownActors();

        String actor_name = actorInstantiation.getType().toString();

        try {
            SymbolTableActorItem actor = (SymbolTableActorItem) SymbolTable.root.get(
                    SymbolTableActorItem.STARTKEY + actor_name);
            ArrayList<VarDeclaration> x2 = actor.getActorDeclaration().getKnownActors();
            if(x1.size() != x2.size())
            {
                System.out.println("errr");
            }
            else for(int i=0;i<x1.size(); i++)
            {
                if((x1.get(i).getType() instanceof NoType) || (x2.get(i).getType() instanceof NoType))
                    continue;
                if(!(x1.get(i).getType() instanceof ActorType) || !(x2.get(i).getType() instanceof NoType))
                    System.out.println("errr");
                else
                {
                    Identifier right = x1.get(i);
                    Identifier left = x2.get(i).getIdentifier();

                    while (true) {
                        if (right.getClass().getName().equals(left.getClass().getName()))
                            break;
                        try {
                            SymbolTableActorItem right_item = (SymbolTableActorItem) SymbolTable.root.get(
                                    SymbolTableActorItem.STARTKEY + right.getName());
                            right_item = (SymbolTableActorItem) SymbolTable.root.get(
                                    SymbolTableActorItem.STARTKEY + (right_item.getParentName()));
                            right = right_item.getActorDeclaration().getName();
                        } catch (ItemNotFoundException ex) {
                            System.out.println("err");
                            break;
                        }
                    }
                }
            }
        } catch (ItemNotFoundException ex) {
            return;
        }

        inActorInt = true;
        for(Expression initArg : actorInstantiation.getInitArgs())
            visitExpr(initArg);
        inActorInt = false;
    }

    @Override
    public void visit(UnaryExpression unaryExpression) {
        boolean unaryError = false;
        if(unaryExpression == null)
            return;

        visitExpr(unaryExpression.getOperand());

        if (unaryExpression.getOperand().getType() instanceof NoType)
            return;
//        not, minus, preinc, postinc, predec, postdec
        UnaryOperator operator = unaryExpression.getUnaryOperator();
        if (operator.equals(UnaryOperator.not)){
            if (! unaryExpression.getOperand().getType().toString().equals(new BooleanType().toString())){
                System.out.println("Line:"+unaryExpression.getLine()+":unsupported operand type for "+
                        unaryExpression.getUnaryOperator());
                unaryExpression.setType(new NoType());
            }
            else
                unaryExpression.setType(new BooleanType());
        }

        else {
            if (! unaryExpression.getOperand().getType().toString().equals(new IntType().toString())){
                unaryError = true;
                System.out.println("Line:"+unaryExpression.getLine()+":unsupported operand type for "+
                        unaryExpression.getUnaryOperator());
                unaryExpression.setType(new NoType());
            }
            else if (operator.equals(UnaryOperator.postdec) || operator.equals(UnaryOperator.postinc) ||
                    operator.equals(UnaryOperator.predec) || operator.equals(UnaryOperator.preinc) ||
                    operator.equals(UnaryOperator.minus)) {
                try {
                    Identifier id = (Identifier) unaryExpression.getOperand();
                } catch (ClassCastException ignored) {
                    unaryError = true;
                    System.out.println("Line:" + unaryExpression.getLine() + ":lvalue required as increment/decrement operand");
                    unaryExpression.setType(new NoType());
                }
            }
            if(!unaryError)
                unaryExpression.setType(new IntType());
        }
    }

    @Override
    public void visit(BinaryExpression binaryExpression) {
        if(binaryExpression == null)
            return;

        visitExpr(binaryExpression.getLeft());
        visitExpr(binaryExpression.getRight());

//        assign, eq, neq, gt, lt, add, sub, mult, div, mod, and, or
        BinaryOperator binaryOperator=binaryExpression.getBinaryOperator();
        Expression right = binaryExpression.getRight();
        Expression left = binaryExpression.getLeft();

        boolean hasError = false;
        // Computational operators
        if(binaryOperator.equals(BinaryOperator.add)||binaryOperator.equals(BinaryOperator.sub)||
                binaryOperator.equals(BinaryOperator.mult)||binaryOperator.equals(BinaryOperator.div)||
                binaryOperator.equals(BinaryOperator.mod)){
            if(right.getType() instanceof IntType && left.getType() instanceof IntType)
                binaryExpression.setType(new IntType());
            else
                hasError = true;
        }
        // Logical operations
        else if(binaryOperator.equals(BinaryOperator.and)||binaryOperator.equals(BinaryOperator.or)){
            if(right.getType() instanceof BooleanType && left.getType() instanceof BooleanType) {
                binaryExpression.setType(new BooleanType());
            }
            else
                hasError = true;
        }
        // Comparative operations
        else if (binaryOperator.equals(BinaryOperator.gt) || binaryOperator.equals(BinaryOperator.lt)){
            if (left.getType() instanceof IntType && right.getType() instanceof IntType)
                binaryExpression.setType(new BooleanType());
            else
                hasError = true;
        }
        else if (binaryOperator.equals(BinaryOperator.eq) || binaryOperator.equals(BinaryOperator.neq)) {

            if (left.getType().toString().equals(right.getType().toString())){
                if (left.getType() instanceof ArrayType){
                    ArrayType leftArray = (ArrayType)left.getType();
                    ArrayType rightArray = (ArrayType)right.getType();
                    if (leftArray.getSize() == rightArray.getSize())
                        binaryExpression.setType(new BooleanType());
                    else
                        hasError = true;
                }
                else
                    binaryExpression.setType(new BooleanType());
            }
            else
                hasError = true;
        }
        if(hasError){
            binaryExpression.setType(new NoType());
            System.out.println("Line:"+binaryExpression.getLine()+":unsupported operand type for "+binaryOperator.toString());
        }
    }

    @Override
    public void visit(ArrayCall arrayCall) {
        visitExpr(arrayCall.getArrayInstance());
        visitExpr(arrayCall.getIndex());
        arrayCall.setType(new IntType());
    }

    // This method calls just in self.* statements(no a.foo() for example)
    @Override
    public void visit(ActorVarAccess actorVarAccess) {
        if (inActorInt) {
            System.out.println("Line:" + actorVarAccess.getLine() + ":self doesn't refer to any actor");
            actorVarAccess.setType(new NoType());
            return;
        }
        if (actorVarAccess == null)
            return;

        visitExpr(actorVarAccess.getVariable());
        Pair<Type, String> type = getTypeOfIdentifier(actorVarAccess.getVariable());
        if (type.getValue() != null)
        {
            System.out.println(type.getValue());
            actorVarAccess.setType(new NoType());
        }
        else
            actorVarAccess.setType(type.getKey());
    }

    @Override
    public void visit(Identifier identifier) {
        if(identifier == null)
            return;
        if (!inActorInt && !isMsgHandlerCall && isStatement) {
            Pair<Type, String> p = getTypeOfIdentifier(identifier);
            if (p.getValue() != null)
            {
                System.out.println(p.getValue());
                identifier.setType(new NoType());
            }
            else
                identifier.setType(p.getKey());
        }
    }

    @Override
    public void visit(Self self) {
        //TODO: implement appropriate visit functionality
    }

    @Override
    public void visit(Sender sender) {
        if (currentHandlerSt.getName() == "initial")
            System.out.println("Line:" + sender.getLine() + ":no sender in initial msghandler");
        sender.setType(new ActorType(new Identifier(sender.toString())));
    }

    @Override
    public void visit(BooleanValue value) {
        //TODO: implement appropriate visit functionality
    }

    @Override
    public void visit(IntValue value) {
        //TODO: implement appropriate visit functionality
    }

    @Override
    public void visit(StringValue value) {
        //TODO: implement appropriate visit functionality
    }

    @Override
    public void visit(Block block) {
        //TODO: implement appropriate visit functionality
        if(block == null)
            return;
        for(Statement statement : block.getStatements())
            visitStatement(statement);
    }

    @Override
    public void visit(Conditional conditional) {
        Expression condExpression = conditional.getExpression();
        visitExpr(condExpression);
        if (condExpression.getType() == null || !(condExpression.getType() instanceof BooleanType))
            System.out.println("Line:"+conditional.getLine()+":condition type must be Boolean");
        visitStatement(conditional.getThenBody());
        visitStatement(conditional.getElseBody());
    }

    @Override
    public void visit(For loop) {
        TypeScope prevScop = currentScop;
        currentScop = TypeScope.FOR;

        visitStatement(loop.getInitialize());

        Identifier id = (Identifier) loop.getInitialize().getlValue();
        String error = null;
        Pair<Type, String> valType = getTypeOfIdentifier(id);

        if ((valType.getValue() != null) && !(valType.getKey() instanceof IntType) &&
                !(valType.getKey() instanceof NoType)){
            error = "unsupported operand type for assignment";
            System.out.println("Line:"+id.getLine() + ":" + error);
        }

        visitExpr(loop.getCondition());
        Expression cond = loop.getCondition();
        if(cond.getType() == null || !(cond.getType() instanceof BooleanType))
            System.out.println("Line:"+loop.getLine()+":condition type must be Boolean");
        visitStatement(loop.getUpdate());
        visitStatement(loop.getBody());

        currentScop = prevScop;
    }

    @Override
    public void visit(Break breakLoop) {
        if(currentScop != TypeScope.FOR)
            System.out.println("Line:" + breakLoop.getLine() + ":break statement not within loop");
    }

    @Override
    public void visit(Continue continueLoop) {
        if(currentScop != TypeScope.FOR)
            System.out.println("Line:" + continueLoop.getLine() + ":continue statement not within loop");
    }


    @Override
    public void visit(MsgHandlerCall msgHandlerCall) {
        isMsgHandlerCall = true;

        if(msgHandlerCall == null)
            return;
        try {
            Identifier actorName = (Identifier) msgHandlerCall.getInstance();
            Identifier msgName = msgHandlerCall.getMsgHandlerName();

            visitExpr(actorName);
            visitExpr(msgName);

            for (Expression argument : msgHandlerCall.getArgs())
                visitExpr(argument);

            Pair<Type, String> type = getTypeOfIdentifier(actorName);
            if (type.getValue() != null) {
                System.out.println(type.getValue());
            }
            else if (!(type.getKey() instanceof ActorType))
                System.out.println("Line:"+msgHandlerCall.getLine()+":variable " + actorName.getName()+" is not callable");

            else{
                SymbolTableActorItem acStIt = null;
                try {
                    acStIt = (SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY +
                            type.getKey().toString());
                    SymbolTable acSt = acStIt.getActorSymbolTable();
                    try {
                        // If it can not find this msgHandler or can not cast it to msgHandlerItem go to catch
                        SymbolTableHandlerItem msgItem = (SymbolTableHandlerItem) acSt.get(SymbolTableHandlerItem.STARTKEY +
                                msgHandlerCall.getMsgHandlerName().getName());
                        HandlerDeclaration handlerDec = msgItem.getHandlerDeclaration();
                        ArrayList<VarDeclaration> origHandlerArgs = handlerDec.getArgs();
                        ArrayList<Expression> msgCallArgs = msgHandlerCall.getArgs();

                        if (origHandlerArgs.size() != msgCallArgs.size()) {
                            System.out.println("Line:" + msgHandlerCall.getLine() + ":arguments do not match with definition");
                            isMsgHandlerCall = false;
                            return;
                        }
                        for (int i = 0; i < origHandlerArgs.size(); i++){
                            if (!(origHandlerArgs.get(i).getType().toString().equals(msgCallArgs.get(i).toString()))){
                                System.out.println("Line:" + msgHandlerCall.getLine() + ":arguments do not match with definition");
                                isMsgHandlerCall = false;
                                return;
                            }
                        }

                    }catch (Exception e){
                        System.out.println("Line:"+msgHandlerCall.getLine()+":there is no msghandler name " +
                                msgHandlerCall.getMsgHandlerName() + "in actor " + acStIt.getKey());
                    }
                } catch (ItemNotFoundException e) {
                    e.getStackTrace();
                }
            }

        }
        catch(NullPointerException npe) {
            System.out.println("null pointer exception occurred");
        }
        isMsgHandlerCall = false;
    }

    @Override
    public void visit(Print print) {
        if(print == null)
            return;
        visitExpr(print.getArg());
        Type argType = print.getArg().getType();
        if((!argType.toString().equals((new NoType()).toString())) && (argType == null || !(argType instanceof BooleanType ||  argType instanceof StringType ||
                argType instanceof IntType || argType instanceof ArrayType)))
            System.out.println("Line:"+print.getLine()+":unsupported type for print");
    }


    @Override
    public void visit(Assign assign) {
        visitExpr(assign.getlValue());
        visitExpr(assign.getrValue());

        Expression left = assign.getlValue();
        Expression right = assign.getrValue();

        if (!(left instanceof Identifier) && !(left instanceof ArrayCall) && !(left instanceof ActorVarAccess))
            System.out.println("Line:"+left.getLine()+":left side of assignment must be lvalue");

        if((left.getType() instanceof NoType) || (right.getType() instanceof NoType))
            return;

        // Check equality of left and right type
        if(!(left.getType().toString().equals(right.getType().toString())) && !((left.getType() instanceof ActorType)))
            System.out.println("Line:"+left.getLine()+":unsupported operand type for "+assign.toString());
        // Check equality of array size if left and right are both array
        else if(left.getType() instanceof ArrayType) {

            ArrayType arr1 = (ArrayType) left.getType();
            ArrayType arr2 = (ArrayType) right.getType();
            if (arr1.getSize() != arr2.getSize())
                System.out.println("Line:" + assign.getLine() + ":unsupported operand type for " + assign.toString());
            return;
        }
        else if(left.getType() instanceof ActorType)
        {
            while (true) {
                if (right.getClass().getName().equals(left.getClass().getName()))
                    break;
                try {
                    SymbolTableActorItem right_item = (SymbolTableActorItem) SymbolTable.root.get(
                            SymbolTableActorItem.STARTKEY + ((Identifier) right).getName());
                    right_item = (SymbolTableActorItem) SymbolTable.root.get(
                            SymbolTableActorItem.STARTKEY + (right_item.getParentName()));
                    right = right_item.getActorDeclaration().getName();
                } catch (ItemNotFoundException ex) {
                    return;
                }
            }
        }
    }
}
