package example.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

public class Guardian {
  /********************************************************************************
   *  アクターの状態遷移
   *******************************************************************************/
  //1つの状態しかないのでcreate()内にメッセージハンドラーを全てベタ書き
  public static Behavior<Message> create(){
    System.out.println("Guardianアクターが作成されました");
    return Behaviors.setup(
      context -> {
        var eventRootRef = context.spawn(OrganizedEventRoot.create(), "organizedEventRoot");
        var purchaserRootRef = context.spawn(TicketPurchaserRoot.create(eventRootRef), "ticketPurchaserRoot");

        //Askパターンへの対応
        return Behaviors.receive(Message.class)
          .onMessage(GetOrganizedEventRootRef.class, message -> {
            message.sender.tell(eventRootRef);
            return Behaviors.same();
          })
          .onMessage(GetTicketPurchaserRootRef.class, message -> {
            message.sender.tell(purchaserRootRef);
            return Behaviors.same();
          })
          .build();
      }
    );
  }

  /********************************************************************************
   *  アクターが受け取るメッセージの型
   *******************************************************************************/
  public interface Message {}

  //AskパターンでOrganizedEventRootのActorRefを取得するためのメッセージ
  public static class GetOrganizedEventRootRef implements Message {
    //Askパターンなのでsenderの型は、senderがレスポンスとして欲しい型をTとしてActorRef<T>、TがさらにActorRefになっている
    final ActorRef<ActorRef<OrganizedEventRoot.Message> > sender;

    public GetOrganizedEventRootRef(ActorRef<ActorRef<OrganizedEventRoot.Message> > sender) {
      this.sender = sender;
    }
  }

  //AskパターンでTicketPurchaserRootのActorRefを取得するためのメッセージ
  public static class GetTicketPurchaserRootRef implements Message {
    //Askパターンなのでsenderの型は、senderがレスポンスとして欲しい型をTとしてActorRef<T>、TがさらにActorRefになっている
    final ActorRef<ActorRef<TicketPurchaserRoot.Message>> sender;

    public GetTicketPurchaserRootRef(ActorRef<ActorRef<TicketPurchaserRoot.Message>> sender) {
      this.sender = sender;
    }
  }
}
