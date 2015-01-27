package com.sctek.warner.ui;

import com.sctek.warner.ContactsAdapter;
import com.sctek.warner.ContactsAdapter.Contact;
import com.sctek.warner.R;
import com.sctek.warner.R.layout;
import com.sctek.warner.database.ContactsProvideData.ContactsTableData;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.ListViewAutoScrollHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class SmscontactsActivity extends Activity {

	private static final String TAG = "SmscontactsActivity";
	
	private ContactsAdapter contactsAdapter;
	private ListView listView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "onCreate");
		setContentView(R.layout.activity_smscontacts);
		
		initView();
	}
	
	private void initView() {
		Log.e(TAG, "initView");
		
		listView = (ListView)findViewById(R.id.sms_contacts_lv);
		contactsAdapter = new ContactsAdapter(this);
		
		listView.setAdapter(contactsAdapter);
	}
	
	public void onSaveButtonClicked(View v) {
		
		ContentResolver cr = getContentResolver();
		Cursor cursor = cr.query(ContactsTableData.CONTENT_URI, null, null, null, null);
		for(int i = 0; i < contactsAdapter.getCount(); i++) {
			
			Contact c = (Contact) contactsAdapter.getItem(i);
			
			cursor.moveToPosition(-1);
			
			boolean isExists = false;
			boolean changed = false;
			while (cursor.moveToNext()) {
				
				String num = cursor.getString(1);
				boolean sendMg = cursor.getInt(2) > 0?true:false;
				boolean dial = cursor.getInt(3) > 0?true:false;
				
				if (c.number.equals(cursor.getString(1))) {
					
					isExists = true;
					if(!c.sendMg&&!c.dial)
						cr.delete(ContactsTableData.CONTENT_URI, 
								ContactsTableData.NUMBER + "=?", new String[]{c.number});
					else if(c.sendMg!=sendMg || c.dial!=dial)
						changed = true;
					
					break;
				}
			}
			
			if(!isExists) {
				
				ContentValues cv = new ContentValues();
				cv.put(ContactsTableData.NAME, c.name);
				cv.put(ContactsTableData.NUMBER, c.number);
				cv.put(ContactsTableData.SEND_MG, c.sendMg?1:0);
				cv.put(ContactsTableData.DIAL, c.dial?1:0);
				
				cr.insert(ContactsTableData.CONTENT_URI, cv);
			}
			
			else if(changed){
				ContentValues cv = new ContentValues();
				cv.put(ContactsTableData.SEND_MG, c.sendMg?1:0);
				cv.put(ContactsTableData.DIAL, c.dial?1:0);
				
				cr.update(ContactsTableData.CONTENT_URI, cv, 
						ContactsTableData.NUMBER + "=?", new String[]{c.number});
			}
		}
		
		finish();
	}
}
