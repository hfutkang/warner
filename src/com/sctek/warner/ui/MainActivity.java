package com.sctek.warner.ui;

import java.lang.reflect.Method;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.sctek.warner.R;
import com.sctek.warner.WarnerService;
import com.sctek.warner.R.id;
import com.sctek.warner.R.layout;
import com.sctek.warner.R.menu;
import com.sctek.warner.database.ContactsProvideData.ContactsTableData;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;

public class MainActivity extends Activity {
	
	private static final String TAG = "MainActivity";
	private static final int ENABLE_BLUETOOTH_REQUSET = 1;
	public static final String NO_DEVICE_AVAILABLE_ACTION = "NO_DEVICE_AVAILABLE_ACTION";
	public static final String LISTENG_WARNING_SUCCESS = "NO_DEVICE_AVAILABLE_ACTION";
	
	private boolean warnerOn;
	
	private Button listenSwitchBt;
	private Button stopWarningBt;
	private Button contactsBt;
	
	private BluetoothAdapter mBluetoothAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		listenSwitchBt = (Button)findViewById(R.id.warn_listen_switch_bt);
		stopWarningBt = (Button)findViewById(R.id.stop_warning_bt);
		contactsBt = (Button)findViewById(R.id.contacts_bt);
		contactsBt.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this, SmscontactsActivity.class);
				startActivity(intent);
			}
		});
		
		BluetoothManager blM = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = blM.getAdapter();
		
		IntentFilter filter = new IntentFilter(NO_DEVICE_AVAILABLE_ACTION);
		
		registerReceiver(broadcastReceiver, filter);
		
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onResume");
		initView();
		super.onResume();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if(resultCode == RESULT_OK) {
			
			Intent intent = new Intent(this, WarnerService.class);
			startService(intent);
			listenSwitchBt.setEnabled(false);
			
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}
	
	BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if(NO_DEVICE_AVAILABLE_ACTION.equals(intent.getAction())) {
				Toast.makeText(MainActivity.this, 
						"未发现可用设备，请确定警报设备在身边", Toast.LENGTH_SHORT).show();
				listenSwitchBt.setEnabled(true);
			}
			else if(LISTENG_WARNING_SUCCESS.equals(intent.getAction())) {
				listenSwitchBt.setEnabled(true);
				listenSwitchBt.setText(R.string.stop_listen_warning);
				warnerOn = true;
			}
		}
	};
	
	private void initView() {
		
		SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(this);
		warnerOn = sPref.getBoolean("warnerOn", false);
		
		stopWarningBt.setText(R.string.stop_warning);
		if(warnerOn)
			listenSwitchBt.setText(R.string.stop_listen_warning);
		else
			listenSwitchBt.setText(R.string.start_listen_warning);
		
	}
	
	public void onListenSwitchButtonClicked(View v) {
		
		if(mBluetoothAdapter == null) {
			Toast.makeText(this, "你的手机不支持蓝牙", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Intent intent = new Intent(this, WarnerService.class);
		if(!warnerOn) {
			
			if(!mBluetoothAdapter.isEnabled()) {
				Intent eintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(eintent, ENABLE_BLUETOOTH_REQUSET);
				return;
			}
			
			if(needAddContacts()) {
				Toast.makeText(this, "请先添加联系人", Toast.LENGTH_SHORT).show();
				return;
			}
			startService(intent);
			listenSwitchBt.setEnabled(false);
		}
		else {
			stopService(intent);
			
			warnerOn = false;
			listenSwitchBt.setText(R.string.start_listen_warning);
			SharedPreferences sPref = PreferenceManager.
					getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = sPref.edit();
			editor.putBoolean("warnerOn", false);
			editor.commit();
		}
		
	}
	
	public void onStopWarningButtonClicked(View v) {
		
		Intent intent = new Intent(MainActivity.this, WarnerService.class);
		intent.putExtra("stopwarn", true);
		startService(intent);
		
		endTheCall();
	}
	
	private boolean needAddContacts() {
		
		ContentResolver cr = getContentResolver();
		Cursor cursor = cr.query(ContactsTableData.CONTENT_URI, 
				new String[]{ContactsTableData.NUMBER}, null, null, null);
		
		if(cursor.getCount() == 0)
			return true;
		
		return false;
	}
	
	private void endTheCall() {
		TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		
		try {
			Method method = tm.getClass().getDeclaredMethod("getITelephony");
			method.setAccessible(true);
			ITelephony iTelehpony = (ITelephony)method.invoke(tm);
			iTelehpony.endCall();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
