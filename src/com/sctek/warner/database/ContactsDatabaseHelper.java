package com.sctek.warner.database;

import java.sql.DatabaseMetaData;

import com.sctek.warner.database.ContactsProvideData.ContactsTableData;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ContactsDatabaseHelper extends SQLiteOpenHelper{
	
	final String TAG = "ContactsDatabaseHelper";
	
	public ContactsDatabaseHelper(Context context) {
		// TODO Auto-generated constructor stub
		super(context,
				ContactsProvideData.DATABASE_NAME,
				null,
				ContactsProvideData.DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onCreate");
		
		db.execSQL("CREATE TABLE " + ContactsTableData.TABLE_NAME + " ("
				+ ContactsTableData.NAME + ","
				+ ContactsTableData.NUMBER + ","
				+ ContactsTableData.SEND_MG + " tinyint default 0,"
				+ ContactsTableData.DIAL + " tinyint default 0"
				+ ");");
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onUpgrade");
		
		db.execSQL("DROP TABLE IF EXISTS " +
				ContactsTableData.TABLE_NAME);
		onCreate(db);
	}
	
	

}
