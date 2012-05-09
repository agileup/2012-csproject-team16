package kaistcs.android.dontkoala;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class EmergencyContactsDB {
	private static final String TAG = "EmergencyContactsDB";
	
	public static interface Columns {
		public static final String ID = "_id";
		public static final String NAME = "name";
		public static final String NUMBER = "number";
	}
	
	private static final String DATABASE_NAME = "profile.db";
	private static final String DATABASE_TABLE = "ecn";
	private static final int DATABASE_VERSION = 2;
	
	private static class ECNOpenHelper extends SQLiteOpenHelper {
		ECNOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + DATABASE_TABLE + "(" +
					Columns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					Columns.NAME + " TEXT, " +
					Columns.NUMBER + " TEXT);"
					);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + "to " + newVersion + ", which will destory all old data");
			db.execSQL("DROP TABLE IF EXISTS" + DATABASE_TABLE);
			onCreate(db);
		}
	}
	
	private ECNOpenHelper mDBHelper;
	private SQLiteDatabase mDB;
	private Context mContext;
	
	public EmergencyContactsDB(Context context) {
		mContext = context;
	}
	
	public EmergencyContactsDB open() throws SQLException {
		mDBHelper = new ECNOpenHelper(mContext);
		mDB = mDBHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		mDBHelper.close();
	}
	
	public long addECN(String name, String number) {
		ContentValues val = new ContentValues();
		val.put(Columns.NAME, name);
		val.put(Columns.NUMBER, number);
		
		return mDB.insert(DATABASE_TABLE, null, val);
	}
	
	public boolean deleteECN(long rowId) {
		return mDB.delete(DATABASE_TABLE, Columns.ID + "=" + rowId, null) > 0;
	}
	
	public Cursor queryAll() {
		return mDB.query(DATABASE_TABLE, null, null, null, null, null, null);
	}
}
