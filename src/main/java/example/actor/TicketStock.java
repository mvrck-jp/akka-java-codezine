package example.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import java.util.*;

public class TicketStock {
  /********************************************************************************
   *  アクターの状態遷移
   *******************************************************************************/
  //Behaviorを返すメソッドの中で、唯一アクター外部からアクセスされる
  public static Behavior<Message> create(String ticketId){
    System.out.println("TicketStockアクターが作成されました");
    return Behaviors.setup(
      context -> beforeSale(context, new BeforeSale(ticketId))
    );
  }

  private static Behavior<Message> beforeSale(
    ActorContext<Message> context,
    BeforeSale beforeSaleState
  ) {
    return Behaviors.receive(Message.class)
      .onMessage(StartSale.class, message ->
        stockAvailable(
          context,
          new StockAvailable(
            beforeSaleState.ticketId,
            message.stockQuantity,
            1
          )
        )
      )
      .build();
  }

  private static Behavior<Message> stockAvailable(
    ActorContext<Message> context,
    StockAvailable stockAvailableState
  ) {
    return Behaviors.receive(Message.class)
      .onMessage(PurchaseTicket.class, message -> {
        if (!stockAvailableState.canPurchaseTicket(message.purchaseQuantity)) {
          message.sender.tell(
            new PurchaseResponseFailure(
              message.ticketId,
              message.purchaseQuantity,
              "在庫数を超えるチケットは購入できません"
            )
          );
          return Behaviors.same();
        } else {
          message.sender.tell(
            new PurchaseResponseSuccess(
              stockAvailableState.nextPurchaseId,
              message.ticketId,
              message.purchaseQuantity
            )
          );

          var nextState = stockAvailableState.purchaseTicket(
            message.userId,
            message.purchaseQuantity);

          if (nextState instanceof OutOfStock) {
            return outOfStock(
              context,
              (OutOfStock) nextState
            );
          } else { //OutOfStockではければ必ずStockAvailable
            return stockAvailable(
              context,
              (StockAvailable) nextState
            );
          }
        }
      })
      .onMessage(EndSale.class, message ->
        saleEnded(
          context,
          new SaleEnded(
            stockAvailableState.ticketId,
            stockAvailableState.purchaseRecords
          )
        )
      )
      .build();
  }

  public static Behavior<Message> outOfStock(
    ActorContext<Message> context,
    OutOfStock outOfStockState
  ) {
    return Behaviors.receive(Message.class)
      .onMessage(EndSale.class, message ->
        saleEnded(
          context,
          new SaleEnded(
            outOfStockState.ticketId,
            outOfStockState.purchaseRecords
          )
        )
      )
      .onMessage(PurchaseTicket.class, message -> {
        message.sender.tell(new PurchaseResponseFailure(
          message.ticketId,
          message.purchaseQuantity,
          "チケットは売り切れです")
        );
        return Behaviors.same();
      })
      .build();
  }

  public static Behavior<Message> saleEnded(
    ActorContext<Message> context,
    SaleEnded saleEndedState
  ) {
    return Behaviors.receive(Message.class)
      .onMessage(PurchaseTicket.class, message -> {
        message.sender.tell(new PurchaseResponseFailure(
          message.ticketId,
          message.purchaseQuantity,
          "チケット販売期間は終了しています")
        );
        return Behaviors.same();
      })
      .build();
  }

  /********************************************************************************
   *  TicketStockが受け取るメッセージの型
   *******************************************************************************/
  public interface Message {}

  public static final class StartSale implements Message {
    public final String ticketId;
    public final int stockQuantity;

    public StartSale(String ticketId, int stockQuantity) {
      this.ticketId = ticketId;
      this.stockQuantity = stockQuantity;
    }
  }

  public static final class EndSale implements Message {
    public final String ticketId;

    public EndSale(String ticketId) {
      this.ticketId = ticketId;
    }
  }

  public static final class PurchaseTicket implements Message {
    public final String userId;
    public final String ticketId;
    public final int purchaseQuantity;
    public final ActorRef<PurchaseResponse> sender;

    public PurchaseTicket(String userId, String ticketId, int purchaseQuantity, ActorRef<PurchaseResponse> sender) {
      this.userId = userId;
      this.ticketId = ticketId;
      this.purchaseQuantity = purchaseQuantity;
      this.sender = sender;
    }
  }

  /********************************************************************************
   *  TicketStockがAskパターンで送り返すメッセージの型
   *******************************************************************************/
  public interface PurchaseResponse {}

  public static class PurchaseResponseFailure implements PurchaseResponse {
    public final String ticketId;
    public final int purchaseQuantity;
    public final String errorMessage;

    public PurchaseResponseFailure(String ticketId, int purchaseQuantity, String errorMessage) {
      this.ticketId = ticketId;
      this.purchaseQuantity = purchaseQuantity;
      this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
      return "PurchaseResponseFailure(ticketId = " + ticketId + ", purchaseQuantity = " + purchaseQuantity + ", errorMessage = " + errorMessage + ")";
    }
  }

  public static class PurchaseResponseSuccess implements PurchaseResponse {
    public final int purchaseId;
    public final String ticketId;
    public final int purchaseQuantity;

    public PurchaseResponseSuccess(int purchaseId, String ticketId, int purchaseQuantity) {
      this.purchaseId = purchaseId;
      this.ticketId = ticketId;
      this.purchaseQuantity = purchaseQuantity;
    }

    @Override
    public String toString() {
      return "PurchaseResponseSuccess(purchaseId = " + purchaseId + ", ticketId = " + ticketId + ", purchaseQuantity = " + purchaseQuantity + ")";
    }
  }


  /********************************************************************************
   *  TicketStockが取りうる状態の型
   *******************************************************************************/
  private interface State {}

  // 状態はアクター外部に晒さないのでprivate
  private static class BeforeSale implements State {
    final String ticketId;

    public BeforeSale(String ticketId) {
      this.ticketId = ticketId;
    }
  }

  private static class StockAvailable implements State {
    final String ticketId;
    final int stockQuantity;
    final int nextPurchaseId;
    final Map<Integer, PurchaseRecord> purchaseRecords;

    public StockAvailable(String ticketId, int stockQuantity, int nextPurchaseId) {
      this.ticketId = ticketId;
      this.stockQuantity = stockQuantity;
      this.nextPurchaseId = nextPurchaseId;
      this.purchaseRecords = Map.of();
    }

    private StockAvailable(String ticketId, int stockQuantity, int nextPurchaseId, Map<Integer, PurchaseRecord> purchaseRecords) {
      this.ticketId = ticketId;
      this.stockQuantity = stockQuantity;
      this.nextPurchaseId = nextPurchaseId;
      this.purchaseRecords = purchaseRecords;
    }

    public boolean canPurchaseTicket(int purchaseQuantity) {
      return purchaseQuantity > 0 && this.stockQuantity >= purchaseQuantity;
    }

    public State purchaseTicket(String userId, int purchaseQuantity) {
      //実際にはpurchaseRecordsを更新する処理を書かなくてはいけない
      final Map<Integer, PurchaseRecord> updatedPurchaseRecords = Map.of();

      if (this.stockQuantity == purchaseQuantity ) {
        return new OutOfStock(
          this.ticketId,
          this.nextPurchaseId + 1,
          updatedPurchaseRecords);
      } else {
        return new StockAvailable(
          this.ticketId,
          this.stockQuantity - purchaseQuantity,
          this.nextPurchaseId + 1,
          updatedPurchaseRecords);
      }
    }
  }

  private static class OutOfStock implements State {
    final String ticketId;
    final int nextPurchaseId;
    final Map<Integer, PurchaseRecord> purchaseRecords;

    public OutOfStock(String ticketId, int nextPurchaseId, Map<Integer, PurchaseRecord> purchaseRecords) {
      this.ticketId = ticketId;
      this.nextPurchaseId = nextPurchaseId;
      this.purchaseRecords = purchaseRecords;
    }
  }

  private static class SaleEnded implements State {
    final String ticketId;
    final Map<Integer, PurchaseRecord> purchaseRecords;

    public SaleEnded(String ticketId, Map<Integer, PurchaseRecord> purchaseRecords) {
      this.ticketId = ticketId;
      this.purchaseRecords = purchaseRecords;
    }
  }

  /********************************************************************************
   *  その他
   *******************************************************************************/
  private static class PurchaseRecord {
    final int purchaseRecordId;
    final String ticketId;
    final String userId;
    final int purchaseQuantity;

    public PurchaseRecord(int purchaseRecordId, String ticketId, String userId, int purchaseQuantity) {
      this.purchaseRecordId = purchaseRecordId;
      this.ticketId = ticketId;
      this.userId = userId;
      this.purchaseQuantity = purchaseQuantity;
    }
  }
}
