package com.fann.voicesiphelper;
import android.content.*;
import android.app.*;
/** Only our production-signed gateway can request compatibility recovery. */
public final class RecoveryReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  if(!"com.fann.voicesiphelper.REPAIR_BINDING".equals(i.getAction()))return;
  try{
   if(DeployService.busy)return;
   long now=android.os.SystemClock.elapsedRealtime();
   android.content.SharedPreferences p=c.getSharedPreferences("recovery",0);
   long previous=p.getLong("last",0);if(now>=previous&&now-previous<120000&&previous>0)return;
   p.edit().putLong("last",now).apply();
   c.startForegroundService(new Intent(c,DeployService.class).putExtra("action","REPAIR_BINDING"));
  }catch(Exception e){
   NotificationManager n=c.getSystemService(NotificationManager.class);
   n.createNotificationChannel(new NotificationChannel("recovery","权限服务恢复",NotificationManager.IMPORTANCE_DEFAULT));
   PendingIntent open=PendingIntent.getActivity(c,8,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
   n.notify(8,new Notification.Builder(c,"recovery").setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle("Voice SIP 权限服务需要恢复").setContentText("系统限制后台启动，请打开助手并启用网关完成恢复。").setContentIntent(open).setAutoCancel(true).build());
  }
 }
}
