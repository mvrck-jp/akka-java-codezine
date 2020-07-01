package example.main;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import example.actor.*;

import java.time.*;

public class Main {

  public static void requestPurchaseTicket(
    String logPrefix,
    ActorSystem<Guardian.Message> system,
    ActorRef<TicketPurchaserRoot.Message> ticketPurchaserRootRef,
    String userId,
    String eventName,
    String ticketId,
    int purchaseQuantity) {

    var result = AskPattern.ask(
      ticketPurchaserRootRef,
      (ActorRef<TicketStock.PurchaseResponse> sender) ->
        new TicketPurchaserRoot.ForwardToTicketPurchaser(userId, new TicketPurchaser.PurchaseTicket(userId, eventName, ticketId, purchaseQuantity, sender)),
      Duration.ofSeconds(3),
      system.scheduler()
    );
    result.thenAccept(res -> System.out.println(logPrefix + " - TicketStockからレスポンスが返ってきました!: " + res));
  }

  public static void main(String[] args) throws Exception {
    var system = ActorSystem.create(Guardian.create(), "actorSystem");

    var response1 = AskPattern.ask(system, (ActorRef<ActorRef<OrganizedEventRoot.Message>> sender) -> new Guardian.GetOrganizedEventRootRef(sender), Duration.ofSeconds(3), system.scheduler());
    var response2 = AskPattern.ask(system, (ActorRef<ActorRef<TicketPurchaserRoot.Message>> sender) -> new Guardian.GetTicketPurchaserRootRef(sender), Duration.ofSeconds(3), system.scheduler());

    response1.thenAcceptBoth(response2,
      (organizedEventRootRef, ticketPurchaserRootRef) -> {
        //TicketStockを作成
        organizedEventRootRef.tell(new OrganizedEventRoot.CreateTicketStock("eventA", "ticketA1"));
        //TicketStockを販売開始状態にする
        organizedEventRootRef.tell(new OrganizedEventRoot.ForwardToTicketStock("eventA", "ticketA1", new TicketStock.StartSale("ticketA1", 2)));
        //TicketPurchaserを活性状態にする
        ticketPurchaserRootRef.tell(new TicketPurchaserRoot.ForwardToTicketPurchaser("userA", new TicketPurchaser.Activate("userA")));

        //チケットを一枚ずつ買う
        requestPurchaseTicket("result1", system, ticketPurchaserRootRef, "userA", "eventA", "ticketA1", 1);
        requestPurchaseTicket("result2", system, ticketPurchaserRootRef, "userA", "eventA", "ticketA1", 1);
        requestPurchaseTicket("result3", system, ticketPurchaserRootRef, "userA", "eventA", "ticketA1", 1);

        //TicketStockを作成
        organizedEventRootRef.tell(new OrganizedEventRoot.CreateTicketStock("eventB", "ticketB1"));
        //TicketStockを販売開始状態にする
        organizedEventRootRef.tell(new OrganizedEventRoot.ForwardToTicketStock("eventB", "ticketB1", new TicketStock.StartSale("ticketB1", 2)));
        //TicketStockを販売終了状態にする
        organizedEventRootRef.tell(new OrganizedEventRoot.ForwardToTicketStock("eventB", "ticketB1", new TicketStock.EndSale("ticketB1")));
        //チケットを一枚ずつ買う
        requestPurchaseTicket("result4", system, ticketPurchaserRootRef, "userA", "eventB", "ticketB1", 1);
      }
    );

    System.in.read();
    system.terminate();
  }
}