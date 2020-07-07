MavenとJavaのインストールが済んでいることが前提です。以下の一連のコマンドでアプリケーションが走ります:

```
git clone git@github.com:mvrck-inc/akka-java-codezine.git
cd akka-java-codezine
mvn exec:java -Dexec.mainClass=example.main.Main
```

出力例は以下:

```
[INFO] Scanning for projects...
[INFO]
[INFO] ---------------< org.mvrck.training:akka-java-codezine >----------------
[INFO] Building app 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.6.0:java (default-cli) @ akka-java-codezine ---
Guardianアクターが作成されました
SLF4J: A number (1) of logging calls during the initialization phase have been intercepted and are
SLF4J: now being replayed. These are subject to the filtering rules of the underlying logging system.
SLF4J: See also http://www.slf4j.org/codes.html#replay
OrganizedEventRootアクターが作成されました
TicketPurchaserRootアクターが作成されました
OrganizedEventアクターが作成されました
TicketPurchaserアクターが作成されました
OrganizedEventアクターが作成されました
TicketPurchaserアクターが作成されました
OrganizedEventアクターが作成されました
TicketPurchaserアクターが作成されました
TicketStockアクターが作成されました
TicketStockアクターが作成されました
result2 - TicketStockからレスポンスが返ってきました!: PurchaseResponseSuccess(purchaseId = 2, ticketId = ticketA1, purchaseQuantity = 1)
result4 - TicketStockからレスポンスが返ってきました!: PurchaseResponseFailure(ticketId = ticketB1, purchaseQuantity = 1, errorMessage = チケット販売期間は終了しています)
result3 - TicketStockからレスポンスが返ってきました!: PurchaseResponseFailure(ticketId = ticketA1, purchaseQuantity = 1, errorMessage = チケットは売り切れです)
result1 - TicketStockからレスポンスが返ってきました!: PurchaseResponseSuccess(purchaseId = 1, ticketId = ticketA1, purchaseQuantity = 1)
```

理解を深めるため、Mainクラスを書き換えて別のチケット購入シナリオを試すと良いでしょう。
