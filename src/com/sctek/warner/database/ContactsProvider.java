package com.sctek.warner.database;

import com.sctek.warner.database.ContactsProvideData.ContactsTableData;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class ContactsProvider extends ContentProvider {
	
	final String TAG = "ContactsProvider";
	
	private static final UriMatcher mUriMatcher;
	private static final int THE_WHOLE_TABLE_URI = 1;
	private static final int SINGLE_CONTACTS_URI = 2;
	
	private ContactsDatabaseHelper mDBHelper;
	
	static
	{
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(ContactsProvideData.AUTHORITY, "contacts", 
							THE_WHOLE_TABLE_URI);
		mUriMatcher.addURI(ContactsProvideData.AUTHORITY, "contacts/#", 
							SINGLE_CONTACTS_URI);
		
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onCreate");
		
		mDBHelper = new ContactsDatabaseHelper(getContext());
		
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		Log.e(TAG, "query");
		
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		SQLiteDatabase database = mDBHelper.getReadableDatabase();
		
		switch (mUriMatcher.match(uri)) {
		
				case THE_WHOLE_TABLE_URI:
					queryBuilder.setTables(ContactsTableData.TABLE_NAME);
					break;
					
				case SINGLE_CONTACTS_URI:
					queryBuilder.setTables(ContactsTableData.TABLE_NAME);
					queryBuilder.appendWhere(ContactsTableData._ID + "="
							+ uri.getPathSegments().get(1));
					break;
					
				default:
					throw new IllegalArgumentException("Unknow URI " + uri);
			
		}
		
		Cursor cursor = queryBuilder.query(database, projection, selection, 
				selectionArgs, null, null, sortOrder);

		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		Log.e(TAG, "getType");
		
		switch (mUriMatcher.match(uri)) {
		
			case THE_WHOLE_TABLE_URI:
				return ContactsTableData.CONTENT_TYPE;
				
			case SINGLE_CONTACTS_URI:
				return ContactsTableData.CONTENT_ITEM_TYPE;
				
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
				
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		Log.e(TAG, "insert");
		
		if(mUriMatcher.match(uri) != THE_WHOLE_TABLE_URI) {
			throw new IllegalArgumentException("Unknow URI " + uri);
		}
		
		SQLiteDatabase database = mDBHelper.getWritableDatabase();
		long rowId = 0;
		if(mUriMatcher.match(uri) == THE_WHOLE_TABLE_URI)
			rowId = database.insert(ContactsTableData.TABLE_NAME, 
					ContactsTableData._ID, values);
		
		if(rowId > 0) {
			
			Uri insertUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(insertUri, null);
			
			return insertUri;
		}
		
		throw new SQLException("Failded to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		Log.e(TAG, "delete");
		
		SQLiteDatabase database = mDBHelper.getWritableDatabase();
		int count;
		
		switch (mUriMatcher.match(uri)){
		
				case THE_WHOLE_TABLE_URI:
					count = database.delete(ContactsTableData.TABLE_NAME, 
							selection, selectionArgs);
					break;
					
				case SINGLE_CONTACTS_URI:
					String rowId = uri.getPathSegments().get(1);
					count = database.delete(ContactsTableData.TABLE_NAME, 
							ContactsTableData._ID + "=" + rowId +(!TextUtils.isEmpty(selection)? 
							"AND (" + selection + ')' : ""), selectionArgs);
					break;
					
				default:
					throw new IllegalArgumentException("Unkown URI" + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		Log.e(TAG, "update");
		
		SQLiteDatabase database = mDBHelper.getWritableDatabase();
		int count;
		
		switch (mUriMatcher.match(uri)){
		
				case THE_WHOLE_TABLE_URI:
					count = database.update(ContactsTableData.TABLE_NAME, 
							values, selection, selectionArgs);
					break;
					
				case SINGLE_CONTACTS_URI:
					String rowId = uri.getPathSegments().get(1);
					
					count = database.update(ContactsTableData.TABLE_NAME, 
							values, ContactsTableData._ID + "=" + rowId + (!TextUtils.isEmpty(selection) ? 
							"AND (" + selection + ')' : ""), selectionArgs);
					break;
				default:
					throw new IllegalArgumentException("Unkonw Uri" + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}
	
}
