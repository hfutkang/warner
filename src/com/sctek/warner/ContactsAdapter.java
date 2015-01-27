package com.sctek.warner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sctek.warner.database.ContactsProvideData.ContactsTableData;

import android.R.integer;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class ContactsAdapter extends BaseAdapter {
	
	private static final String TAG = "ContactsAdapter";
	
	private ArrayList<Contact> contactList;
	private Context mContext;
	private ContentResolver mContentResolver;
	
	public ContactsAdapter(Context context) {
		Log.e(TAG, "ContactsAdapter");
		
		mContext = context;
		mContentResolver = context.getContentResolver();
		contactList = new ArrayList<ContactsAdapter.Contact>();
		
		loadContactsList();
		
		initContactsListView();
	}
	
	public void loadContactsList() {
		
		Log.e(TAG, "loadContactsList");
		
		Cursor contactsCursor = mContentResolver.
				query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		
		while(contactsCursor.moveToNext()) {
			
			int nameIndex = contactsCursor.getColumnIndex(PhoneLookup.DISPLAY_NAME);
			String name = contactsCursor.getString(nameIndex);
			
			int idIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts._ID);
			String contactId = contactsCursor.getString(idIndex);
			
			Cursor numberCursor = mContentResolver.
					query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
							null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{contactId}, null);
			
			while(numberCursor.moveToNext()) {
				
				Contact c = new Contact();
				
				int numberIndex = numberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
				String number = numberCursor.getString(numberIndex);
				
				c.name = name;
				c.number = number;
				c.dial = false;
				c.sendMg = false;
				
				contactList.add(c);
				
			}
			numberCursor.close();
		}
		contactsCursor.close();	
	}
	
	public void initContactsListView() {
		Log.e(TAG, "initContactsListView");
		Cursor warnerCursor = mContentResolver.
				query(ContactsTableData.CONTENT_URI, new String[]{ContactsTableData.NUMBER, 
						ContactsTableData.SEND_MG, ContactsTableData.DIAL}, null, null, null);
		while(warnerCursor.moveToNext()) {
			String number = warnerCursor.getString(0);
			int sendmg = warnerCursor.getInt(1);
			int dial = warnerCursor.getInt(2);
			Log.e(TAG, number + " s:" + sendmg + " d:" + dial);
			if(!markerThisNumber(number, sendmg, dial))
				mContentResolver.delete(ContactsTableData.CONTENT_URI, 
						ContactsTableData.NUMBER + "=?", new String[]{number});
		}
		
		warnerCursor.close();
	}
	
	public boolean markerThisNumber(String num, int sendmg, int dial) {
		Log.e(TAG, "markerThisNumber");
		for(Contact c : contactList) {
			if(num.equals(c.number)) {
				c.sendMg = sendmg > 0?true:false;
				c.dial = dial > 0?true:false;
				Log.e(TAG, c.number + " s:" + sendmg + " d:" + dial);
				return true;
			}
		}
		return false;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return contactList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return contactList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		Log.e(TAG, "getView");
		
		final Contact c = contactList.get(position);
		Holder holder = new Holder();
		Log.e(TAG, c.name + ":" + c.number + " s:" + c.sendMg + " d:" + c.dial);
		
		if(convertView == null) {
			
			convertView = LayoutInflater.from(mContext).
					inflate(R.layout.contact_listview_item, null);
			
			TextView nameTv = (TextView)convertView.findViewById(R.id.name_tv);
			TextView numberTv = (TextView)convertView.findViewById(R.id.number_tv);
			CheckBox mCb = (CheckBox)convertView.findViewById(R.id.sendmg_cb);
			CheckBox dCb = (CheckBox)convertView.findViewById(R.id.dial_cb);
			
			CheckBoxCheckedListener cbL = new CheckBoxCheckedListener();
			
			mCb.setOnCheckedChangeListener(cbL);
			dCb.setOnCheckedChangeListener(cbL);
			
			holder.nameTv = nameTv;
			holder.numberTv = numberTv;
			holder.dCb = dCb;
			holder.mCb = mCb;
			holder.cbL = cbL;
			
			convertView.setTag(holder);
			
		} else {
			holder = (Holder)convertView.getTag();
		}
		holder.cbL.setContact(c);
		
		holder.nameTv.setText(c.name);
		holder.numberTv.setText(c.number);
		holder.dCb.setChecked(c.dial);	
		holder.mCb.setChecked(c.sendMg);
		
		CheckBoxCheckedListener cbL = new CheckBoxCheckedListener();
		
		return convertView;
	}
	
	public class Holder {
		
		public TextView nameTv;
		public TextView numberTv;
		public CheckBox mCb;
		public CheckBox dCb;
		public CheckBoxCheckedListener cbL;
	}
	
	public class Contact {
		
		public String name;
		public String number;
		public boolean sendMg;
		public boolean dial;
		
	}
	
	public class CheckBoxCheckedListener implements OnCheckedChangeListener {
		
		private Contact mContact;
		
		public void setContact(Contact c) {
			mContact = c;
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			// TODO Auto-generated method stub
			switch (buttonView.getId()) {
			case R.id.sendmg_cb:
				mContact.sendMg = isChecked;
				break;
			case R.id.dial_cb:
				mContact.dial = isChecked;
				break;
			default:
				break;
			
			}
		}
		
	}

}
