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
 * 
 * 
 */

package org.umit.ns.mobile.api.scanner;

import java.io.BufferedReader;
import java.io.IOException;

import org.umit.ns.mobile.PortScanner;

import android.os.AsyncTask;

public class PrintingThread extends AsyncTask<BufferedReader, String, String>{

    BufferedReader reader;
    
    @Override
    protected String doInBackground(BufferedReader... params) {

        int read;
        reader = params[0];
        char[] buffer = new char[1024];
        StringBuffer output = new StringBuffer();
        try{
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
                publishProgress(output.toString());
                output = new StringBuffer();
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    protected void onPostExecute(String successIp) {
    }
    
    protected void onProgressUpdate(String... params) {
        PortScanner.resultPublish(params[0]);
    }   
}