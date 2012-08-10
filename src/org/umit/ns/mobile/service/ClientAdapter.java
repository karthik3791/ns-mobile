package org.umit.ns.mobile.service;

import org.umit.ns.mobile.api.ScanCommunication;
import org.umit.ns.mobile.provider.Scanner;
import org.umit.ns.mobile.provider.Scanner.Scans;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.lang.Integer;
import java.lang.String;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

class ClientAdapter implements ScanCommunication {
	private final int ID;
	private Messenger messenger;
	protected boolean pendingStartScan = false;
	private boolean rootAccess;

	private static String scanResultsPath;
	private final static Random random = new Random();

	private static LinkedHashMap<Integer, Integer> scanID_clientID =
			new LinkedHashMap<Integer, Integer>();

	private LinkedHashMap<Integer, ScanWrapper> scans =
			new LinkedHashMap<Integer, ScanWrapper>();

	private ScanWrapper pendingScan = null;

	private ContentResolver contentResolver;
	private String action;

	protected ClientAdapter(int clientID, Messenger messenger,
	                        String scanResultsPath, ContentResolver contentResolver,
	                        String action) {
		this.scanResultsPath = scanResultsPath;
		this.ID = clientID;
		this.messenger = messenger;
		this.contentResolver = contentResolver;
		this.action = action;
	}

	protected void rebind(Messenger messenger) {
		this.messenger = messenger;
	}

	//Parse ARGS; Create a new scan and put it in pendingScan, unique non-duplicate id.
	protected void newScan(String scanArguments) {
		//generate unique scanID
		int scanID = Math.abs(random.nextInt());
		while (scanID_clientID.containsKey(scanID))
			scanID = Math.abs(random.nextInt());

		pendingScan = new ScanWrapper(scanID,ID,contentResolver,scanArguments, scanResultsPath);
	}

	//Start scan in tmp and put it in list, notify Client, add to database
	protected void startScan(boolean rootAccess) {
		this.rootAccess = rootAccess;

		//start scan
		pendingScan.start(rootAccess);

		//put in lists
		scans.put(pendingScan.getScanID(), pendingScan);
		scanID_clientID.put(pendingScan.getScanID(), ID);

		//notify client
		tellClient(RESP_START_SCAN_OK, pendingScan.getScanID(), (rootAccess ? 1 : 0),
				"ScanResultsFilename", null);

		Uri uri = Uri.parse(Scanner.SCANS_URI + "/" + ID + "/" + pendingScan.getScanID());
		ContentValues values = new ContentValues();
		values.put(Scans.CLIENT_ACTION, action);
		values.put(Scans.ROOT_ACCESS, rootAccess ? 1 : 0);
		values.put(Scans.SCAN_ARGUMENTS, pendingScan.arguments);
		values.put(Scans.TASK_PROGRESS, 0);
		values.put(Scans.SCAN_STATE, Scans.SCAN_STATE_STARTED);
		contentResolver.insert(uri, values);

		pendingScan = null;
	}

	//stop scan with id, notify client, clean from list
	protected boolean stopScan(int scanID) {
		ScanWrapper scan = scans.get(scanID);
		if (scan == null) {
			scanProblem(scanID, "No running scan with that scanID");
			return false;
		}
		scan.stop();
		scans.remove(scanID);
		scanID_clientID.remove(scanID);

		//Clean the record from the DB
		Uri scanUri = Uri.parse(Scanner.SCANS_URI.toString() + "/" + ID + "/" + scanID);
		contentResolver.delete(scanUri, null, null);

		tellClient(RESP_STOP_SCAN_OK, scanID, 0, null, null);
		return true;
	}

	//send info message, stop scan, clean from list
	protected void scanProblem(int scanID, String info) {
		tellClient(NOTIFY_SCAN_PROBLEM, scanID, 0, "Info", info);
		ScanWrapper scan = scans.get(scanID);
		if ((scanID != -1) && (scan != null)) {
			scan.stop();
			scans.remove(scanID);
			scanID_clientID.remove(scanID);

			//Clean the record from the DB
			Uri scanUri = Uri.parse(Scanner.SCANS_URI.toString() + "/" + ID + "/" + scanID);
			contentResolver.delete(scanUri, null, null);
		}
	}

	//send notification, clean from list
	protected void scanFinished(int scanID) {
		tellClient(NOTIFY_SCAN_FINISHED, scanID, 0, null, null);
		scans.remove(scanID);
		scanID_clientID.remove(scanID);
	}

	//send progress
	protected void scanProgress(int scanID, int progress) {
		tellClient(NOTIFY_SCAN_PROGRESS, scanID, progress, null, null);
	}

	//Used in ScanService
	protected boolean scanRunning() {
		for (ScanWrapper scan : scans.values()) {
			if (scan.running)
				return true;
		}
		return false;
	}

	//Used in ScanService
	protected void stopAllScans() {
		for (ScanWrapper scan : scans.values()) {
			scan.stop();
		}
		scans.clear();
		scanID_clientID.clear();
	}

	//Used in ScanService
	protected static int getClientIDByScanID(int scanID) {
		return scanID_clientID.get(scanID);
	}

	private boolean tellClient(int RESP_CODE, int ARG1, int ARG2, String key, String value) {
		Message message;
		if (key == null || value == null)
			message = Message.obtain(null, RESP_CODE, ARG1, ARG2);
		else {
			Bundle bundle = new Bundle();
			bundle.putString(key, value);
			message = Message.obtain(null, RESP_CODE, ARG1, ARG2, bundle);
		}

		try {
			messenger.send(message);
			return true;
		} catch (RemoteException e) {
			return false;
		}
	}

	//Do not use this for sending progress
	private boolean tellClient(Message message) {
		if (message.what == NOTIFY_SCAN_PROGRESS)
			return false;

		try {
			messenger.send(message);
			return true;
		} catch (RemoteException e) {
			return false;
		}
	}
}