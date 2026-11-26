package com.arvexa.capture;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

/** Token and parsed queued fields are encrypted; no SMS body is stored. Backup is disabled. */
final class Vault {
 private static final String ALIAS="arvexa_capture_v1";
 private final Context context;
 Vault(Context c){context=c.getApplicationContext();}
 private static synchronized javax.crypto.SecretKey key() throws Exception {
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(!ks.containsAlias(ALIAS)) {
   KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
   generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generator.generateKey();
  }
  return (javax.crypto.SecretKey)ks.getKey(ALIAS,null);
 }
 synchronized String encrypt(String value) throws Exception {
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
  return Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
 }
 synchronized String decrypt(String value) throws Exception {
  String[] parts=value.split(":",2);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
  c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
  return new String(c.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
 }
 SharedPreferences prefs(){return context.getSharedPreferences("capture",Context.MODE_PRIVATE);}
 void save(String credential) throws Exception {if(!prefs().edit().putString("credential",encrypt(credential)).commit())throw new java.io.IOException("Could not persist device credential");}
 String credential() throws Exception {String c=prefs().getString("credential",null);return c==null?null:decrypt(c);}
 boolean enabled(){return prefs().getBoolean("enabled",false);}
 void enabled(boolean value){prefs().edit().putBoolean("enabled",value).commit();}
 void status(String text){prefs().edit().putString("status",text).apply();}
 String status(){return prefs().getString("status","Waiting for a supported bank SMS");}
 void clear(){prefs().edit().clear().commit();}
}
