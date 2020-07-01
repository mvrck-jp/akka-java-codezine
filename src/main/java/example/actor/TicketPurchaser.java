package example.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

public class TicketPurchaser {
  /********************************************************************************
   *  アクターの状態遷移
   *******************************************************************************/
  //Behaviorを返すメソッドの中で、唯一アクター外部からアクセスされる
  public static Behavior<Message> create(String userId, ActorRef<OrganizedEventRoot.Message> eventRootRef){
    System.out.println("TicketPurchaserアクターが作成されました");
    return Behaviors.setup(context -> inactive(context, eventRootRef, new Inactive(userId)));
  }

  private static Behavior<Message> inactive(
    ActorContext<Message> context,
    ActorRef<OrganizedEventRoot.Message> eventRootRef,
    Inactive inactiveState
  ) {
    return Behaviors.receive(Message.class)
      .onMessage(Activate.class, message ->
         active(
           context,
           eventRootRef,
           new Active(message.userId)
         )
      )
      .build();
  }

  private static Behavior<Message> active(
    ActorContext<Message> context,
    ActorRef<OrganizedEventRoot.Message> eventRootRef,
    Active activeState
  ) {
    return Behaviors.receive(Message.class)
      .onMessage(PurchaseTicket.class, message -> {
        eventRootRef.tell(
          new OrganizedEventRoot.ForwardToTicketStock(
            message.eventName,
            message.ticketId,
            new TicketStock.PurchaseTicket(
              message.userId,
              message.ticketId,
              message.purchaseQuantity,
              message.sender
            )
          )
        );
        return Behaviors.same();
      })
      .build();
  }

  /********************************************************************************
   *  TicketPurchaserが受け取るメッセージの型
   *******************************************************************************/
  public interface Message {}

  public static final class Activate implements Message {
    public final String userId;

    public Activate(String userId) {
      this.userId = userId;
    }
  }

  public static final class PurchaseTicket implements Message {
    public final String userId;
    public final String eventName;
    public final String ticketId;
    public final int purchaseQuantity;
    public final ActorRef<TicketStock.PurchaseResponse> sender;


    public PurchaseTicket(String userId, String eventName, String ticketId, int purchaseQuantity, ActorRef<TicketStock.PurchaseResponse> sender) {
      this.userId = userId;
      this.eventName = eventName;
      this.ticketId = ticketId;
      this.purchaseQuantity = purchaseQuantity;
      this.sender = sender;
    }
  }

  /********************************************************************************
   *  TicketPurchaserが取りうる状態の型
   *******************************************************************************/
  private interface State {}

  // 状態はアクター外部に晒さないのでprivate
  private static class Inactive implements State {
    final String userId;

    public Inactive(String userId) {
      this.userId = userId;
    }
  }

  private static class Active implements State {
    final String userId;

    public Active(String userId) {
      this.userId = userId;
    }

  }}
