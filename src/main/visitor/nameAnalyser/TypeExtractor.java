package main.visitor.nameAnalyser;

import main.ast.node.Main;
import main.ast.node.Program;
import main.ast.node.declaration.ActorDeclaration;
import main.ast.node.declaration.ActorInstantiation;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.declaration.handler.HandlerDeclaration;
import main.ast.node.declaration.handler.MsgHandlerDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.operators.UnaryOperator;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.node.statement.*;
import main.ast.type.Type;
import main.ast.type.noType.NoType;
import main.ast.type.primitiveType.BooleanType;
import main.ast.type.primitiveType.IntType;
import main.symbolTable.*;
import main.symbolTable.itemException.ItemNotFoundException;
import main.symbolTable.symbolTableVariableItem.SymbolTableLocalVariableItem;
import main.symbolTable.symbolTableVariableItem.SymbolTableVariableItem;
import main.visitor.Visitor;
import main.visitor.VisitorImpl;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class TypeExtractor extends VisitorImpl {
    SymbolTableActorItem curr_act;
    SymbolTableHandlerItem curr_handler;

  public Type get_type_of_identifier(String name){
      Type ret;
      try {
          System.out.println(curr_handler.getName());
          SymbolTable hst = curr_handler.getHandlerSymbolTable();
          SymbolTableItem test2 = (hst.get(SymbolTableLocalVariableItem.STARTKEY + name));
          if ( test2 instanceof  SymbolTableVariableItem){
              System.out.println("chetori ali?");
          }
          SymbolTableVariableItem test = (SymbolTableVariableItem)(hst.get(SymbolTableLocalVariableItem.STARTKEY + name));
          ret = test.getType();
      } catch (ItemNotFoundException ignored) {
          try {
              ret = ((SymbolTableLocalVariableItem)curr_act.getActorSymbolTable().get(SymbolTableLocalVariableItem.STARTKEY + name)).getType();
          } catch (ItemNotFoundException ignored2) {
              String error = "variable " + name + " is not declared";
              System.out.println(error);
              return new NoType();
          }
      }
      return ret;
  }


//    public Type get_type_in_msg_handler(Identifier identifier){
//        for (MsgHandlerDeclaration msg_handler: curr_act.getMsgHandlers()){
//
//            if (var.getIdentifier().getName().equals(identifier.getName())){
//                return var.getType();
//            }
//        }
//
//        //else in babas
//        return new NoType();
//    }
//
//
//    public Type get_type_in_known_actor(Identifier identifier){
//        for (VarDeclaration var: curr_act.getActorVars()){
//            if (var.getIdentifier().getName().equals(identifier.getName())){
//                return var.getType();
//            }
//        }
//
//        //else in babas
//        return new NoType();
//    }

//    public void  set_type_in_actor_vars(Identifier identifier , Type type ){
//        for (VarDeclaration var: curr_act.getActorVars()){
//            if (var.getIdentifier().getName().equals(identifier.getName())){
//                var.setType(type);
//                var.getIdentifier().setType(type);
//            }
//        }
//
//        //else in babas
//
//    }


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
            curr_act = (SymbolTableActorItem) SymbolTable.root.get(SymbolTableActorItem.STARTKEY + actorDeclaration.getName().getName());
        } catch (ItemNotFoundException ignored) {
        }
        visitExpr(actorDeclaration.getName());
        visitExpr(actorDeclaration.getParentName());

        for(VarDeclaration varDeclaration: actorDeclaration.getKnownActors())
            varDeclaration.accept(this);

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
            curr_handler = (SymbolTableHandlerItem) curr_act.getActorSymbolTable().get(handlerKey);
        } catch (ItemNotFoundException ignored) {
        }
        visitExpr(handlerDeclaration.getName());
        for(VarDeclaration argDeclaration: handlerDeclaration.getArgs())
            argDeclaration.accept(this);
        for(VarDeclaration localVariable: handlerDeclaration.getLocalVars())
            localVariable.accept(this);
        for(Statement statement : handlerDeclaration.getBody()) {
            visitStatement(statement);
        }

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
        SymbolTableMainItem mainItem = (SymbolTableMainItem) SymbolTable.root.getSymbolTableItems().get("Main_main");
//        SymbolTable.root.getSymbolTableItems().get("Actor_");
        System.out.println("Ali");
        System.out.println(mainItem.getKey().toString());
        System.out.println("Mammad");

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
            System.out.println(unaryExpression.getOperand().getType().toString());
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
        //TODO: implement appropriate visit functionality
        if(binaryExpression == null)
            return;

        visitExpr(binaryExpression.getLeft());
        visitExpr(binaryExpression.getRight());


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
        //TODO: implement appropriate visit functionality
        if(identifier == null)
            return;
    }

    @Override
    public void visit(Self self) {
        //TODO: implement appropriate visit functionality
    }

    @Override
    public void visit(Sender sender) {
        //TODO: implement appropriate visit functionality
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
        //TODO: implement appropriate visit functionality
        visitExpr(conditional.getExpression());
        visitStatement(conditional.getThenBody());
        visitStatement(conditional.getElseBody());
    }

    @Override
    public void visit(For loop) {
        //TODO: implement appropriate visit functionality
        visitStatement(loop.getInitialize());
        Type val_type = get_type_of_identifier(((Identifier) loop.getInitialize().getlValue()).getName());
        if (!(val_type instanceof IntType) && !(val_type instanceof NoType)){
            String error = "unsupported operand type for assignment";
            System.out.println(error);
        }
        visitExpr(loop.getCondition());
        visitStatement(loop.getUpdate());
        visitStatement(loop.getBody());
    }

    @Override
    public void visit(Break breakLoop) {
        //TODO: implement appropriate visit functionality
    }

    @Override
    public void visit(Continue continueLoop) {
        //TODO: implement appropriate visit functionality
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
        //TODO: implement appropriate visit functionality
        if(print == null)
            return;
        visitExpr(print.getArg());
    }


    @Override
    public void visit(Assign assign) {
        //TODO: implement appropriate visit functionality
        visitExpr(assign.getlValue());
        visitExpr(assign.getrValue());

//        System.out.println(assign.getlValue().getType().toString());

    }
}
