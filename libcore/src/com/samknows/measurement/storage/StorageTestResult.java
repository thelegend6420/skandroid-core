package com.samknows.measurement.storage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Pair;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.environment.LocationData;
import com.samknows.measurement.environment.NetworkData;
import com.samknows.measurement.environment.NetworkDataCollector;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.schedule.ScheduleConfig.LocationType;
import com.samknows.measurement.schedule.datacollection.LocationDataCollector;
import com.samknows.measurement.test.TestExecutor;
import com.samknows.measurement.util.SKDateFormat;

//Model for the test_result table in the SQLite database 
public class StorageTestResult extends JSONObject{
	
	//Test Result JSONObject implementation
	public static final String JSON_TYPE_ID = "type";
	public static final String JSON_TYPE_NAME = "type_name";
	public static final String JSON_TESTNUMBER = "testnumber";
	public static final String JSON_STATUS_COMPLETE = "status_complete";
	public static final String JSON_DTIME = "dtime";
	public static final String JSON_DATETIME = "datetime";
	public static final String JSON_LOCATION = "location";
	public static final String JSON_RESULT = "result";
	public static final String JSON_SUCCESS = "success";
	public static final String JSON_HRRESULT = "hrresult";
	
	
	public static final int DOWNLOAD_TEST_ID = 0;
	public static final int UPLOAD_TEST_ID = 1;
	public static final int LATENCY_TEST_ID = 2;
	public static final int PACKETLOSS_TEST_ID = 3;
	public static final int JITTER_TEST_ID = 4;
	public static final String UPLOAD_TEST_STRING = "upload";
	public static final String DOWNLOAD_TEST_STRING = "download";
	public static final String LATENCY_TEST_STRING = "latency";
	public static final String PACKETLOSS_TEST_STRING = "packetloss";
	public static final String JITTER_TEST_STRING = "jitter";
	private enum TESTSSTRINGID {
		JHTTPGET, JHTTPGETMT, JHTTPPOST, JHTTPPOSTMT, JUDPLATENCY, JUDPJITTER
	}
	
	
	
	private int _test_id;
	
	private StorageTestResult(int test_id){
		_test_id = test_id;
		put(JSON_TYPE_ID, _test_id+"");
		put(JSON_TYPE_NAME, testIdToString(_test_id));
	}
	
	public String getTestIdAsString() {
		return testIdToString(_test_id);
	}
	
	public static String testIdToString(int test_id) {
		switch (test_id) {
		case UPLOAD_TEST_ID:
			return UPLOAD_TEST_STRING;
		case DOWNLOAD_TEST_ID:
			return DOWNLOAD_TEST_STRING;
		case LATENCY_TEST_ID:
			return LATENCY_TEST_STRING;
		case PACKETLOSS_TEST_ID:
			return PACKETLOSS_TEST_STRING;
		case JITTER_TEST_ID:
			return JITTER_TEST_STRING;
		}
		return "";
	}
	
	public StorageTestResult(String type, long dtime, String location, long success, double result){
		_test_id = testStringToId(type);
		setTime(dtime);
		setLocation(location);
		putLong(JSON_SUCCESS, success);
		setResult(result);
	}
	
	public static int testStringToId(String testString) {
		int ret = -1;
		if (UPLOAD_TEST_STRING.equals(testString)) {
			ret = UPLOAD_TEST_ID;
		} else if (DOWNLOAD_TEST_STRING.equals(testString)) {
			ret = DOWNLOAD_TEST_ID;
		} else if (LATENCY_TEST_STRING.equals(testString)) {
			ret = LATENCY_TEST_ID;
		} else if (PACKETLOSS_TEST_STRING.equals(testString)) {
			ret = PACKETLOSS_TEST_ID;
		} else if (JITTER_TEST_STRING.equals(testString)) {
			ret = JITTER_TEST_ID;
		}
		return ret;
	}
	
	public static String hrResult(String test_type, double value){
		return hrResult(testStringToId(test_type),value);
	}
	
	public static String hrResult(int test_type_id, double value){
		String ret = value +"";
		switch (test_type_id) {
		case UPLOAD_TEST_ID:
		case DOWNLOAD_TEST_ID:
			ret = throughputToString(value);
			break;
		case LATENCY_TEST_ID:
		case JITTER_TEST_ID:
			ret = timeMicrosecondsToString(value);
			break;
		case PACKETLOSS_TEST_ID:
			ret = String.format("%.2f %%", value);
			break;
		}
		return ret;
	}

	private void setResult(double value) {
		String hrresult = "";
		switch (_test_id) {
		case UPLOAD_TEST_ID:
		case DOWNLOAD_TEST_ID:
			hrresult = throughputToString(value);
			break;
		case LATENCY_TEST_ID:
		case JITTER_TEST_ID:
			hrresult = timeMicrosecondsToString(value);
			break;
		case PACKETLOSS_TEST_ID:
			hrresult = String.format("%.2f %%", value);
			break;
		}
		putDouble(JSON_RESULT, value);
		put(JSON_HRRESULT, hrresult);
	}

	public static String throughputToString(double value) {
		String ret = "";
		if (value < 1000) {
			ret = String.format("%.0f bps", value);
		} else if (value < 1000000) {
			ret = String.format("%.2f Kbps", (double) (value / 1000.0));
		} else {
			ret = String.format("%.2f Mbps", (double) (value / 1000000.0));
		}
		return ret;
	}

	public static String timeMicrosecondsToString(double valueMicroseconds) {
		String ret = "";
		if (valueMicroseconds < 1000) {
			ret = String.format("%.0f microseconds", valueMicroseconds );
		} else if (valueMicroseconds < 1000000) {
			ret = String.format("%.0f ms", valueMicroseconds/1000);
		} else {
			ret = String.format("%.2f s", valueMicroseconds/1000000);
		}
		return ret;
	}
	
	
	private void put(String key, String value){
		try{
			super.put(key, value);
		}catch(JSONException je){
			SKLogger.e(StorageTestResult.class, "JSONException "+ key +" "+ value);
		}
	}
	
	private void putLong(String key, long value){
		try{
			super.put(key, value);
		}catch(JSONException je){
			SKLogger.e(StorageTestResult.class, "JSONException "+ key +" "+ value);
		}
	}
	
	private void setTime(long dtime_mills){
		putLong(JSON_DTIME, dtime_mills);
		put(JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(dtime_mills)));
	}
	
	private void putDouble(String key, double value){
		try{
			super.put(key, value);
		}catch(JSONException je){
			SKLogger.e(StorageTestResult.class, "JSONException "+ key +" "+ value);
		}
	}
	
	//Empty constructor
	public StorageTestResult(){}
	
	public void setSuccess(int success){
		put(JSON_SUCCESS, success+"");
	}
	
	public static List<JSONObject> testOutput(String data, TestExecutor forTestExecutor){
		SKLogger.d(StorageTestResult.class, data);
		return testOutput(data.split(SKConstants.RESULT_LINE_SEPARATOR), forTestExecutor);
	}

	
	public static List<JSONObject> testOutput(String[] data, TestExecutor forTestExecutor){
		List<JSONObject> ret = new ArrayList<JSONObject>();
		if (data[0].equals("NETACTIVITY")) {
    		return ret;
		}
		if (data[0].equals("CPUACTIVITY")) {
    		return ret;
		}
		if (data[0].equals("CLOSESTTARGET")) {
			return null;
		}

		TESTSSTRINGID tsid;
		
		try {
			tsid = TESTSSTRINGID.valueOf(data[0]);
		} catch(IllegalArgumentException iae) {
			SKLogger.sAssert(StorageTestResult.class, false);
			return ret;
		}
		
		switch (tsid){
		case JHTTPGET:
		case JHTTPGETMT:
		{
			StorageTestResult testResult = convertThroughputTest(DOWNLOAD_TEST_ID, data);
			
			ret.add(testResult);
		}
			break;
		case JHTTPPOST:
		case JHTTPPOSTMT:
		{
			StorageTestResult testResult = convertThroughputTest(UPLOAD_TEST_ID, data);
			
			ret.add(testResult);
		}
			break;
		case JUDPLATENCY:
		{
			List<StorageTestResult> testResults = convertLatencyTest(data);
			
			ret.addAll(testResults);
		}
			break;
		case JUDPJITTER:
		{
			StorageTestResult testResult = convertJitterTest(data);

			ret.add(testResult);
		}
			break;
		}
		return ret;
	}
	
	private static StorageTestResult convertJitterTest(String[] data){
		StorageTestResult ret = new StorageTestResult(JITTER_TEST_ID);
		long dtime = Long.parseLong(data[1])*1000;
		long success = data[2].equals("OK") ? 1 : 0;
		String target = data[3];
		Double metric = Double.parseDouble(data[12]);
		ret.setTime(dtime);
		ret.setLocation(target);
		ret.setResult(metric);
		ret.putLong(JSON_SUCCESS, success);
		ret.setTest(StorageTestResult.JITTER_TEST_ID);
		ret.setComplete();
		return ret;
	}
	
	private static StorageTestResult convertThroughputTest(int test_id, String[] data){
		StorageTestResult ret = new StorageTestResult(test_id);
		long dtime = Long.parseLong(data[1])*1000;
		long success = data[2].equals("OK") ? 1 : 0;
		String target = data[3];
		Double metric = Double.parseDouble(data[7]);
		ret.setTime(dtime);
		ret.setLocation(target);
		ret.setResult(metric*8);
		ret.putLong(JSON_SUCCESS, success);
		ret.setTest(test_id);
		ret.setComplete();
		return ret;
	}
	
	private static List<StorageTestResult> convertLatencyTest(String[] data){
		List<StorageTestResult> ret = new ArrayList<StorageTestResult>();
		StorageTestResult lat = new StorageTestResult(LATENCY_TEST_ID);
		long dtime = Long.parseLong(data[1])*1000;
		long success = data[2].equals("OK") ? 1 : 0;
		String target = data[3];
		Double latencyResult = Double.parseDouble(data[5]);
		int received = Integer.parseInt(data[9]);
		int lost = Integer.parseInt(data[10]);
		int sent = received + lost;
		double packetLoss = 0.0;
		if(sent != 0){
			packetLoss = 100d * ((double) lost)/sent;
		}
		lat.setTime(dtime);
		lat.setLocation(target);
		lat.setResult(latencyResult);
		lat.putLong(JSON_SUCCESS, success);
		lat.setTest(StorageTestResult.LATENCY_TEST_ID);
		lat.setComplete();
		ret.add(lat);
		StorageTestResult pl = new StorageTestResult(PACKETLOSS_TEST_ID);
		pl.setTime(dtime);
		pl.setResult(packetLoss);
		pl.setLocation(target);
		pl.setTest(StorageTestResult.PACKETLOSS_TEST_ID);
		pl.putLong(JSON_SUCCESS, success);
		pl.setComplete();
		ret.add(pl);
		return ret;
	}
	
	private void setTest(int test_number){
		put(JSON_TYPE_ID, "test");
		put(JSON_TESTNUMBER, ""+test_number);
	}
	
	private void setComplete(){
		put(JSON_STATUS_COMPLETE, "100");
	}
	
	private void setPassiveMetric(){
		put(JSON_TYPE_ID, "passivemetric");
	}
	
	private void setLocation(String target){
		put(JSON_LOCATION, targetToLocation(target));
	}
	
	private static String targetToLocation(String target){
		String ret = target;
		ScheduleConfig config = CachingStorage.getInstance().loadScheduleConfig();
		
		if(config != null && config.hosts.containsKey(target)){
			ret = config.hosts.get(target);
		}
		return ret;
	}
	
}
