package com.fann.voicesiphelper;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.os.*;
import java.io.*;
import java.util.concurrent.*;

public class DeployService extends Service {
 static volatile boolean busy;
 static volatile String progress="就绪";
 final ExecutorService executor=Executors.newSingleThreadExecutor();
 PowerManager.WakeLock wake;
 public void onCreate(){super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("deploy","部署与管理",NotificationManager.IMPORTANCE_LOW));}
 void report(String s){progress=s;getSharedPreferences("state",0).edit().putString("progress",s).apply();PendingIntent pending=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);startForeground(1,new Notification.Builder(this,"deploy").setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("Voice SIP 助手").setContentText(s).setContentIntent(pending).setOngoing(true).build());}
 public int onStartCommand(Intent intent,int flags,int id){
  Root.selectAgent(this);
  String action=intent==null?null:intent.getStringExtra("action");
  if(action==null){stopSelf();return START_NOT_STICKY;}
  if(busy)return START_NOT_STICKY;busy=true;report("正在处理…");
  wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"voice-sip-assistant:deploy");wake.acquire(900000);
  final boolean healthOn=intent.getBooleanExtra("healthEnabled",false);final String healthMinutes=intent.getStringExtra("healthMinutes");
  executor.submit(()->{try{if(action.equals("HEALTH_CONFIG")){if(!Root.run("id -u",15).trim().equals("0"))throw new IOException("请授予 Root");int minutes=HealthPolicy.minutes(healthMinutes);Root.healthConfigure(this,healthOn,minutes);refresh();report(healthOn?"已开启自动恢复，每 "+minutes+" 分钟检查一次":"已关闭自动检查，现有通话服务继续运行");}else execute(action);}catch(Exception e){String s=e.getMessage();if(e instanceof java.net.UnknownHostException)report("无法解析 GitHub 地址，请检查手机网络或稍后重试");else if(e instanceof java.net.SocketTimeoutException)report("网络连接超时，请稍后重试；已校验的完整安装包会保留");else if(e instanceof javax.net.ssl.SSLException)report("无法建立安全下载连接，请检查手机时间和网络");else report(s!=null&&(e instanceof IOException)?s:"操作失败，请检查网络、Root 授权后重试");}finally{busy=false;if(wake!=null&&wake.isHeld())wake.release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}});return START_NOT_STICKY;
 }
 void execute(String action)throws Exception{
  if(action.equals("CHECK")){
   Releases.Release r=Releases.latest();getSharedPreferences("state",0).edit().putString("latest",r.version).apply();report("官方最新正式版："+r.version);return;
  }
  if(!Root.run("id -u",15).trim().equals("0"))throw new IOException("请在 BakaSU 为 Voice SIP 助手授予 Root");
  switch(action){
   case "DEPLOY":deploy();break;
   case "SHIZUKU":
    prepareShizuku();Root.startShizuku(this);refresh();report("Shizuku 已启动；请在官方 App 中点 Grant Shizuku 并允许");break;
   case "REFRESH":refresh();report("状态已刷新");break;
   case "OFF":Root.control("off");refresh();report("关闭接管请求已提交；通话中会等挂断后生效");break;
   case "ON":
    prepareShizuku();Root.startShizuku(this);Root.run("am start -n "+Root.AGENT+"/io.github.sparkcyf.voicesipagent.app.MainActivity",15);Root.control("companion");Root.control("on");Root.repairMtk(this,false);refresh();report("伴侣模式接管请求已提交，保留本机静音设置；来电行为由服务器路由决定");break;
   case "HEALTH_NOW":Root.healthNow(this);refresh();report("已提交立即检查，请稍后查看自动恢复状态");break;
   case "HEALTH_STATUS":healthStatus();break;
   case "MTK_FIX":
    verifyShizuku();Root.startShizuku(this);Root.run("am start -n "+Root.AGENT+"/io.github.sparkcyf.voicesipagent.app.MainActivity",15);Root.repairMtk(this,true);refresh();report("联发科权限服务已连接；以后可用本助手的启用按钮恢复");break;
   case "RECONNECT":Root.control("reconnect");refresh();report("重连请求已提交，请刷新查看注册状态");break;
   case "RESTORE":Root.restoreDialer();refresh();report("接管已关闭，已恢复系统电话；请实测小米互联来电");break;
   case "BOOT_ON":prepareShizuku();Root.startShizuku(this);Root.autostart(this,true);refresh();report("已开启：开机解锁后准备 Shizuku，不自动开启接管");break;
   case "BOOT_OFF":Root.autostart(this,false);refresh();report("已关闭本助手的 Shizuku 开机准备");break;
   default:throw new IOException("无效操作");
  }
 }
 void verifyShizuku()throws Exception{if(!Releases.signedBy(Releases.installed(this,Root.SHIZUKU),Releases.SHIZUKU_CERT))throw new IOException("未找到官方 Shizuku，请先一键部署");}
 void prepareShizuku()throws Exception{
  PackageInfo shizuku=Releases.installed(this,Root.SHIZUKU);
  boolean newerAndroid=android.os.Build.VERSION.SDK_INT>=35;
  if(shizuku==null||(newerAndroid&&shizuku.getLongVersionCode()<1086)){
   if(shizuku!=null)verifyShizuku();
   File dependency=Releases.download(this,"Shizuku",newerAndroid?Releases.SHIZUKU_NEW_URL:Releases.SHIZUKU_URL,newerAndroid?2571773:3442426,newerAndroid?Releases.SHIZUKU_NEW_SHA:Releases.SHIZUKU_SHA,this::report);
   Releases.install(this,dependency,Root.SHIZUKU,Releases.SHIZUKU_CERT,this::report);
  }else verifyShizuku();
 }
 void deploy()throws Exception{
  if(Root.AGENT.equals(Root.DUALSIM))throw new IOException("当前使用整合版，请安装对应网关更新；官方升级不会覆盖整合版");
  if(!android.os.Build.SUPPORTED_ABIS[0].equals("arm64-v8a"))throw new IOException("当前上游 APK 仅支持 ARM64 手机");
  report("正在获取 GitHub 最新正式版…");Releases.Release r=Releases.latest();getSharedPreferences("state",0).edit().putString("latest",r.version).apply();
  boolean fresh=Releases.installed(this,Root.AGENT)==null;
  File agent=Releases.download(this,"Voice-SIP-Agent",r.url,r.size,r.sha,this::report);
  // Validate the agent before installing its dependency.
  PackageInfo info=getPackageManager().getPackageArchiveInfo(agent.getAbsolutePath(),android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES);
  if(info==null||!info.packageName.equals(Root.AGENT)||!Releases.signedBy(info,Releases.VSA_CERT))throw new IOException("Voice SIP Agent 官方签名校验失败");
  prepareShizuku();
  Releases.install(this,agent,Root.AGENT,Releases.VSA_CERT,this::report);
  if(fresh){report("正在设置初始状态：关闭通话接管…");Root.control("off");Root.control("companion");Root.control("unmute");}
  if(Root.run("pidof shizuku_server || true",10).isEmpty()){report("正在以 Root 模式启动 Shizuku…");Root.startShizuku(this);}
  refresh();getSharedPreferences("state",0).edit().putString("verified",Releases.digest(agent)).apply();
  PackageInfo installedAgent=Releases.installed(this,Root.AGENT);
  report("已部署官方 "+(installedAgent==null?r.version:installedAgent.versionName)+"；请打开官方 App 授权 Shizuku 并填写 SIP 配置");
 }
 void refresh()throws Exception{
  String root=Root.run("printf 'dialer='; cmd role get-role-holders --user 0 android.app.role.DIALER; printf 'shizuku='; if pidof shizuku_server >/dev/null; then echo running; else echo stopped; fi; printf 'boot='; if [ -f "+Root.BASE+"/autostart ]; then echo on; else echo off; fi; printf 'compat='; if [ -f "+Root.BASE+"/mtk-compat-enabled ]; then echo on; else echo off; fi",15);
  String status=Releases.installed(this,Root.AGENT)==null?"未安装":Root.control("status");
  // Store only fixed status fields; do not persist raw upstream reports or call details.
  String safe="";
  for(String field:new String[]{"takeover=on","takeover=off","sip=ready","sip=down","audioReady=true","audioReady=false","ready=true","ready=false"," pending"})if(status.contains(field))safe+=field.trim()+" ";
  getSharedPreferences("state",0).edit().putString("rootStatus",root).putString("agentStatus",safe.trim()).putLong("checkedAt",System.currentTimeMillis()).apply();
  healthStatus();
 }
 void healthStatus()throws Exception{
  readHealthStatus(this);
 }
 static void readHealthStatus(android.content.Context context)throws Exception{
  String config=Root.run("if [ -f "+Root.BASE+"/health-enabled ]; then echo enabled; else echo disabled; fi; cat "+Root.BASE+"/health-minutes 2>/dev/null || echo 5; cat "+Root.BASE+"/health-status.json 2>/dev/null || true",8);
  String[] lines=config.split("\\n",3);int minutes=lines.length>1?HealthPolicy.minutes(lines[1].trim()):5;
  context.getSharedPreferences("state",0).edit().putBoolean("healthEnabled",lines[0].equals("enabled")).putInt("healthMinutes",minutes).putString("healthStatus",lines.length>2?lines[2]:"").apply();
 }
 public void onDestroy(){if(wake!=null&&wake.isHeld())wake.release();executor.shutdown();super.onDestroy();}
 public IBinder onBind(Intent intent){return null;}
}
