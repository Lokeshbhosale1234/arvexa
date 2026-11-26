package com.arvexa.capture;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
final class Outbox extends SQLiteOpenHelper {
 Outbox(Context c){super(c,"capture_outbox.db",null,1);}
 @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE alerts (id TEXT PRIMARY KEY, generation TEXT NOT NULL, payload TEXT NOT NULL)");}
 @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("Unsupported outbox schema");}
 void enqueue(String id,String generation,String encryptedPayload){ContentValues v=new ContentValues();v.put("id",id);v.put("generation",generation);v.put("payload",encryptedPayload);getWritableDatabase().insertWithOnConflict("alerts",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
 List<String[]> batch(String generation){List<String[]> rows=new ArrayList<>();try(Cursor c=getReadableDatabase().query("alerts",new String[]{"id","payload"},"generation=?",new String[]{generation},null,null,"rowid ASC","50")){while(c.moveToNext())rows.add(new String[]{c.getString(0),c.getString(1)});}return rows;}
 void remove(String id,String generation){getWritableDatabase().delete("alerts","id=? AND generation=?",new String[]{id,generation});}
 void clear(){getWritableDatabase().delete("alerts",null,null);}
 int count(String generation){try(Cursor c=getReadableDatabase().rawQuery("SELECT count(*) FROM alerts WHERE generation=?",new String[]{generation})){c.moveToFirst();return c.getInt(0);}}
 int count(){try(Cursor c=getReadableDatabase().rawQuery("SELECT count(*) FROM alerts",null)){c.moveToFirst();return c.getInt(0);}}
}
