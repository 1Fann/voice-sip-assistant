package com.fann.voicesiphelper;

import android.content.Context;
import android.os.Looper;
import android.util.Xml;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

/** A bounded check, started by the root-owned monitor, independent of the helper UI process. */
public final class HealthMain {
 static final String PBX="/data/adb/asterisk-app";
 static String message="检查尚未完成",phase="initialize",error="";static boolean repaired,retrySoon,fastStarted;
 static String observedPackage="",observedPid="";
 static Context context;
 static final File base=new File(Root.BASE);
 public static void main(String[] args){
  if(android.os.Process.myUid()!=0)throw new SecurityException();
  if(args.length==2){observedPackage=args[0];observedPid=args[1];}
  try{
   Looper.prepareMainLooper();Class<?> at=Class.forName("android.app.ActivityThread");Object thread=at.getMethod("systemMain").invoke(null);
   Context system=(Context)at.getMethod("getSystemContext").invoke(thread);
   context=system.createPackageContext("com.fann.voicesiphelper",Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
   if(!Releases.signedBy(Releases.installed(context,context.getPackageName()),Root.DUALSIM_CERT))throw new IOException("助手签名校验失败");
   if(!new File(base,"health-enabled").isFile())return;
   phase="check";check();
  }catch(Exception e){retrySoon=enabled()&&!"paused_or_busy".equals(e.getMessage());error=e.getClass().getSimpleName();if(e.getStackTrace().length>0){StackTraceElement f=e.getStackTrace()[0];error+=":"+f.getMethodName()+":"+f.getLineNumber();}message="检查或恢复未完成，请查看权限与网络后重试";}
  finally{save();if(retrySoon)System.exit(75);}
 }
 static boolean enabled(){return new File(base,"health-enabled").isFile();}
 static boolean wifi()throws Exception{
  // The root checker can inspect the interface directly, without starting a shell
  // twice on every guarded recovery. Re-read at each guard; never cache an address.
  NetworkInterface network=NetworkInterface.getByName("wlan0");
  if(network==null)return false;
  Enumeration<InetAddress> addresses=network.getInetAddresses();
  while(addresses.hasMoreElements())if(addresses.nextElement() instanceof Inet4Address)return true;
  return false;
 }
 static Boolean takeover()throws Exception {
  File f=new File("/data/user/0/"+Root.AGENT+"/shared_prefs/gateway.xml");if(!f.isFile())return null;
  // Read only the switch. Never deserialize, log or export account/password fields.
  XmlPullParser p=Xml.newPullParser();try(InputStream in=new FileInputStream(f)){
   p.setInput(in,"UTF-8");for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next())
    if(event==XmlPullParser.START_TAG&&"boolean".equals(p.getName())&&"takeover".equals(p.getAttributeValue(null,"name")))return "true".equals(p.getAttributeValue(null,"value"));
  }return null;
 }
 static boolean pbxEnabled(){return new File(PBX,"installed").isFile()&&!new File(PBX,"disabled").exists();}
 static boolean idle()throws Exception {
  boolean server=pbxEnabled();String channels="";
  if(server){
   String state=Root.run(PBX+"/control.sh state",10);
   if(state.equals("running"))channels=Root.run(PBX+"/control.sh cli core show channels count",10);
   else channels="0 active calls\n";
  }
  return HealthPolicy.idle(Root.run("dumpsys telephony.registry",12),channels,server);
 }
 static void guard()throws Exception{if(!enabled()||!idle())throw new IOException("paused_or_busy");}
 static String status()throws Exception{return Root.control("status");}
 static void check()throws Exception {
  Root.selectAgent(context);
  // Share only the selected package name with the cheap root PID probe, never credentials.
  rememberAgent();
  fastStarted=tryFastStart();
  if(!idle()){message="正在通话或无法确认通话状态，本轮跳过恢复";return;}
  if(!wifi()){retrySoon=true;message="等待 Wi-Fi，本轮不重启服务";return;}
  boolean wantedPbx=pbxEnabled();
  // Read-only dependency check. Asterisk Assistant alone owns server recovery.
  if(new File(PBX,"installed").isFile()){
   if(!wantedPbx){message="服务器已手动停止，暂停恢复网关";return;}
   if(!Root.run(PBX+"/control.sh state",10).equals("running")){
    retrySoon=true;message="等待 Asterisk 助手恢复服务器";return;
   }
   JSONObject pbx=new JSONObject(Root.run(PBX+"/control.sh status",15));
   if(!sipAlive(pbx.getJSONObject("ports").getInt("localPort"))){
    retrySoon=true;message="服务器尚未响应，等待 Asterisk 助手恢复";return;
   }
  }
  if(!Root.trustedAgent(context)){message="网关缺失或签名不符，未启动";return;}
  Boolean wanted=takeover();
  if(!Boolean.TRUE.equals(wanted)){message="手机通话网关已停用，尊重手动停用";return;}
  if(!Releases.signedBy(Releases.installed(context,Root.SHIZUKU),Releases.SHIZUKU_CERT)){message="需要安装或授权官方 Shizuku";return;}
  // An ordered status broadcast alone cannot wake every force-stopped ROM package.
  // Explicitly start its existing foreground service, without opening an activity.
  if(!BackgroundAccess.repairAuthorized(context)){message="后台运行受限，请打开助手允许后台运行与自启动";return;}
  phase="gateway_start";
  String gatewayOwner=Root.run("dumpsys activity services "+Root.AGENT,10);
  String s;
  if(!gatewayOwner.contains("isForeground=true")){
   guard();if(!Boolean.TRUE.equals(takeover())){message="接管已手动关闭，取消恢复";return;}
   Root.startShizuku(context);startGateway();repaired=true;s=awaitGateway();
  }else s=status();
  phase="gateway_status";
  if(permissionMissing(s)){message="需要在网关内手动允许 Shizuku 授权";return;}
  if(s.contains("phoneAccess=blocked")&&s.contains("shizuku=CONNECTED")){message="电话权限或 SIM 信息未就绪，请打开网关完成权限引导；MIUI 请关闭空白通行证。本轮不重启服务";return;}
  if(finishIfHealthy(s,wantedPbx))return;
  // A known MTK workaround can attach as soon as the normal authorized request exists.
  // Starting the already-running foreground service again cannot create a fresher bind.
  if(!fastMtk(s)){
   guard();phase="prepare_shizuku";Root.startShizuku(context);
   startGateway();s=awaitGateway();
   if(permissionMissing(s)){message="需要在网关内手动允许 Shizuku 授权";return;}
   if(finishIfHealthy(s,wantedPbx))return;
  }
  boolean bindingRecovery=!s.contains("shizuku=CONNECTED");
  if(bindingRecovery){
   guard();phase="repair_binding";try{Root.repairMtkFromMonitor(context,true);repaired=true;}catch(IOException ignored){}
   s=status();
   if(permissionMissing(s)){message="需要在网关内手动允许 Shizuku 授权";return;}
   if(!s.contains("takeover=on")||s.contains(" pending")){message="网关正在停用，取消恢复";return;}
   if(!s.contains("shizuku=CONNECTED")){
    // An old service record can survive a swipe-force-stop. A fresh server + app request is required.
    guard();if(!Boolean.TRUE.equals(takeover())){message="接管已手动关闭，取消恢复";return;}
    phase="fresh_binding";restartShizuku();
    // Shizuku startup itself can start the client's provider. Recreate the client AFTER
    // the new server is ready. On this ROM its activity transition delivers the binder.
    guard();Root.run("am force-stop "+Root.AGENT,12);
    Root.run("am start -n "+Root.AGENT+"/io.github.sparkcyf.voicesipagent.app.MainActivity",15);Thread.sleep(2000);
    Root.repairMtkFromMonitor(context,true);
   }
  }
  // CONNECTED triggers setup and SIP registration asynchronously in the gateway.
  // Observe normal readiness before considering a restart, without the BINDING fast predicate.
  if(bindingRecovery&&finishIfHealthy(ReadinessWait.await(1500,HealthMain::status),wantedPbx))return;
  guard();if(!Boolean.TRUE.equals(takeover())){message="接管已手动关闭，取消恢复";return;}
  // Binding/setup also registers SIP itself. Never tear down a registration that has recovered.
  if(finishIfHealthy(status(),wantedPbx))return;
  phase="sip_reconnect";Root.control("reconnect");repaired=true;Thread.sleep(2500);s=status();
  boolean contact=!wantedPbx||gatewayRegistered();
  retrySoon=!(foreground()&&HealthPolicy.healthy(s)&&contact);
  message=!retrySoon?"已恢复权限、音频与 SIP 连接":"恢复尚未完成，将在下轮重试";
 }
 static boolean permissionMissing(String status){return status.contains("PERMISSION_REQUIRED")||status.contains("PERMISSION_DENIED");}
 static boolean tryFastStart(){try{
  return GatewayFastStart.attempt(observedPackage,observedPid,Root.AGENT,new GatewayFastStart.Host(){
   public boolean missing(int oldPid)throws Exception{
    // An EPERM/unknown result is not evidence of death. Reused PIDs also reject the fast path.
    try{android.system.Os.kill(oldPid,0);return false;}
    catch(android.system.ErrnoException e){return e.errno==android.system.OsConstants.ESRCH
      &&!new File("/proc/"+oldPid).exists()&&new File("/proc/1/stat").canRead();}
   }
   public boolean authorized()throws Exception{return enabled()&&new File(base,"background-authorized").isFile()&&Boolean.TRUE.equals(takeover())&&pbxEnabled()&&wifi();}
   public boolean trusted()throws Exception{return Releases.signedBy(Releases.installed(context,context.getPackageName()),Root.DUALSIM_CERT)&&Root.trustedAgent(context)&&Releases.signedBy(Releases.installed(context,Root.SHIZUKU),Releases.SHIZUKU_CERT);}
   public boolean shizukuRunning()throws Exception{return Root.run("pidof shizuku_server || true",5).matches("[1-9][0-9]*");}
   public Boolean idle()throws Exception{return HealthMain.idle();}
   public void start()throws Exception{phase="fast_gateway_start";startGateway();repaired=true;}
  });
 }catch(Exception ignored){return false;}}
 static boolean fastMtk(String status){return HealthPolicy.fastMtk(status,new File(base,"mtk-compat-enabled").isFile());}
 static String awaitGateway()throws Exception{return ReadinessWait.await(1500,HealthMain::status,s->HealthPolicy.healthy(s)||fastMtk(s));}
 static boolean finishIfHealthy(String status,boolean wantedPbx)throws Exception{
  if(!HealthPolicy.healthy(status)||!foreground()||(wantedPbx&&!gatewayRegistered()))return false;
  message=repaired?"已恢复网关，权限与 SIP 连接正常":"网关权限、音频与 SIP 注册正常";return true;
 }
 static void rememberAgent(){try{
  File target=new File(base,"health-agent");byte[] value=Root.AGENT.getBytes(StandardCharsets.US_ASCII);
  if(target.isFile())try(FileInputStream in=new FileInputStream(target)){
   byte[] previous=new byte[128];int count=in.read(previous);
   if(count==value.length&&Root.AGENT.equals(new String(previous,0,count,StandardCharsets.US_ASCII)))return;
  }
  File temp=new File(base,"health-agent.tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(value);}
  if(!temp.renameTo(target))temp.delete();
 }catch(IOException ignored){}} // A cache failure must not suppress the actual health check.
 static boolean foreground()throws Exception{return Root.run("dumpsys activity services "+Root.AGENT,10).contains("isForeground=true");}
 static void startGateway()throws Exception {
  Root.run("am start-foreground-service --user 0 -n "+Root.AGENT+"/io.github.sparkcyf.voicesipagent.app.GatewayService",15);
 }
 static void restartShizuku()throws Exception {
  guard();String ids=Root.run("pidof shizuku_server || true",5);
  for(String id:ids.split("\\s+"))if(id.matches("[0-9]+")){
   // ART may rename the Linux main thread to "main"; identify the process by argv[0].
   String command=Root.run("cat /proc/"+id+"/cmdline",5);
   if(!command.equals("shizuku_server")&&!command.startsWith("shizuku_server"+(char)0))throw new IOException("process_changed");
   guard();Root.run("kill -TERM "+id,5);
  }
  Thread.sleep(1200);Root.startShizuku(context);Thread.sleep(1000);
 }
 static boolean gatewayRegistered()throws Exception {
  JSONObject s=new JSONObject(Root.run(PBX+"/control.sh status",15));
  org.json.JSONArray a=s.optJSONArray("accounts");if(a==null)return false;
  for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);if("gateway".equals(p.optString("role"))&&p.optBoolean("registered"))return true;}
  return false;
 }
 static boolean sipAlive(int port){
  if(port<1024||port>65535)return false;
  try(DatagramSocket socket=new DatagramSocket()){
   socket.setSoTimeout(2000);String id=UUID.randomUUID().toString();
   String request="OPTIONS sip:health@127.0.0.1:"+port+" SIP/2.0\r\nVia: SIP/2.0/UDP 127.0.0.1:"+socket.getLocalPort()+";branch=z9hG4bK"+id+";rport\r\nFrom: <sip:health@localhost>;tag="+id+"\r\nTo: <sip:health@localhost>\r\nCall-ID: "+id+"\r\nCSeq: 1 OPTIONS\r\nMax-Forwards: 0\r\nContent-Length: 0\r\n\r\n";
   byte[] b=request.getBytes(StandardCharsets.US_ASCII);socket.send(new DatagramPacket(b,b.length,InetAddress.getByName("127.0.0.1"),port));
   byte[] out=new byte[8192];DatagramPacket packet=new DatagramPacket(out,out.length);socket.receive(packet);
   String reply=new String(out,0,packet.getLength(),StandardCharsets.US_ASCII);return reply.startsWith("SIP/2.0 ")&&reply.contains("Call-ID: "+id);
  }catch(Exception e){return false;}
 }
 static void save(){try{
  JSONObject result=new JSONObject();result.put("checkedAt",System.currentTimeMillis());result.put("message",message);result.put("repaired",repaired);result.put("phase",phase);result.put("error",error);result.put("fastStarted",fastStarted);
  File temp=new File(base,"health-status.tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(result.toString().getBytes(StandardCharsets.UTF_8));}
  if(!temp.renameTo(new File(base,"health-status.json")))temp.delete();
 }catch(Exception ignored){}}
}
