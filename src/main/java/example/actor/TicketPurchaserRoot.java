package example.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import java.util.*;

public class TicketPurchaserRoot {
  /********************************************************************************
   *  アクターの状態遷移
   *******************************************************************************/
  public static Behavior<Message> create(ActorRef<OrganizedEventRoot.Message> eventRootRef){
    System.out.println("TicketPurchaserRootアクターが作成されました");
    return Behaviors.setup(context -> {
      //最初に3つのOrgnizedEventアクターを作る
      var userNameA = "userA";
      var userNameB = "userB";
      var userNameC = "userC";

      var actorRefA = context.spawn(TicketPurchaser.create(userNameA, eventRootRef), userNameA);
      var actorRefB = context.spawn(TicketPurchaser.create(userNameB, eventRootRef), userNameB);
      var actorRefC = context.spawn(TicketPurchaser.create(userNameC, eventRootRef), userNameC);

      var children = new HashMap<String, ActorRef<TicketPurchaser.Message>>();
      children.put(userNameA, actorRefA);
      children.put(userNameB, actorRefB);
      children.put(userNameC, actorRefC);

      return handleMessage(children);
    });
  }

  //唯一の状態はchildrenなので状態のための型を新たに設ける必要はない
  public static Behavior<Message> handleMessage(Map<String, ActorRef<TicketPurchaser.Message>> children) {
    return Behaviors.receive(Message.class)
      .onMessage(ForwardToTicketPurchaser.class, forwardMessage -> {
        var actorRef = children.get(forwardMessage.userName);
        if (actorRef == null) {
          System.out.println("No TicketPurchaser actor for userName = " + forwardMessage.userName);
        } else {
          actorRef.tell(forwardMessage.message);
        }
        return handleMessage(children);
      })
      .build();
  }

  /********************************************************************************
   *  TicketPurchaserが受け取るメッセージの型
   *******************************************************************************/
  public interface Message {}

  public static class ForwardToTicketPurchaser implements Message {
    final String userName;
    final TicketPurchaser.Message message;

    public ForwardToTicketPurchaser(String userName, TicketPurchaser.Message message) {
      this.userName = userName;
      this.message = message;
    }
  }
}
