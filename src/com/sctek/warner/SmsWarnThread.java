package com.sctek.warner;

import java.util.ArrayList;
import java.util.TimerTask;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.telephony.SmsManager;
import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.sctek.warner.database.ContactsProvideData.ContactsTableData;


public class SmsWarnThread extends TimerTask implements BDLocationListener{
	
	public static final String TAG = "WarnThread";
	public static final String IN_DANGER = "我有危险";
	
	private LocationClient mLocationClient;
	private String mAddress;
	private Double latitude, longitude;
	
	private ArrayList<String> smsContactsList;
	
	public SmsWarnThread(Context context) {
		
		mLocationClient = new LocationClient(context);
		
		LocationClientOption option = new LocationClientOption();
		option.setIsNeedAddress(true);//返回的定位结果包含地址信息
		mLocationClient.setLocOption(option);
		
		mLocationClient.registerLocationListener(this);
		
		mLocationClient.start();
		
		initSmsContactsList(context);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (mLocationClient != null && mLocationClient.isStarted())
			Log.e(TAG, "run");
			mLocationClient.requestLocation();
	}

	@Override
	public void onReceiveLocation(BDLocation location) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onReceiveLocation");
		if (location == null)
            return ;
		
		int locType = location.getLocType();
		if(locType == BDLocation.TypeNone 
				|| locType == BDLocation.TypeServerError) {
			Log.e(TAG, "onReceiveLocation123");
			sendSmsMessageToContacts(IN_DANGER);
			return;
			
		}
		Log.e(TAG, "onReceiveLocation111");
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		mAddress = location.getAddrStr();
		
		StringBuffer mgBuffer = new StringBuffer();
		mgBuffer.append("经度:" + latitude);
		mgBuffer.append("\n纬度:" + longitude);
		mgBuffer.append("\n位置:" + mAddress);
		
		sendSmsMessageToContacts(mgBuffer.toString());
		
		StringBuffer sb = new StringBuffer(256);
		sb.append("time : ");
		sb.append(location.getTime());
		sb.append("\nerror code : ");
		sb.append(location.getLocType());
		sb.append("\nlatitude : ");
		sb.append(location.getLatitude());
		sb.append("\nlontitude : ");
		sb.append(location.getLongitude());
		sb.append("\nradius : ");
		sb.append(location.getRadius());
		if (location.getLocType() == BDLocation.TypeGpsLocation){
			sb.append("\nspeed : ");
			sb.append(location.getSpeed());
			sb.append("\nsatellite : ");
			sb.append(location.getSatelliteNumber());
		} else if (location.getLocType() == BDLocation.TypeNetWorkLocation){
			sb.append("\naddr : ");
			sb.append(location.getAddrStr());
		} 
	}
	
	private void initSmsContactsList(Context context) {
		
		smsContactsList = new ArrayList<String>();
		
		ContentResolver cr = context.getContentResolver();
		
		Cursor cursor = cr.query(ContactsTableData.CONTENT_URI, 
				new String[] {"number"}, 
				ContactsTableData.SEND_MG + "=?", new String[]{"1"}, null);
		
		while (cursor.moveToNext()) {
			
			String num = cursor.getString(0);
			smsContactsList.add(num);
			
			sendSmsMessage(num, IN_DANGER);		
		}
		cursor.close();
	}
	
	private static void sendSmsMessage( String addr, String msg) {
		
		SmsManager smsMgr = SmsManager.getDefault();
		
		try {
			smsMgr.sendTextMessage(addr, null, msg, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void sendSmsMessageToContacts(String msg) {
		
		for(String num : smsContactsList)
			sendSmsMessage(num, msg);
	}

}
