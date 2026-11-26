package com.arvexa.capture;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public final class SyncWorker extends Worker {
 public SyncWorker(@NonNull Context c,@NonNull WorkerParameters p){super(c,p);}
 static Constraints constraints(){return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();}
 static void schedule(Context c){WorkManager.getInstance(c).enqueueUniqueWork("arvexa-sync",ExistingWorkPolicy.APPEND_OR_REPLACE,new OneTimeWorkRequest.Builder(SyncWorker.class).setConstraints(constraints()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build());}
 static void periodic(Context c){WorkManager.getInstance(c).enqueueUniquePeriodicWork("arvexa-periodic",ExistingPeriodicWorkPolicy.KEEP,new PeriodicWorkRequest.Builder(SyncWorker.class,15,TimeUnit.MINUTES).setConstraints(constraints()).build());}
 @NonNull @Override public Result doWork(){
  Vault vault=new Vault(getApplicationContext());if(!vault.enabled())return Result.success();
  try {
   String stored=vault.credential();if(stored==null)return Result.success();
   JSONObject session=new JSONObject(stored);String base=Api.validateBase(session.getString("base")), token=session.getString("token"), generation=session.getString("generation");
   try(Outbox outbox=new Outbox(getApplicationContext())){
    for(String[] row:outbox.batch(generation)){
     if(!vault.enabled())return Result.success();
     String current=vault.credential();if(current==null||!generation.equals(new JSONObject(current).getString("generation")))return Result.success();
     Api.Reply r=Api.post(base,"/capture/device/ingest",new JSONObject(vault.decrypt(row[1])),null,token);
     if(r.status==401||r.status==403){vault.status("Connection expired or revoked. Reconnect this device.");vault.enabled(false);return Result.success();}
     if(r.status==429||r.status>=500)return Result.retry();
     if(r.status<200||r.status>=300){outbox.reject(row[0],generation);vault.status("An alert was rejected and retained locally for attention. Other alerts can still sync.");continue;}
     outbox.remove(row[0],generation);vault.status("Last synced "+java.time.Instant.now()+". Check the website for imported entries or review items.");
    }
    if(outbox.count(generation)>0){schedule(getApplicationContext());return Result.success();}
   }
   Api.Reply heartbeat=Api.post(base,"/capture/device/heartbeat",new JSONObject(),null,token);
   if(heartbeat.status==401||heartbeat.status==403){vault.enabled(false);vault.status("Reconnect this device.");}
   else if(heartbeat.status==429||heartbeat.status>=500)return Result.retry();
   return Result.success();
  }catch(Exception e){vault.status("Sync paused; will retry when the server is reachable.");return Result.retry();}
 }
}
