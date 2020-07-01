package example.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import java.util.*;

public class OrganizedEventRoot {
  /********************************************************************************
   *  アクターの状態遷移
   *******************************************************************************/
  public static Behavior<Message> create(){
    System.out.println("OrganizedEventRootアクターが作成されました");
    return Behaviors.setup(context -> {
      //最初に3つのOrgnizedEventアクターを作る
      var eventNameA = "eventA";
      var eventNameB = "eventB";
      var eventNameC = "eventC";

      var actorRefA = context.spawn(OrganizedEvent.create(eventNameA), eventNameA);
      var actorRefB = context.spawn(OrganizedEvent.create(eventNameB), eventNameB);
      var actorRefC = context.spawn(OrganizedEvent.create(eventNameC), eventNameC);

      var children = new HashMap<String, ActorRef<OrganizedEvent.Message>>();
      children.put(eventNameA, actorRefA);
      children.put(eventNameB, actorRefB);
      children.put(eventNameC, actorRefC);

      return handleMessage(children);
    });
  }

  //唯一の状態はchildrenなので状態のための型を新たに設ける必要はない
  public static Behavior<Message> handleMessage(Map<String, ActorRef<OrganizedEvent.Message>> children) {
    return Behaviors.receive(Message.class)
      .onMessage(CreateTicketStock.class, message -> {
        var actorRef = children.get(message.eventName);
        actorRef.tell(new OrganizedEvent.CreateTicketStock(message.ticketId));
        return handleMessage(children);
      })
      .onMessage(ForwardToTicketStock.class, forwardMessage -> {
        var actorRef = children.get(forwardMessage.eventName);
        if (actorRef == null) {
          System.out.println("No OrganizedEvent actor for eventName = " + forwardMessage.eventName);
        } else {
          actorRef.tell(new OrganizedEvent.ForwardToTicketStock(forwardMessage.ticketId, forwardMessage.message));
        }
        return handleMessage(children);
      })
      .build();
  }

  /********************************************************************************
   *  TicketPurchaserが受け取るメッセージの型
   *******************************************************************************/
  public interface Message {}

  public static class CreateTicketStock implements Message {
    final String eventName;
    final String ticketId;

    public CreateTicketStock(String eventName, String ticketId) {
      this.eventName = eventName;
      this.ticketId = ticketId;
    }
  }

  public static class ForwardToTicketStock implements Message {
    final String eventName;
    final String ticketId;
    final TicketStock.Message message;

    public ForwardToTicketStock(String eventName, String ticketId, TicketStock.Message message) {
      this.eventName = eventName;
      this.ticketId = ticketId;
      this.message = message;
    }
  }
}
