/*
Android Network Scanner Umit Project
Copyright (C) 2011 Adriano Monteiro Marques

Author: Angad Singh <angad@angad.sg>

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 */

/**
 * @author angadsg
 */

package org.umit.ns.mobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.umit.ns.mobile.api.cmdLine;
import org.umit.ns.mobile.model.FileManager;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class Traceroute extends Activity{
    
    TextView cmd;
    static boolean started = false;
    static Button start;
    static ListView lv;
    static SimpleAdapter sa;
    static List<HashMap<String, String>> fillMaps;
    TextView list_host;

    
    AsyncTask<String, String, String> traceroute;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.traceroute);
        
        start = (Button)findViewById(R.id.startTraceroute);
        start.setOnClickListener(tracerouteLoad);

        cmd = (TextView)findViewById(R.id.traceroutecmd);
        lv = (ListView)findViewById(R.id.listView);
        String[] f = new String[] {"host"};
        int[] t = new int[] { R.id.host };
        fillMaps = new ArrayList<HashMap<String, String>>();
        sa = new SimpleAdapter(this, fillMaps, R.layout.list_item, f, t);
        lv.setAdapter(sa);
        //lv.setOnItemClickListener();

    }
    
    public OnClickListener tracerouteLoad = new OnClickListener() {
        public void onClick(View v) {
            traceroute = new cmdLine();
            traceroute.execute("/data/local/busybox " + cmd.getText().toString(), "traceroute");
            started = true;
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { 
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.cmdmenu, menu);
        return true;
    }
    
    public static void onDone()
    {
        started = false;
        //shellUtils.killProcess("/data/local/busybox");
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.clear:
            clearList();
            return true;
        case R.id.logs:
            loadLogs();
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void loadLogs() {        
        Intent n = new Intent(Traceroute.this, LogsViewer.class);
        startActivityForResult(n, 0);
    }

    public static void addToList(String str) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("host", str);
        fillMaps.add(map);
        sa.notifyDataSetChanged();
    }
    
    public static void clearList() {
        fillMaps.clear();
        sa.notifyDataSetChanged();
    }
    
    /**
     * Static UI methods
     */
    public static void resultPublish(String string) {
        Log.v("traceroute", string);
        FileManager.write("traceroute", string);
        //addToList(string.substring(string.indexOf(' '), string.indexOf('(')));
        addToList(string);
    }
}
