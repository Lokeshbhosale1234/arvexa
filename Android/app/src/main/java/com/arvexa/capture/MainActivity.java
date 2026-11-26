package com.arvexa.capture;
import android.Manifest;
import android.app.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.work.WorkManager;
import org.json.JSONObject;
import java.util.UUID;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private TextView status;private Button connect;private Vault vault;
 private EditText base,email,password;private Switch enabled;
 @Override public void onCreate(Bundle state){super.onCreate(state);vault=new Vault(this);render();}
 private EditText input(LinearLayout root,String hint,int type){EditText e=new EditText(this);e.setHint(hint);e.setInputType(type);e.setSingleLine(true);root.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
 private void render(){
  ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(40,64,40,40);scroll.addView(root);setContentView(scroll);
  TextView title=new TextView(this);title.setText("Arvexa Capture");title.setTextSize(28);root.addView(title);
  TextView intro=new TextView(this);intro.setText("Automatic bank SMS tracking for your Arvexa dashboard. Add your bank and last four account digits on the website first. Supported templates only; this is not a verified bank feed.");intro.setPadding(0,20,0,24);root.addView(intro);
  base=input(root,"Backend HTTPS URL ending in /api",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);base.setText("https://zentriq-backend.onrender.com/api");
  email=input(root,"Arvexa email",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
  password=input(root,"Password",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  connect=new Button(this);connect.setText("Connect device");root.addView(connect);connect.setOnClickListener(v->disclose());
  enabled=new Switch(this);enabled.setText("Automatic capture enabled");enabled.setChecked(vault.enabled());root.addView(enabled);
  enabled.setOnCheckedChangeListener((button,value)->{
   if(value){try{if(vault.credential()==null){enabled.setChecked(false);status.setText("Connect your account first.");return;}}catch(Exception e){enabled.setChecked(false);status.setText("Reconnect this device.");return;}
    requestCapture();
   }else{vault.enabled(false);status.setText("Capture paused. No new messages will be processed.");}
  });
  Button sync=new Button(this);sync.setText("Sync now / refresh status");root.addView(sync);sync.setOnClickListener(v->{SyncWorker.schedule(this);refresh();});
  Button disconnect=new Button(this);disconnect.setText("Disconnect and delete local queue");root.addView(disconnect);disconnect.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Delete unsynced alerts and credentials from this phone? Revoke this device on the website too.").setPositiveButton("Disconnect",(d,x)->{
   vault.enabled(false);WorkManager.getInstance(this).cancelUniqueWork("arvexa-sync");WorkManager.getInstance(this).cancelUniqueWork("arvexa-periodic");
   try(Outbox outbox=new Outbox(this)){outbox.clear();}vault.clear();enabled.setChecked(false);refresh();
  }).setNegativeButton("Cancel",null).show());
  status=new TextView(this);status.setPadding(0,24,0,0);root.addView(status);refresh();
 }
 private void disclose(){
  new AlertDialog.Builder(this).setTitle("Allow bank SMS capture?")
   .setMessage("Arvexa processes new SMS on this device, including while this app is closed, to recognize supported bank credit/debit alerts. It uploads only extracted amounts, bank/account endings, references and SMS timestamps to the backend URL you enter. Personal messages and OTPs are ignored; raw SMS is not stored or uploaded. You can pause capture, disconnect, or revoke the device on the website. This does not read old messages. Only connect to a backend you trust.")
   .setPositiveButton("Agree and connect",(d,x)->connect()).setNegativeButton("Cancel",null).show();
 }
 private void connect(){
  final String url,mail=email.getText().toString().trim(),pass=password.getText().toString();
  try{url=Api.validateBase(base.getText().toString());}catch(Exception e){status.setText("Enter a valid HTTPS backend URL ending in /api.");return;}
  if(mail.isEmpty()||pass.isEmpty()){status.setText("Enter your email and password.");return;}
  connect.setEnabled(false);status.setText("Connecting…");
  io.execute(()->{try{
   Api.Reply login=Api.post(url,"/auth/login",new JSONObject().put("email",mail).put("password",pass),null,null);
   if(login.status!=200)throw new IllegalStateException("Login failed. Check credentials and server availability.");
   String jwt=login.body.getJSONObject("data").getString("token");
   Api.Reply enrolled=Api.post(url,"/capture/devices",new JSONObject().put("name",android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL),jwt,null);
   if(enrolled.status!=200)throw new IllegalStateException("Could not connect. Check the backend version or revoke an old device on the website.");
   JSONObject data=enrolled.body.getJSONObject("data");
   // Stop previous work and drop old-account queue before replacing the credential.
   vault.enabled(false);try(Outbox outbox=new Outbox(this)){outbox.clear();}
   vault.save(new JSONObject().put("base",url).put("token",data.getString("token")).put("generation",UUID.randomUUID().toString()).toString());
   runOnUiThread(()->{password.setText("");connect.setEnabled(true);requestCapture();});
  }catch(Exception e){runOnUiThread(()->{status.setText(e instanceof IllegalStateException?e.getMessage():"Connection failed. Check server availability and try again.");connect.setEnabled(true);});}});
 }
 private void requestCapture(){
  if(checkSelfPermission(Manifest.permission.RECEIVE_SMS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS},71);return;}
  vault.enabled(true);enabled.setChecked(true);SyncWorker.periodic(this);SyncWorker.schedule(this);refresh();
 }
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);
  if(code==71){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)requestCapture();else{vault.enabled(false);enabled.setChecked(false);status.setText("SMS permission was declined. Automatic capture is disabled.");}}
 }
 private void refresh(){try(Outbox outbox=new Outbox(this)){status.setText(vault.status()+"\nQueued alerts: "+outbox.count()+"\nCapture: "+(vault.enabled()?"on":"off")+"\nDelivery may be delayed by Android or network availability.");}}
 @Override protected void onResume(){super.onResume();if(status!=null){if(checkSelfPermission(Manifest.permission.RECEIVE_SMS)!=PackageManager.PERMISSION_GRANTED){vault.enabled(false);enabled.setChecked(false);}refresh();}}
 @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
}
