package com.fann.voicesiphelper.tests;

import android.app.Instrumentation;
import android.os.Bundle;
import android.content.Context;
import android.content.pm.PackageInfo;
import java.lang.reflect.*;

/** Negative tests exercise the release installer trust boundaries without installing anything. */
public class Checks extends Instrumentation {
 int passed;
 public void onCreate(Bundle args){super.onCreate(args);start();}
 void require(boolean condition,String label){if(!condition)throw new AssertionError(label);passed++;}
 Method method(Class<?> c,String name,Class<?>...types)throws Exception{Method m=c.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
 void rejects(Method method,Object...args)throws Exception{try{method.invoke(null,args);throw new AssertionError("Unsafe input accepted");}catch(InvocationTargetException e){require(e.getCause() instanceof java.io.IOException,"Expected controlled rejection");}}
 public void onStart(){Bundle out=new Bundle();try{
  Context c=getTargetContext();ClassLoader loader=c.getClassLoader();Class<?> releases=loader.loadClass("com.fann.voicesiphelper.Releases");Class<?> root=loader.loadClass("com.fann.voicesiphelper.Root");
  rejects(method(releases,"connection",String.class),"https://example.com/untrusted.apk");
  rejects(method(releases,"connection",String.class),"http://github.com/insecure.apk");
  rejects(method(root,"control",String.class),"provision --es password unsafe");
  String official="b8fe38aa02f9657c6785fb6faeb20abb20deba410f9c643fcd93269b642850c4";
  PackageInfo helper=c.getPackageManager().getPackageInfo(c.getPackageName(),android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES);
  require(!(Boolean)method(releases,"signedBy",PackageInfo.class,String.class).invoke(null,helper,official),"Wrong publisher certificate must be rejected");
  require(!(Boolean)method(releases,"signedBy",PackageInfo.class,String.class).invoke(null,null,official),"Missing APK must be rejected");
  PackageInfo agent=null;try{agent=c.getPackageManager().getPackageInfo("io.github.sparkcyf.voicesipagent",android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES);}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
  if(agent!=null)require((Boolean)method(releases,"signedBy",PackageInfo.class,String.class).invoke(null,agent,official),"Original upstream publisher must be accepted");
  require(android.os.Process.myUid()>=10000,"Helper must run as a normal application UID");
  method(root,"selectAgent",Context.class).invoke(null,c);
  Field selected=root.getDeclaredField("AGENT");selected.setAccessible(true);
  PackageInfo fork=null;try{fork=c.getPackageManager().getPackageInfo("com.fann.voicesip.dualsim",android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES);}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
  if(fork!=null){
   require("com.fann.voicesip.dualsim".equals(selected.get(null)),"Pinned local fork must be selected");
   require((Boolean)method(root,"trustedAgent",Context.class).invoke(null,c),"Selected local fork signature must be verified");
  }
  out.putString("result","PASS: "+passed+" installer boundary checks");finish(-1,out);
 }catch(Throwable e){out.putString("result","FAIL: "+e.getClass().getSimpleName()+" "+e.getMessage());finish(0,out);}}
}
