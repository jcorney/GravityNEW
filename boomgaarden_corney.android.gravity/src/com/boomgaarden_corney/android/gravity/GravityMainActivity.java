package com.boomgaarden_corney.android.gravity;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class GravityMainActivity extends Activity implements
		SensorEventListener {

	private final String DEBUG_TAG = "DEBUG_GRAVITY";
	private final String SERVER_URL = "http://54.86.68.241/gravity/test.php";

	private TextView txtResults;
	private SensorManager sensorManager;

	private String errorMsg;

	private float gravityAccuracy;
	private float gravityValue0;
	private float gravityValue1;
	private float gravityValue2;
	private float gravityMaxRange = 0;
	private float gravityPower = 0;
	private float gravityResolution = 0;
	private int  gravitySensorType;
	private int numGravityChanges = 0;
	private int gravityVersion = 0;
	private int gravityHashCode = 0;
	private Sensor mGravity;
	private long gravityTimeStamp;
	private String gravityVendor;

	private List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsGravity = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsSensor = new ArrayList<NameValuePair>();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gravity_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);

		// Setup Gravity Manager and Provider
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mGravity = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		

		setDeviceData();
		showDeviceData();
		sendDeviceData();

		if (mGravity == null){
			setErrorMsg("No Gravity Detected");
			showErrorMsg();
			sendErrorMsg();
		} else{
			setSensorData();
			showSensorData();
			sendSensorData();
		}
		

	}

	/* Request Gravity updates at startup */
	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);

	}

	/* Remove the Gravitylistener updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (mGravity != null) {
			if ((event.sensor.getType() == mGravity.getType()) && numGravityChanges < 10) {
				
				numGravityChanges++;
				setGravityData(event);
				showGravityData();
				sendGravityData();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gravity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("GRAVITY")) {
			writer.write(buildPostRequest(paramsGravity));
			paramsGravity = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		}  else if (postParameters.equals("SENSOR")) {
			writer.write(buildPostRequest(paramsSensor));
			paramsSensor = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();
		
		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Gravity
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setDeviceData() {
		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));
	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private void setGravityData(SensorEvent gravity) {
		gravityAccuracy = gravity.accuracy;
		gravitySensorType = gravity.sensor.getType();
		gravityTimeStamp = gravity.timestamp;
		gravityValue0 = gravity.values[0];
		gravityValue1 = gravity.values[1];
		gravityValue2 = gravity.values[2];
		gravityHashCode = gravity.hashCode();

		paramsGravity.add(new BasicNameValuePair("Gravity Update Count",
				String.valueOf(numGravityChanges)));
		paramsGravity.add(new BasicNameValuePair("Accuracy", String
				.valueOf(gravityAccuracy)));
		paramsGravity.add(new BasicNameValuePair("Sensor Type", String
				.valueOf(gravitySensorType)));
		paramsGravity.add(new BasicNameValuePair("Time Stamp", String
				.valueOf(gravityTimeStamp)));
		paramsGravity.add(new BasicNameValuePair(
				"Value 0 Acceleration minus Gx on the x axis", String
						.valueOf(gravityValue0)));
		paramsGravity.add(new BasicNameValuePair(
				"Value 1 Acceleration minus Gy on the y axis", String
						.valueOf(gravityValue1)));
		paramsGravity.add(new BasicNameValuePair(
				"Value 2 Acceleration minus Gz on the z axis", String
						.valueOf(gravityValue2)));
		paramsGravity.add(new BasicNameValuePair(
				"Hash Code Value", String
						.valueOf(gravityHashCode)));
	}
	
	private void setSensorData() {
		gravityMaxRange = mGravity.getMaximumRange();
		gravityPower = mGravity.getPower();
		gravityResolution = mGravity.getResolution();
		gravityVendor = mGravity.getVendor();
		gravityVersion = mGravity.getVersion();	
		
		paramsSensor.add(new BasicNameValuePair("Max Range", String
						.valueOf(gravityMaxRange)));
		paramsSensor.add(new BasicNameValuePair("Power", String
				.valueOf(gravityPower)));
		paramsSensor.add(new BasicNameValuePair("Resolution", String
				.valueOf(gravityResolution)));
		paramsSensor.add(new BasicNameValuePair("Vendor", String
				.valueOf(gravityVendor)));
		paramsSensor.add(new BasicNameValuePair("Version", String
				.valueOf(gravityVersion)));
	}

	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");

		txtResults.append("\n");

	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void showGravityData() {
		StringBuilder results = new StringBuilder();

		results.append("Gravity Update Count: "
				+ String.valueOf(numGravityChanges) + "\n");
		results.append("Gravity Accuracy: " + String.valueOf(gravityAccuracy) + "\n");
		results.append("Gravity Sensor Type: " + String.valueOf(gravitySensorType) + "\n");
		results.append("Gravity Time Stamp: " + String.valueOf(gravityTimeStamp) + "\n");
		results.append("Gravity Vaule 0 (X axis): " + String.valueOf(gravityValue0) + "\n");
		results.append("Gravity Vaule 1 (Y axis): " + String.valueOf(gravityValue1) + "\n");
		results.append("Gravity Vaule 2 (Z axis): " + String.valueOf(gravityValue2) + "\n");
		results.append("Gravity Hash Code " + String.valueOf(gravityHashCode) + "\n");
		
		txtResults.append(new String(results));
		txtResults.append("\n");
	}
	
	private void showSensorData() {
		StringBuilder results = new StringBuilder();
		
		results.append("Max Range: " + String.valueOf(gravityMaxRange) + "\n");
		results.append("Power: " + String.valueOf(gravityPower) + "\n");
		results.append("Resolution: " + String.valueOf(gravityResolution) + "\n");
		results.append("Vendor: " + String.valueOf(gravityVendor) + "\n");
		results.append("Version: " + String.valueOf(gravityVersion) + "\n");
		
		txtResults.append(new String(results));
		txtResults.append("\n");
	}

	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Gravity info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Gravity info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendGravityData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Gravity info
			new SendHttpRequestTask().execute(SERVER_URL, "GRAVITY");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendSensorData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Gravity info
			new SendHttpRequestTask().execute(SERVER_URL, "SENSOR");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}
