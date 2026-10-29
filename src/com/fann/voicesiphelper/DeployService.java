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
   Releases.Release r=Releases.latest();getSharedPreferences("state",0).edit().putString("latest",r.version).apply();report("整合网关最新正式版："+r.version);return;
  }
  if(!Root.run("id -u",15).trim().equals("0"))throw new IOException("请在手机的 Root 管理器中为 Voice SIP 助手授权");
  switch(action){
   case "DEPLOY":deploy();break;
   case "SHIZUKU":
    prepareShizuku();Root.startShizuku(this);refresh();report("Shizuku 已启动；未授权时请在网关中点“允许 Shizuku 授权”");break;
   case "REFRESH":Root.stage(this);refresh();report("状态已刷新");break;
   case "OFF":Root.control("off");refresh();report("停用手机通话网关请求已提交；通话中会等挂断后生效");break;
   case "ON":
    prepareShizuku();Root.startShizuku(this);Root.run("am start -n "+Root.AGENT+"/io.github.sparkcyf.voicesipagent.app.MainActivity",15);Root.control("companion");Root.control("on");Root.repairOrSchedule(this);refresh();report("手机通话网关已请求启用，正在检查依赖；请等权限、音频与 SIP 均就绪再测试通话");break;
   case "HEALTH_NOW":Root.healthNow(this);refresh();report("已提交立即检查，请稍后查看自动恢复状态");break;
   case "HEALTH_STATUS":healthStatus();break;
   case "REPAIR_BINDING":
    if(!Root.control("status").contains("takeover=on")){report("网关已手动停用，不执行恢复");break;}
    verifyShizuku();boolean done=Root.repairOrSchedule(this);refresh();report(done?"权限服务检查完成，请核对网关状态":"已提交权限服务恢复，正在等待独立检查完成");break;
   case "UPDATE_RUNTIME":
    Root.stage(this);Root.syncBoot();if(Root.run("test -f "+Root.BASE+"/health-enabled && echo yes || true",5).equals("yes"))Root.healthLaunch();getSharedPreferences("state",0).edit().putLong("runtimeStamp",Root.runtimeStamp(this)).apply();report("运行组件已随 APK 更新");break;
   case "MTK_FIX":
    verifyShizuku();Root.startShizuku(this);Root.run("am start -n "+Root.AGENT+"/io.github.sparkcyf.voicesipagent.app.MainActivity",15);Root.repairOrSchedule(this);refresh();report("权限服务恢复已请求，请等待网关显示连接就绪");break;
   case "RECONNECT":Root.control("reconnect");refresh();report("重连请求已提交，请刷新查看注册状态");break;
   case "RESTORE":Root.restoreDialer(this);refresh();report("手机通话网关已停用，系统默认电话已恢复；重新启用网关后可继续使用 SIP 通话");break;
   case "BOOT_ON":prepareShizuku();Root.startShizuku(this);Root.autostart(this,true);refresh();report("已开启：开机应用数据可用后准备 Shizuku；无密码时通常无需手动解锁，不启用已手动停用的网关");break;
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
  if(Root.AGENT.equals(Root.OFFICIAL))throw new IOException("检测到上游官方版，请先停用并备份其配置；本助手不会安装第二个同时运行的网关");
  Root.AGENT=Root.DUALSIM;
  if(!android.os.Build.SUPPORTED_ABIS[0].equals("arm64-v8a"))throw new IOException("当前上游 APK 仅支持 ARM64 手机");
  report("正在获取 GitHub 最新正式版…");Releases.Release r=Releases.latest();getSharedPreferences("state",0).edit().putString("latest",r.version).apply();
  boolean fresh=Releases.installed(this,Root.AGENT)==null;
  File agent=Releases.download(this,"Voice-SIP-Agent",r.url,r.size,r.sha,this::report);
  // Validate the agent before installing its dependency.
  PackageInfo info=getPackageManager().getPackageArchiveInfo(agent.getAbsolutePath(),android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES);
  if(info==null||!info.packageName.equals(Root.AGENT)||!Releases.signedBy(info,Root.DUALSIM_CERT))throw new IOException("Voice SIP Agent 整合网关签名校验失败");
  prepareShizuku();
  Releases.install(this,agent,Root.AGENT,Root.DUALSIM_CERT,this::report);
  Root.stage(this);
  if(fresh){report("正在设置初始状态：暂不启用手机通话网关，保留本机静音设置，等待账号与权限配置…");Root.control("off");Root.control("companion");}
  if(Root.run("pidof shizuku_server || true",10).isEmpty()){report("正在以 Root 模式启动 Shizuku…");Root.startShizuku(this);}
  Root.startShizuku(this);
  if(fresh){Root.autostart(this,true);Root.healthConfigure(this,true,5);}
  Root.run("am start -n "+Root.AGENT+"/io.github.sparkcyf.voicesipagent.app.MainActivity",15);
  refresh();getSharedPreferences("state",0).edit().putString("verified",Releases.digest(agent)).apply();
  PackageInfo installedAgent=Releases.installed(this,Root.AGENT);
  report("已部署整合网关 "+(installedAgent==null?r.version:installedAgent.versionName)+"；请打开整合网关 App 授权 Shizuku 并填写 SIP 配置；首次权限向导会检查电话信息，完成后启用网关");
 }
 void refresh()throws Exception{
  String dialer=getSystemService(android.telecom.TelecomManager.class).getDefaultDialerPackage();
  String root="dialer="+(dialer==null?"未设置":dialer)+"\n"+Root.run("printf 'shizuku='; if pidof shizuku_server >/dev/null; then echo running; else echo stopped; fi; printf 'boot='; if [ -f "+Root.BASE+"/autostart ]; then echo on; else echo off; fi; printf 'compat='; if [ -f "+Root.BASE+"/mtk-compat-enabled ]; then echo on; else echo off; fi",15);
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
