package com.fann.voicesiphelper;
import android.content.Context;
import android.os.IBinder;
import android.os.Looper;
import java.io.*;
import java.lang.reflect.*;
import java.util.regex.*;

/** Root-only fallback for the Shizuku 13.6 makeApplication failure on this MediaTek ROM. */
public final class MtkServiceBootstrap {
 public static void main(String[] args){
  try{
   if(android.os.Process.myUid()!=0)throw new SecurityException();
   String pkg=args.length==0?"io.github.sparkcyf.voicesipagent":args[0];
   if(!pkg.equals("io.github.sparkcyf.voicesipagent")&&!pkg.equals("com.fann.voicesip.dualsim"))throw new SecurityException();
   Process logs=new ProcessBuilder("logcat","-d","-s","UserServiceManager").start();
   String token=null;Pattern pattern=Pattern.compile("New service record "+Pattern.quote(pkg)+":vsa-privileged \\(([^)]+)\\)");
   try(BufferedReader in=new BufferedReader(new InputStreamReader(logs.getInputStream()))){String line;while((line=in.readLine())!=null){Matcher m=pattern.matcher(line);if(m.find())token=m.group(1);}}
   logs.waitFor();if(token==null)throw new IOException("request_missing");
   Looper.prepareMainLooper();
   Class<?> at=Class.forName("android.app.ActivityThread");Object thread=at.getMethod("systemMain").invoke(null);
   Context system=(Context)at.getMethod("getSystemContext").invoke(thread);
   Context context=system.createPackageContext(pkg,Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
   Class<?> serviceClass=context.getClassLoader().loadClass("io.github.sparkcyf.voicesipagent.privileged.shell.PrivilegedService");
   IBinder service=(IBinder)serviceClass.getConstructor(Context.class).newInstance(context);
   Class<?> starter=Class.forName("moe.shizuku.starter.ServiceStarter");Method send=null;
   for(Method method:starter.getDeclaredMethods()){
    Class<?>[] p=method.getParameterTypes();
    if(p.length==3&&p[0]==IBinder.class&&p[1]==String.class&&p[2]==boolean.class&&method.getReturnType()==boolean.class){send=method;break;}
   }
   if(send==null)throw new IOException("upstream_api_changed");send.setAccessible(true);
   if(!(Boolean)send.invoke(null,service,token,true))throw new IOException("attach_failed");
   System.out.println("compatibility_service_attached");
   Looper.loop();
  }catch(Throwable e){System.out.println("compatibility_service_failed:"+e.getClass().getSimpleName());android.util.Log.e("VsaMtkBootstrap","compatibility startup failed",e);System.exit(1);}
 }
}
