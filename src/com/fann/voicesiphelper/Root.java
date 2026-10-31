package com.fann.voicesiphelper;

import android.content.Context;
import java.io.*;
import java.util.concurrent.TimeUnit;

final class Root {
 static long runtimeStamp(Context c){try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).lastUpdateTime;}catch(Exception e){return -1;}}
 static String busybox()throws Exception{
  String command="for p in /data/adb/magisk/busybox /data/adb/ksu/bin/busybox /data/adb/ap/bin/busybox; do if [ -x \"$p\" ]; then echo \"$p\"; exit 0; fi; done; exit 20";
  String path=run(command,8);
  if(!path.matches("/data/adb/(magisk|ksu/bin|ap/bin)/busybox"))throw new IOException("需要 Root 管理器提供 BusyBox；请检查 Magisk、KernelSU 或 APatch 安装");
  return path;
 }

 static final String BASE="/data/adb/voice-sip-assistant";
 static final String OFFICIAL="io.github.sparkcyf.voicesipagent";
 static final String DUALSIM="com.fann.voicesip.dualsim";
 static final String DUALSIM_CERT="4611bccce0a83124ffae63faf4ade682fa97ed398430c4558531115a20d2b6a8";
 static String AGENT=OFFICIAL;
 static final String SHIZUKU="moe.shizuku.privileged.api";
 static void selectAgent(Context context){try{AGENT=Releases.signedBy(Releases.installed(context,DUALSIM),DUALSIM_CERT)?DUALSIM:(Releases.installed(context,OFFICIAL)!=null?OFFICIAL:DUALSIM);}catch(Exception e){AGENT=OFFICIAL;}}
 static boolean trustedAgent(Context context)throws Exception{return Releases.signedBy(Releases.installed(context,AGENT),AGENT.equals(DUALSIM)?DUALSIM_CERT:Releases.VSA_CERT);}
 static synchronized String run(String command,int seconds)throws Exception{
  boolean root=android.os.Process.myUid()==0;
  Process p=(root?new ProcessBuilder("/system/bin/sh","-c",command):new ProcessBuilder("su","-M","-c",command)).redirectErrorStream(true).start();
  p.getOutputStream().close();ByteArrayOutputStream output=new ByteArrayOutputStream();
  Thread reader=new Thread(()->{try{byte[] b=new byte[4096];int n;while((n=p.getInputStream().read(b))!=-1)if(output.size()<131072)output.write(b,0,n);}catch(IOException ignored){}});reader.start();
  int code;
  if(root){
   // The root monitor runs several short guards in sequence. The exit signal
   // avoids Android's timed waitFor polling delay; stdout keeps its own reader.
   try{code=ProcessExit.await(p,seconds,TimeUnit.SECONDS);}catch(IOException e){reader.join(1000);throw e;}
  }else{
   if(!p.waitFor(seconds,TimeUnit.SECONDS)){p.destroyForcibly();reader.join(1000);throw new IOException("操作超时，请刷新状态后重试");}
   code=p.exitValue();
  }
  reader.join(2000);String result=output.toString("UTF-8").trim();
  if(code!=0)throw new IOException("Root 操作失败，请在手机的 Root 管理器中为 Voice SIP 助手授权；已授权时请查看系统安装提示");
  return result;
 }
 static String quote(String value){return "'"+value.replace("'","'\\''")+"'";}
 static String control(String operation)throws Exception{
  String args;
  switch(operation){
   case "status":args="--es op status";break;
   case "off":args="--es op takeover --ez on false";break;
   case "on":args="--es op takeover --ez on true";break;
   case "companion":args="--es op provision --ei call_access 1 --ei audio_method 2";break;
   case "reconnect":args="--es op sip-restart";break;
   case "setup":args="--es op setup";break;
   default:throw new IOException("无效控制操作");
  }
  String value=run("am broadcast --receiver-foreground -n "+AGENT+"/io.github.sparkcyf.voicesipagent.app.AdbControlReceiver "+args,20);
  if(!value.contains("result=1"))throw new IOException("网关尚未确认操作，请打开网关页面查看运行状态 后重试");
  return value;
 }
 static void stage(Context c)throws Exception{
  String installedMonitor=run("sha256sum "+BASE+"/health.sh 2>/dev/null | cut -d ' ' -f 1 || true",8);

  if(!Releases.signedBy(Releases.installed(c,"com.fann.voicesiphelper"),DUALSIM_CERT))throw new IOException("助手签名校验失败");
  String helperHash=Releases.digest(new File(c.getApplicationInfo().sourceDir));
  android.content.pm.PackageInfo dependency=Releases.installed(c,SHIZUKU);String shizukuHash="";
  if(dependency!=null){if(!Releases.signedBy(dependency,Releases.SHIZUKU_CERT))throw new IOException("Shizuku 签名校验失败");shizukuHash=Releases.digest(new File(c.getPackageManager().getApplicationInfo(SHIZUKU,0).sourceDir));}
  File dir=new File(c.getFilesDir(),"runtime");dir.mkdirs();
  for(String name:c.getAssets().list("runtime"))try(InputStream in=c.getAssets().open("runtime/"+name);OutputStream out=new FileOutputStream(new File(dir,name))){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
  // Publish each runtime file by rename: truncating a script currently being read
  // can terminate its monitor half-way through a refresh or Shizuku preparation.
  run("set -eu; umask 077; if [ -e "+BASE+" ] && [ ! -f "+BASE+"/.owned ]; then exit 21; fi; mkdir -p "+BASE+"; chmod 700 "+BASE+"; touch "+BASE+"/.owned; for name in boot.sh health.sh; do cp "+quote(dir.getAbsolutePath())+"/$name "+BASE+"/$name.new.$$; chmod 700 "+BASE+"/$name.new.$$; mv -f "+BASE+"/$name.new.$$ "+BASE+"/$name; done",15);
  run("set -eu; umask 077; printf '%s\\n' "+quote(helperHash)+" > "+BASE+"/health-helper.sha256; printf '%s\\n' "+quote(shizukuHash)+" > "+BASE+"/health-shizuku.sha256",10);
  String currentMonitor=run("sha256sum "+BASE+"/health.sh | cut -d ' ' -f 1",8);
  if(!installedMonitor.equals(currentMonitor)){
   run(busybox()+" setsid /system/bin/sh "+BASE+"/health.sh upgrade </dev/null >/dev/null 2>&1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- &",10);
  }
 }
 static void startShizuku(Context c)throws Exception{
  android.content.pm.ApplicationInfo info=c.getPackageManager().getApplicationInfo(SHIZUKU,0);
  // Hot recovery needs the existing server, not another extraction/runtime publication.
  // Keep the official package check before this early return, as stage() previously did.
  if(!Releases.signedBy(Releases.installed(c,SHIZUKU),Releases.SHIZUKU_CERT))throw new IOException("Shizuku 签名校验失败");
  if(!run("pidof shizuku_server || true",5).isEmpty())return;
  // Run the signed upstream starter in its supported root mode. No UID substitution.
  File extracted=new File(c.getFilesDir(),"shizuku-starter");
  try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(info.sourceDir)){
   java.util.zip.ZipEntry entry=zip.getEntry("lib/arm64-v8a/libshizuku.so");
   if(entry==null)throw new IOException("此 Shizuku 版本的启动格式已改变，请在 Shizuku 内手动启动");
   try(InputStream in=zip.getInputStream(entry);OutputStream out=new FileOutputStream(extracted)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
  }
  stage(c);
  run("set -eu; cp "+quote(extracted.getAbsolutePath())+" "+BASE+"/shizuku_starter; chmod 700 "+BASE+"/shizuku_starter; rm -f "+BASE+"/shizuku-starter",15);
  if(!run("pidof shizuku_server || true",5).isEmpty())return;
  try{run(BASE+"/shizuku_starter --apk="+quote(info.sourceDir)+" </dev/null >/dev/null 2>&1",30);}
  catch(IOException e){throw new IOException("Shizuku 启动失败，请打开 Shizuku 查看原因，或重试“启动 / 修复 Shizuku”");}
  boolean ready=false;
  for(int i=0;i<10;i++){Thread.sleep(500);if(!run("pidof shizuku_server || true",5).isEmpty()){ready=true;break;}}
  if(ready){Thread.sleep(4000);ready=!run("pidof shizuku_server || true",5).isEmpty();}
  if(!ready)throw new IOException("Shizuku 未持续运行，可能与当前系统不兼容；请打开 Shizuku 检查，或重新部署推荐依赖");
 }
 static void autostart(Context c,boolean enabled)throws Exception{
  stage(c);
  run(enabled?"touch "+BASE+"/autostart":"rm -f "+BASE+"/autostart",10);syncBoot();
 }
 static void syncBoot()throws Exception{
  run("set -eu; if [ -f "+BASE+"/autostart ] || [ -f "+BASE+"/health-enabled ]; then mkdir -p /data/adb/service.d; if [ -e /data/adb/service.d/96-voice-sip-assistant.sh ] && ! head -n 3 /data/adb/service.d/96-voice-sip-assistant.sh | grep -q voice-sip-assistant-owned; then exit 22; fi; cp "+BASE+"/boot.sh /data/adb/service.d/96-voice-sip-assistant.sh.new.$$; chmod 700 /data/adb/service.d/96-voice-sip-assistant.sh.new.$$; mv -f /data/adb/service.d/96-voice-sip-assistant.sh.new.$$ /data/adb/service.d/96-voice-sip-assistant.sh; elif [ -f /data/adb/service.d/96-voice-sip-assistant.sh ] && head -n 3 /data/adb/service.d/96-voice-sip-assistant.sh | grep -q voice-sip-assistant-owned; then rm -f /data/adb/service.d/96-voice-sip-assistant.sh; fi",15);
 }
 static void healthConfigure(Context c,boolean enabled,int minutes)throws Exception{
  HealthPolicy.minutes(Integer.toString(minutes));stage(c);
  run("set -eu; umask 077; printf '%s\\n' "+minutes+" > "+BASE+"/health-minutes.tmp; mv "+BASE+"/health-minutes.tmp "+BASE+"/health-minutes; "+(enabled?"touch "+BASE+"/health-enabled "+BASE+"/health-now":"rm -f "+BASE+"/health-enabled "+BASE+"/health-now"),10);
  syncBoot();if(enabled)healthLaunch();
 }
 static void healthLaunch()throws Exception{
  run(busybox()+" setsid /system/bin/sh "+BASE+"/health.sh loop </dev/null >/dev/null 2>&1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- &",10);
 }
 static void healthNow(Context c)throws Exception{
  stage(c);if(!run("test -f "+BASE+"/health-enabled && echo enabled || true",5).equals("enabled"))throw new IOException("请先开启自动检查与恢复");
  run("touch "+BASE+"/health-now",5);healthLaunch();
 }
 static boolean repairOrSchedule(Context c)throws Exception{
  // The root monitor holds the cross-app repair lock. Do not race it with a UI request.
  if(run("test -f "+BASE+"/health-enabled && echo enabled || true",5).equals("enabled")){
   healthNow(c);return false;
  }
  repairMtk(c,true);return true;
 }
 static void restoreDialer(Context context)throws Exception{
  control("off");boolean off=false;
  for(int i=0;i<6;i++){Thread.sleep(1000);String s=control("status");if(s.contains("takeover=off")&&!s.contains(" pending")){off=true;break;}}
  if(!off)throw new IOException("接管关闭尚未完成，可能仍有通话；请结束通话后再恢复系统电话");
  String pkg=run("cmd telecom get-system-dialer",10).trim();
  if(!pkg.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+"))throw new IOException("无法识别系统电话，请到系统默认应用设置选择电话 App");
  run("cmd telecom add-or-remove-call-companion-app "+AGENT+" 0 >/dev/null 2>&1 || true; cmd role add-role-holder --user 0 android.app.role.DIALER "+pkg,15);
  if(!pkg.equals(context.getSystemService(android.telecom.TelecomManager.class).getDefaultDialerPackage()))throw new IOException("系统未确认电话恢复，请到默认应用设置检查");
 }
 static void repairMtk(Context context,boolean enable)throws Exception{
  repairMtk(context,enable,false);
 }
 static void repairMtkFromMonitor(Context context,boolean enable)throws Exception{
  if(android.os.Process.myUid()!=0)throw new SecurityException();
  repairMtk(context,enable,true);
 }
 private static long processTicks(int pid,String name)throws Exception{
  if(android.os.Process.myUid()==0)return MtkRequestToken.processTicks(pid,name);
  String command=run("cat /proc/"+pid+"/cmdline",5);
  if(!command.equals(name)&&!command.startsWith(name+(char)0))throw new IOException("process_changed");
  return MtkRequestToken.startTicks(run("cat /proc/"+pid+"/stat",5));
 }
 private static void repairMtk(Context context,boolean enable,boolean prepared)throws Exception{
  if(!Releases.signedBy(Releases.installed(context,"com.fann.voicesiphelper"),DUALSIM_CERT)||!trustedAgent(context)||!Releases.signedBy(Releases.installed(context,SHIZUKU),Releases.SHIZUKU_CERT))throw new IOException("助手、网关或 Shizuku 签名未通过校验");
  boolean knownMtk=new File(BASE,"mtk-compat-enabled").isFile();
  // The root monitor validated protected APK hashes before loading this code.
  if(!prepared||!new File(BASE,".owned").isFile())stage(context);
  if(enable)run("touch "+BASE+"/mtk-compat-enabled",10);
  String state=control("status");
  if(state.contains("shizuku=CONNECTED"))return;
  if(!HealthPolicy.bindingAllowed(state))return;
  if(!HealthPolicy.fastMtk(state,knownMtk))Thread.sleep(800);
  state=control("status");
  // A normal bind, a new call or a manual/permission change may win the race.
  if(state.contains("shizuku=CONNECTED")||!HealthPolicy.bindingAllowed(state))return;
  String helper=context.getApplicationInfo().sourceDir;
  String shizuku=context.getPackageManager().getApplicationInfo(SHIZUKU,0).sourceDir;
  try{
   int serverPid=MtkRequestToken.pid(run("pidof shizuku_server",5)),gatewayPid=MtkRequestToken.pid(run("pidof "+AGENT,5));
   long serverTicks=processTicks(serverPid,"shizuku_server"),gatewayTicks=processTicks(gatewayPid,AGENT);
   long after=MtkRequestToken.startMillis(gatewayTicks,android.system.Os.sysconf(android.system.OsConstants._SC_CLK_TCK),System.currentTimeMillis(),android.os.SystemClock.elapsedRealtime());
   MtkRequestToken.sameTicks(serverTicks,processTicks(serverPid,"shizuku_server"));MtkRequestToken.sameTicks(gatewayTicks,processTicks(gatewayPid,AGENT));
   run(busybox()+" setsid env CLASSPATH="+quote(helper+":"+shizuku)+" app_process /system/bin --nice-name=vsa_mtk_service_root com.fann.voicesiphelper.MtkServiceBootstrap "+quote(AGENT)+" "+serverPid+" "+serverTicks+" "+gatewayPid+" "+gatewayTicks+" "+after+" "+(prepared?"monitor":"manual")+" </dev/null >"+BASE+"/compat-status.log 2>&1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- &",10);
  }catch(IOException ignored){
   // Unknown identity must not attach an old token or immediately restart the shared server.
   // Retain the existing bounded opportunity for the normal connection below.
  }
  for(int attempt=0;attempt<12;attempt++){Thread.sleep(800);state=control("status");if(state.contains("shizuku=CONNECTED")||!HealthPolicy.bindingAllowed(state))return;}
  throw new IOException("兼容服务未连接，请确认 Shizuku 仍在运行并已授权网关");
 }
}
