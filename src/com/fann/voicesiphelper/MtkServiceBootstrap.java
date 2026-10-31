package com.fann.voicesiphelper;
import android.content.Context;
import android.os.IBinder;
import android.os.Looper;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Root-only fallback for the Shizuku 13.6 makeApplication failure on this MediaTek ROM. */
public final class MtkServiceBootstrap {
 static void identity(String pkg,int serverPid,long serverTicks,int gatewayPid,long gatewayTicks)throws IOException{
  MtkRequestToken.sameProcess(serverPid,"shizuku_server",serverTicks);
  MtkRequestToken.sameProcess(gatewayPid,pkg,gatewayTicks);
 }
 static MtkRequestToken.Record latest(String pkg,int serverPid,long budget)throws Exception{
  Process logs=new ProcessBuilder("logcat","-d","-v","epoch","-s","UserServiceManager").redirectErrorStream(true).start();
  AtomicReference<MtkRequestToken.Record> latest=new AtomicReference<>();
  Thread reader=new Thread(()->{
   try(BufferedReader in=new BufferedReader(new InputStreamReader(logs.getInputStream()))){
    String line;while((line=in.readLine())!=null){MtkRequestToken.Record record=MtkRequestToken.parse(line,pkg,serverPid);if(record!=null)latest.set(record);}
   }catch(IOException ignored){}
  });reader.setDaemon(true);reader.start();
  try{
   if(!logs.waitFor(Math.max(1,budget),TimeUnit.MILLISECONDS))return null;
   reader.join(100);return logs.exitValue()==0&&!reader.isAlive()?latest.get():null;
  }finally{logs.destroyForcibly();try{logs.getInputStream().close();}catch(IOException ignored){}}
 }
 public static void main(String[] args){
  try{
   if(android.os.Process.myUid()!=0||args.length!=7||(!args[6].equals("monitor")&&!args[6].equals("manual")))throw new SecurityException();
   String pkg=args[0];if(!pkg.equals(Root.OFFICIAL)&&!pkg.equals(Root.DUALSIM))throw new SecurityException();
   int serverPid=MtkRequestToken.pid(args[1]),gatewayPid=MtkRequestToken.pid(args[3]);
   long serverTicks=Long.parseLong(args[2]),gatewayTicks=Long.parseLong(args[4]),gatewayAfter=Long.parseLong(args[5]);
   identity(pkg,serverPid,serverTicks,gatewayPid,gatewayTicks);
   long hz=android.system.Os.sysconf(android.system.OsConstants._SC_CLK_TCK),wall=System.currentTimeMillis(),elapsed=android.os.SystemClock.elapsedRealtime();
   long computedAfter=MtkRequestToken.startMillis(gatewayTicks,hz,wall,elapsed);
   long serverAfter=MtkRequestToken.startMillis(serverTicks,hz,wall,elapsed);
   if(Math.abs(computedAfter-gatewayAfter)>250)throw new IOException("process_clock_changed");
   Looper.prepareMainLooper();
   Class<?> at=Class.forName("android.app.ActivityThread");Object thread=at.getMethod("systemMain").invoke(null);
   Context system=(Context)at.getMethod("getSystemContext").invoke(thread);
   Root.AGENT=pkg;
   if(!Root.trustedAgent(system)||!Releases.signedBy(Releases.installed(system,Root.SHIZUKU),Releases.SHIZUKU_CERT))throw new SecurityException();
   Context context=system.createPackageContext(pkg,Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
   android.content.pm.PackageInfo info=context.getPackageManager().getPackageInfo(pkg,0);
   String apk=context.getApplicationInfo().sourceDir;long version=info.lastUpdateTime/1000%Integer.MAX_VALUE;
   long deadline=android.os.SystemClock.elapsedRealtime()+1500;MtkRequestToken.Record request=null;
   do{
    identity(pkg,serverPid,serverTicks,gatewayPid,gatewayTicks);
    long remaining=deadline-android.os.SystemClock.elapsedRealtime();if(remaining<=0)break;
    // Android logcat -d waits about 500 ms before completing even a small dump.
    // A 400 ms cap killed every valid read on the tested MIUI device.
    MtkRequestToken.Record observed=latest(pkg,serverPid,Math.min(1500,remaining));
    if(MtkRequestToken.valid(observed,serverAfter,gatewayAfter,System.currentTimeMillis(),apk,version)){request=observed;break;}
    remaining=deadline-android.os.SystemClock.elapsedRealtime();if(remaining>0)Thread.sleep(Math.min(100,remaining));
   }while(android.os.SystemClock.elapsedRealtime()<deadline);
   if(request==null)throw new IOException("request_missing");
   Class<?> serviceClass=context.getClassLoader().loadClass("io.github.sparkcyf.voicesipagent.privileged.shell.PrivilegedService");
   IBinder service=(IBinder)serviceClass.getConstructor(Context.class).newInstance(context);
   Class<?> starter=Class.forName("moe.shizuku.starter.ServiceStarter");Method send=null;
   for(Method method:starter.getDeclaredMethods()){
    Class<?>[] p=method.getParameterTypes();
    if(p.length==3&&p[0]==IBinder.class&&p[1]==String.class&&p[2]==boolean.class&&method.getReturnType()==boolean.class){send=method;break;}
   }
   if(send==null)throw new IOException("upstream_api_changed");send.setAccessible(true);
   identity(pkg,serverPid,serverTicks,gatewayPid,gatewayTicks);
   // The normal service can win while this process loads classes. Do not replace it.
   String state=Root.control("status");
   if(state.contains("shizuku=CONNECTED")){System.out.println("compatibility_service_already_connected");return;}
   if(!HealthPolicy.bindingAllowed(state)||!HealthMain.idle())throw new IOException("binding_no_longer_safe");
   identity(pkg,serverPid,serverTicks,gatewayPid,gatewayTicks);
   // Class loading can race a bind retry. Only the server's latest record may attach.
   MtkRequestToken.Record current=latest(pkg,serverPid,1500);
   if(!MtkRequestToken.valid(current,serverAfter,gatewayAfter,System.currentTimeMillis(),apk,version)||!current.token.equals(request.token))throw new IOException("request_changed");
   identity(pkg,serverPid,serverTicks,gatewayPid,gatewayTicks);
   if(args[6].equals("monitor")&&!HealthMain.enabled())throw new IOException("monitor_stopped");
   if(!(Boolean)send.invoke(null,service,request.token,true))throw new IOException("attach_failed");
   System.out.println("compatibility_service_attached");
   Looper.loop();
  }catch(Throwable e){System.out.println("compatibility_service_failed:"+e.getClass().getSimpleName());android.util.Log.e("VsaMtkBootstrap","compatibility startup failed",e);System.exit(1);}
 }
}
