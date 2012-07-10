package com.teamboid.twitter;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.SSLCertificateSocketFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class PushReceiver extends BroadcastReceiver {
	public static final String SENDER_EMAIL = "107821281305";
	public static final String SERVER = "https://192.168.0.9:1337";
	
	public static class PushWorker extends Service{
		
		@Override
		public IBinder onBind(Intent arg0) {
			return null;
		}
		
		@Override
		public int onStartCommand(final Intent intent, int flags, int startId) {
			if(intent.hasExtra("reg")){
			new Thread(new Runnable(){

				@Override
				public void run() {
					try{
						Account acc = AccountService.getCurrentAccount();
						final URL url = new URL(SERVER + "/register?userid="+acc.getId()+
								"&token=" + Uri.encode(intent.getStringExtra("reg")) +
								"&accesstoken=" +  Uri.encode(acc.getToken()) + 
								"&accesssecret=" +  Uri.encode(acc.getSecret()));
						
						HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
						urlConnection.setSSLSocketFactory(SSLCertificateSocketFactory.getInsecure(3000, null) );
						urlConnection.setHostnameVerifier(new HostnameVerifier(){

							@Override
							public boolean verify(String hostname,
									SSLSession session) {
								if(hostname.equals(url.getHost())) return true;
								return false;
							}
							
						});
						urlConnection.setDoOutput(true);
						
						InputStream in = new BufferedInputStream(urlConnection.getInputStream());
						while(in.read() != -1) { };
						
						if(urlConnection.getResponseCode() == 200){
							Log.d("push", "REGISTERED");
						}
						urlConnection.disconnect();
					}catch(Exception e){ e.printStackTrace(); }
				}
				
			}).start();
			} else if(intent.hasExtra("hm")){
				Bundle b = intent.getBundleExtra("hm");
				try {
					JSONObject status = new JSONObject(b.getString("tweet"));
					twitter4j.Status s = new twitter4j.internal.json.StatusJSONImpl(status);
					MultiAPIMethods.showNotification(s, PushWorker.this);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			return Service.START_NOT_STICKY;
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("boidpush", "Got a push message");
		if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
			handleRegistration(context, intent);
		} else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
			handleMessage(context, intent);
		}
	}

	private void handleMessage(Context context, Intent intent) {
		Log.d("boidpush", "Got a message :D");
		context.startService(new Intent(context, PushWorker.class).putExtra("hm", intent.getExtras()));
	}

	private void handleRegistration(Context context, Intent intent) {
		Log.d("boidpush", "REGISTER");
		String registration = intent.getStringExtra("registration_id"); 
	    if (intent.getStringExtra("error") != null) {
	        // Registration failed, should try again later.
	    } else if (intent.getStringExtra("unregistered") != null) {
	        // unregistration done, new messages from the authorized sender will be rejected
	    } else if (registration != null) {
	    	// Send the registration ID to the 3rd party site that is sending the messages.
	    	// This should be done in a separate thread.
	    	// When done, remember that all registration is done. 
	    	Log.d("boidpush", "c2dm worked! :D");
	    	context.startService(new Intent(context, PushWorker.class).putExtra("reg", registration));
	    }
	}

}
