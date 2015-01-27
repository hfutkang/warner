package com.sctek.warner;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

import com.sctek.warner.database.ContactsProvideData.ContactsTableData;
import com.sctek.warner.ui.MainActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.SurfaceHolder;

public class WarnerService extends Service implements LeScanCallback {
	
	private final static String TAG = WarnerService.class.getSimpleName();
	
	private final static String DEVICE_NAME = "WARNER";
	private final static String NOTIFICATION_TITLE = "警报器";
	private final static String NOAVAILABLE_DEVICE = "未发现警报设备";
	private final static String ON_LISTENNING = "警报监听中";
	private final static String WARNER_LISTENER_EXIT = "警报监听已停止";
	
	private final static long WARNING_PERIOD = 1*60*1000;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    
    private TelephonyManager mTelephonyManager;
    private MyPhoneStateListener mPhoneStateListener;
    
    private String mBleAddress;
    
    private boolean scanning;
    private boolean listenning;
    private boolean warnThreadRunning;
    
    private List<BluetoothGattService> serviceList;
	private List<BluetoothGattCharacteristic> characterList;
	
	private final static String SERVICE_UUID = "0000fee9-0000-1000-8000-00805f9b34fb";
	private final static String TX_CHARA_UUID = "d44bc439-abfd-45a2-b575-925416129600";
	private final static String RX_CHARA_UUID = "d44bc439-abfd-45a2-b575-925416129607";
	
	private UUID serviceUuid = UUID.fromString(SERVICE_UUID);
	private UUID txCharaUuid = UUID.fromString(TX_CHARA_UUID);
	private UUID rxCharaUuid = UUID.fromString(RX_CHARA_UUID);
	
	private Timer warnTimer;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onStartCommand");
		if(intent != null&&intent.getBooleanExtra("stopwarn", false)) {
			if(warnTimer != null)
				warnTimer.cancel();
			
			if(mPhoneStateListener != null)
				mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
			
			warnTimer = null;
			mPhoneStateListener = null;
			return super.onStartCommand(intent, flags, startId);
		}
		
		mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		mBluetoothManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		
		warnThreadRunning = false;
		listenning = true;
		
		scanLeDevice();
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDestroy");
		try {
		SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sPref.edit();
		editor.putBoolean("warnerOn", false);
		editor.commit();
		
		disconnect();
		
		if(warnTimer != null) {
			warnTimer.cancel();
		}
		
		if(mPhoneStateListener != null)
			mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		
		notify(WARNER_LISTENER_EXIT);
		super.onDestroy();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		// TODO Auto-generated method stub
		String deviceName = device.getName();
		try {
			Log.e(TAG, deviceName);
		} catch (NullPointerException e) {
			// TODO: handle exception
			Log.e(TAG, "deviceName is null");
		}
		
		if(deviceName !=null&&deviceName.equals(DEVICE_NAME)) {
			
			scanning = false;
			mBluetoothAdapter.stopLeScan(this);
			
			mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mBleGattCallback);
			
			mBleAddress = device.getAddress();
			Log.e(TAG, device.getAddress() + " " + device.getName() + " " + rssi);
		}
	}
	
	public void scanLeDevice() {
		Log.e(TAG, "scanLeDevice");
		if(mBluetoothAdapter.enable()) {
			
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					if(scanning) {
						mBluetoothAdapter.stopLeScan(WarnerService.this);
						Log.e(TAG, "le scan stopped");
						scanning = false;
						listenning = false;
						WarnerService.this.notify(NOAVAILABLE_DEVICE);
						sendBroadcast(new Intent(MainActivity.NO_DEVICE_AVAILABLE_ACTION));
//						mPhoneStateListener = new MyPhoneStateListener(WarnerService.this);
//						mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//						warnTimer = new Timer();
//						warnTimer.schedule(new SmsWarnThread(getApplicationContext()), 10000);
						stopSelf();
					}
				}
			}, 10000);
			
			scanning = true;
			mBluetoothAdapter.startLeScan(this);
			
		}
	}
	
	public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBleAddress != null && address.equals(mBleAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mBleGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBleAddress = address;
        return true;
    }
	
	public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        listenning = false;
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
    }
	
	private BluetoothGattCallback mBleGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onConnectionStateChange");
			switch (newState) {
			case BluetoothProfile.STATE_CONNECTED:
				Log.e(TAG, "STATE_CONNECTED");
				gatt.discoverServices();
				break;
			case BluetoothProfile.STATE_DISCONNECTED:
				Log.e(TAG, "STATE_DISCONNECTED");
				if(listenning)
					connect(mBleAddress);
				break;
			case BluetoothProfile.STATE_CONNECTING:
				Log.e(TAG, "STATE_CONNECTING");
				break;
			case BluetoothProfile.STATE_DISCONNECTING:
				Log.e(TAG, "STATE_DISCONNECTING");
				break;
			}
			super.onConnectionStateChange(gatt, status, newState);
		}
		
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			Log.e(TAG, "onServicesDiscovered");
			if (status == BluetoothGatt.GATT_SUCCESS) {
				serviceList = gatt.getServices();
				for (int i = 0; i < serviceList.size(); i++) {
					BluetoothGattService theService = serviceList.get(i);
					Log.e(TAG, "ServiceName:" + theService.getUuid());
					if(theService.getUuid().equals(serviceUuid)) {
						characterList = theService.getCharacteristics();
						for (int j = 0; j < characterList.size(); j++) {
							Log.e(TAG,
									"---CharacterName:"
											+ characterList.get(j).getUuid());
							BluetoothGattCharacteristic bleGattCharacteristic 
															= characterList.get(j);
							
							if(bleGattCharacteristic.getUuid().equals(rxCharaUuid)) {
								List<BluetoothGattDescriptor> descriptors = 
										bleGattCharacteristic.getDescriptors();
								BluetoothGattDescriptor mDescriptor = descriptors.get(0);
								mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
								gatt.writeDescriptor(mDescriptor);
								gatt.setCharacteristicNotification(bleGattCharacteristic, true);
								
								SharedPreferences sPref = PreferenceManager.
										getDefaultSharedPreferences(getApplicationContext());
								SharedPreferences.Editor editor = sPref.edit();
								editor.putBoolean("warnerOn", true);
								editor.commit();
								
								Intent intent = new Intent(MainActivity.LISTENG_WARNING_SUCCESS);
								sendBroadcast(intent);
								
								WarnerService.this.notify(ON_LISTENNING);
							}
						}
					}
				}
			}
			super.onServicesDiscovered(gatt, status);
		}
		
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			Log.e(TAG, "onCharacteristicRead");
			super.onCharacteristicRead(gatt, characteristic, status);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			Log.e(TAG, "onCharacteristicWrite"+ "  status:" + status);
			super.onCharacteristicWrite(gatt, characteristic, status);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			Log.e(TAG, "onCharacteristicChanged" + 
					" " + characteristic.getUuid() + 
					" " + characteristic.getProperties());
			byte[] v = characteristic.getValue();
			
			if(!warnThreadRunning) {
				
				warnTimer = new Timer();
				warnTimer.schedule(new SmsWarnThread(getApplicationContext()), 
						WARNING_PERIOD, WARNING_PERIOD);
				
				mPhoneStateListener = new MyPhoneStateListener(WarnerService.this);
				mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
				
			}
			super.onCharacteristicChanged(gatt, characteristic);
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			Log.e(TAG, "onDescriptorRead");
			super.onDescriptorRead(gatt, descriptor, status);
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			Log.e(TAG, "onDescriptorWrite");
			super.onDescriptorWrite(gatt, descriptor, status);
		}

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			Log.e(TAG, "onReliableWriteCompleted");
			super.onReliableWriteCompleted(gatt, status);
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			Log.e(TAG, "onReadRemoteRssi");
			super.onReadRemoteRssi(gatt, rssi, status);
		}
	};
	
	public class MyPhoneStateListener extends PhoneStateListener {
		
		private int dialIndex = 0;
		private ArrayList<String> dialNumberList;
		
		private boolean calling = false;
		
		public MyPhoneStateListener(Context context) {
			
			dialNumberList = new ArrayList<String>();
			
			ContentResolver cr = getContentResolver();
			Cursor cursor = cr.query(ContactsTableData.CONTENT_URI, 
									new String[]{ContactsTableData.NUMBER}, 
									ContactsTableData.DIAL + "=?", new String[]{"1"}, null);
			while(cursor.moveToNext()) {
				String num = cursor.getString(0);
				dialNumberList.add(num);
			}
			cursor.close();
		}
		
		private String getNextDialNumber() {
			String num;
			if(dialNumberList.size() == 0) 
				return null;
			else {
				num = dialNumberList.get(dialIndex);
				dialIndex = dialIndex + 1 < dialNumberList.size() ? dialIndex+1:0;
			}
			
			return num;
		}
		
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);
			switch (state) {
			
			case TelephonyManager.CALL_STATE_IDLE:
				if (calling&&LastCallSucceed()) {
					break;
				}
				PhoneCall(getNextDialNumber());
				if(!calling)
					calling = true;
				break;
				
			case TelephonyManager.CALL_STATE_OFFHOOK:
				break;
				
			default:
				break;
			}
		}
	}
	
	private void PhoneCall(String num) {
		Log.e(TAG, num);
		if(num == null)
			return;
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Uri localUri = Uri.parse("tel:" + num);
		Intent call = new Intent(Intent.ACTION_CALL, localUri);
		call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(call);
	}
	
	private boolean LastCallSucceed() {

		String[] projection = new String[] { Calls.NUMBER, Calls.DURATION };

		ContentResolver cr = getContentResolver();
		try {
		final Cursor cur = cr.query(android.provider.CallLog.Calls.CONTENT_URI,
				projection, null, null, Calls.DEFAULT_SORT_ORDER);
		
		if (cur.moveToFirst()) {
			int duration = cur.getInt(1);
			if (duration > 0) {
				cur.close();
				return true;
			}
		}
		cur.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void notify(String mBody) {
		Log.e(TAG, "notify");
		int notificationId = 100;
		Notification.Builder notification = new Notification.Builder(WarnerService.this)
        .setWhen(System.currentTimeMillis())
        .setContentTitle(NOTIFICATION_TITLE)
        .setContentText(mBody)
        .setSmallIcon(R.drawable.ic_launcher);
//        .setTicker(id);

	    NotificationManager notificationManager =
	        (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
	    
	    Notification nf = notification.getNotification();
	    notificationManager.cancel(notificationId);
	    notificationManager.notify(notificationId, nf);
	    
	}

}
