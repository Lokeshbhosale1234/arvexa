package com.arvexa.capture;
import android.content.*;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import com.fintech.capture.parser.BankSmsParser;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.concurrent.*;

public final class BankSmsReceiver extends BroadcastReceiver {
 private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor();
 @Override public void onReceive(Context context,Intent intent) {
  if(!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction()))return;
  Vault vault=new Vault(context);if(!vault.enabled())return;
  SmsMessage[] messages=Telephony.Sms.Intents.getMessagesFromIntent(intent);
  if(messages==null||messages.length==0)return;
  String sender=messages[0].getOriginatingAddress();StringBuilder body=new StringBuilder();
  for(SmsMessage m:messages){if(!java.util.Objects.equals(sender,m.getOriginatingAddress()))return;body.append(m.getMessageBody());}
  var parsed=BankSmsParser.parse(sender,body.toString());if(!parsed.isPresent())return;
  BankSmsParser.Alert a=parsed.get();
  // Fingerprint before dropping the raw message. Raw body is never persisted or uploaded.
  String canonical=sender+"|"+messages[0].getTimestampMillis()+"|"+body;
  long timestamp=messages[0].getTimestampMillis();
  PendingResult pending=goAsync();Context app=context.getApplicationContext();
  EXECUTOR.execute(()->{try {
   String credential=vault.credential();if(credential==null)return;
   JSONObject session=new JSONObject(credential);
   byte[] digest=MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
   StringBuilder id=new StringBuilder();for(byte b:digest)id.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
   JSONObject payload=new JSONObject().put("eventId",id.toString()).put("bank",a.bank).put("suffix",a.accountSuffix==null?JSONObject.NULL:a.accountSuffix)
    .put("amount",a.amount.toPlainString()).put("direction",a.direction).put("reference",a.reference==null?JSONObject.NULL:a.reference).put("observedAt",Instant.ofEpochMilli(timestamp).toString());
   try(Outbox outbox=new Outbox(app)){outbox.enqueue(id.toString(),session.getString("generation"),vault.encrypt(payload.toString()));}
   vault.status("Bank alert queued for sync");SyncWorker.schedule(app);
  }catch(Exception e){vault.status("Could not queue an alert. Open the app to check device storage and connection.");}finally{pending.finish();}});
 }
}
