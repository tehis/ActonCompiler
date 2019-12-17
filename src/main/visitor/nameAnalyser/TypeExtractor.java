package main.visitor.nameAnalyser;

import javafx.util.Pair;
import main.ast.node.Main;
import main.ast.node.Program;
import main.ast.node.declaration.ActorDeclaration;
import main.ast.node.declaration.ActorInstantiation;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.declaration.handler.HandlerDeclaration;
import main.ast.node.declaration.handler.MsgHandlerDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.operators.BinaryOperator;
import main.ast.node.expression.operators.UnaryOperator;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.node.statement.*;
import main.ast.type.Type;
import main.ast.type.arrayType.ArrayType;
import main.ast.type.noType.NoType;
import main.ast.type.primitiveType.BooleanType;
import main.ast.type.primitiveType.IntType;
import main.ast.type.primitiveType.StringType;
import main.symbolTable.*;
import main.symbolTable.itemException.ItemNotFoundException;
import main.symbolTable.symbolTableVariableItem.SymbolTableLocalVariableItem;
import main.symbolTable.symbolTableVariableItem.SymbolTableVariableItem;
import main.visitor.VisitorImpl;

public class TypeExtractor extends VisitorImpl {
    TypeScope currentScop;

    SymbolTable currentActorSt;
    SymbolTable currentHandlerSt;

    boolean isStatement = false;

    public SymbolTableItem checkExistenceOfKey(SymbolTable st, String key){
        try {
            return  st.get(key);
        } catch (ItemNotFoundException e) {
                return null;
        }
    }

  public Pair<Type, String> getTypeOfIdentifier(Identifier name) {
      Type ret;
      SymbolTableItem var1 =
              checkExistenceOfKey(currentHandlerSt, SymbolTableLocalVariableItem.STARTKEY + name.getName());

      String error = null;
      if (var1 == null)
          var1 = checkExistenceOfKey(currentActorSt, SymbolTableLocalVariableItem.STARTKEY + name.getName());
      if (var1 == null)
          error = "Line:" + name.getLine() + ":variable " + name + " is not declared";
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
        //TODO: implement appropriate visit functionality
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

    @Override
    public void visit(ActorInstantiation actorInstantiation) {
        //TODO: implement appropriate visit functionality
        if(actorInstantiation == null)
            return;

        visitExpr(actorInstantiation.getIdentifier());
        for(Identifier knownActor : actorInstantiation.getKnownActors())
            visitExpr(knownActor);
        for(Expression initArg : actorInstantiation.getInitArgs())
            visitExpr(initArg);

    }

    @Override
    public void visit(UnaryExpression unaryExpression) {
        //TODO: implement appropriate visit functionality
        if(unaryExpression == null)
            return;

        visitExpr(unaryExpression.getOperand());

        System.out.println("unary");

        if (unaryExpression.getUnaryOperator().equals(UnaryOperator.not)){
            if (! unaryExpression.getOperand().getType().equals(BooleanType.class)){
                System.out.println("unary type erorr bool");
                unaryExpression.setType(new NoType());
            }
            unaryExpression.setType(new BooleanType());
        }

        else {
            if (! unaryExpression.getOperand().getType().toString().equals(new IntType().toString())){
                System.out.println("unary type erorr int");
                unaryExpression.setType(new NoType());
            }
            unaryExpression.setType(new IntType());
        }
        System.out.println(unaryExpression.getType().toString());
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
            if(right.getType() instanceof BooleanType && left.getType() instanceof BooleanType)
                binaryExpression.setType(new BooleanType());
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
        // Assignment statement
        else if(binaryOperator.equals(BinaryOperator.assign)) {
//            Assign
        }
        if(hasError){
            binaryExpression.setType(new NoType());
            System.out.println("Line:"+binaryExpression.getLine()+":unsupported operand type for "+binaryOperator.toString());
        }
    }

    @Override
    public void visit(ArrayCall arrayCall) {
        //TODO: implement appropriate visit functionality
        visitExpr(arrayCall.getArrayInstance());
        visitExpr(arrayCall.getIndex());
    }

    @Override
    public void visit(ActorVarAccess actorVarAccess) {
        //TODO: implement appropriate visit functionality
        if(actorVarAccess == null)
            return;

        visitExpr(actorVarAccess.getVariable());
//        actorVarAccess.setType(get_type_in_actor_vars(actorVarAccess.getVariable()));
    }

    @Override
    public void visit(Identifier identifier) {
        if(identifier == null)
            return;
        if (isStatement){
            Pair<Type, String> p = getTypeOfIdentifier(identifier);
            if(p.getValue() != null)
                System.out.println(p.getValue());
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
        //TODO: implement appropriate visit functionality
        if(msgHandlerCall == null) {
            return;
        }
        try {
            visitExpr(msgHandlerCall.getInstance());
            visitExpr(msgHandlerCall.getMsgHandlerName());
            for (Expression argument : msgHandlerCall.getArgs())
                visitExpr(argument);
        }
        catch(NullPointerException npe) {
            System.out.println("null pointer exception occurred");
        }
    }

    @Override
    public void visit(Print print) {
        if(print == null)
            return;
        visitExpr(print.getArg());
        Type argType = print.getArg().getType();
        if(argType == null || !(argType instanceof BooleanType ||  argType instanceof StringType ||
                argType instanceof IntType || argType instanceof ArrayType))
            System.out.println("Line:"+print.getLine()+":unsupported type for ptint");
    }


    @Override
    public void visit(Assign assign) {
        //TODO: implement appropriate visit functionality
        visitExpr(assign.getlValue());
        visitExpr(assign.getrValue());

    }
}
