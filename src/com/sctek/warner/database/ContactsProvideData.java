package com.sctek.warner.database;

import android.net.Uri;
import android.provider.BaseColumns;

public class ContactsProvideData {
	
	public static final String AUTHORITY = "com.sctek.provider.ContactsProvider";
	
	public static final String DATABASE_NAME = "warnercontacts.db";
	public static final int DATABASE_VERSION = 1;
	public static final String DEVICE_TABLE_NAME = "contacts";
	
	private ContactsProvideData() {}
	
	public static final class ContactsTableData implements BaseColumns {
		
		private ContactsTableData() {}
		
		public static final String TABLE_NAME = "contacts";
		
		public static final Uri CONTENT_URI = 
				Uri.parse("content://" + AUTHORITY + "/contacts");
		
		public static final String CONTENT_TYPE = 
				"vnd.android.cursor.dir/vnd.sctek.warner";
		
		public static final String CONTENT_ITEM_TYPE =
				"vnd.android.cursor.item/vnd.sctek.warner";
		
		public static final String NAME = "name";
		
		public static final String NUMBER = "number";
		
		public static final String SEND_MG = "sendmg";
		
		public static final String DIAL = "dial";
		
	}

}
