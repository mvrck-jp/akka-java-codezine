package example.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import java.util.*;

public class OrganizedEvent {
  /********************************************************************************
   *  アクターの状態遷移
   *******************************************************************************/
  public static Behavior<Message> create(String eventName){
    System.out.println("OrganizedEventアクターが作成されました");
    var initialState = new State(eventName, new HashMap<>());
    return Behaviors.setup(context -> handleMessage(context, initialState));
  }

  public static Behavior<Message> handleMessage(ActorContext<Message> context, State state) {
    return Behaviors.receive(Message.class)
      .onMessage(CreateTicketStock.class, message -> {
        var actorRef = context.spawn(TicketStock.create(message.ticketId), message.ticketId);
        state.children.put(message.ticketId, actorRef);
        return handleMessage(context, state);
      })
      .onMessage(ForwardToTicketStock.class, forwardMessage -> {
        var actorRef = state.children.get(forwardMessage.ticketId);
        if (actorRef == null) {
          System.out.println("No TicketStock actor for ticketId = " + forwardMessage.ticketId);
        } else {
          actorRef.tell(forwardMessage.message);
        }
        return handleMessage(context, state);
      })
      .build();
  }

  /********************************************************************************
   *  TicketPurchaserが受け取るメッセージの型
   *******************************************************************************/
  public interface Message {}

  public static class CreateTicketStock implements Message {
    final String ticketId;

    public CreateTicketStock(String ticketId) {
      this.ticketId = ticketId;
    }
  }

  public static class ForwardToTicketStock implements Message {
    final String ticketId;
    final TicketStock.Message message;

    public ForwardToTicketStock(String ticketId, TicketStock.Message message) {
      this.ticketId = ticketId;
      this.message = message;
    }
  }

  /********************************************************************************
   *  TicketPurchaserが取りうる状態の型
   *******************************************************************************/
  //childrenが変わるだけなので、他のアクターと違ってStateをそのまま唯一の状態を表す型として使う
  private static class State {
    String eventName;
    Map<String, ActorRef<TicketStock.Message>> children;

    public State(String eventName, Map<String, ActorRef<TicketStock.Message>> children) {
      this.eventName = eventName;
      this.children = children;
    }
  }
}
