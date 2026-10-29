package com.fann.voicesiphelper;
import android.content.*;
public final class UpdateReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  if(!Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction()))return;
  try{c.startForegroundService(new Intent(c,DeployService.class).putExtra("action","UPDATE_RUNTIME"));}catch(Exception ignored){}
 }
}
