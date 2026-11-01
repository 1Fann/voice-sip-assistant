package com.fann.voicesiphelper;

import android.content.Context;
import android.os.Looper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Runs with the pinned helper as the ONLY APK on root's classpath. */
public final class DependencyUpdateMain {
 public static void main(String[] args){
  if(android.os.Process.myUid()!=0)throw new SecurityException();
  String outcome="failed";File staged=new File(Root.BASE,"shizuku-starter.update");
  try{
   Looper.prepareMainLooper();Class<?> at=Class.forName("android.app.ActivityThread");Object thread=at.getMethod("systemMain").invoke(null);
   Context system=(Context)at.getMethod("getSystemContext").invoke(thread);
   final Context c=system.createPackageContext("com.fann.voicesiphelper",Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
   if(!Releases.signedBy(Releases.installed(c,c.getPackageName()),Root.DUALSIM_CERT))throw new SecurityException();
   Root.selectAgent(c);
   final String[] path={""};
   outcome=DependencyHandoff.attempt(new DependencyHandoff.Host(){
    public boolean authorized()throws Exception{return HealthMain.enabled()&&new File(Root.BASE,"background-authorized").isFile()&&Boolean.TRUE.equals(HealthMain.takeover())&&HealthMain.pbxEnabled();}
    public boolean trusted()throws Exception{return Releases.signedBy(Releases.installed(c,Root.SHIZUKU),Releases.SHIZUKU_CERT);}
    public boolean idle()throws Exception{return HealthMain.idle();}
    public String fingerprint()throws Exception{
     String current=c.getPackageManager().getApplicationInfo(Root.SHIZUKU,0).sourceDir;
     if(!path[0].isEmpty()&&!path[0].equals(current))return "";
     path[0]=current;return Releases.digest(new File(current));
    }
    public void prepare()throws Exception{
     try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(path[0])){
      java.util.zip.ZipEntry entry=zip.getEntry("lib/arm64-v8a/libshizuku.so");
      if(entry==null||entry.getSize()<1||entry.getSize()>16777216)throw new IOException("unsupported_starter");
      try(InputStream in=zip.getInputStream(entry);FileOutputStream out=new FileOutputStream(staged)){
       byte[] buffer=new byte[8192];int n,total=0;
       while((n=in.read(buffer))!=-1){total+=n;if(total>16777216)throw new IOException("starter_too_large");out.write(buffer,0,n);}out.getFD().sync();
      }
     }
     android.system.Os.chmod(staged.getPath(),0700);
    }
    public void commit(String fingerprint)throws Exception{
     // Publish the starter first. A crash before the APK hash is committed leaves
     // the old hash gate closed; no changed APK is ever loaded by this process.
     if(!staged.renameTo(new File(Root.BASE,"shizuku_starter")))throw new IOException("starter_publish_failed");
     File tmp=new File(Root.BASE,"health-shizuku.sha256.update");
     try(FileOutputStream out=new FileOutputStream(tmp)){out.write((fingerprint+"\n").getBytes(StandardCharsets.US_ASCII));out.getFD().sync();}
     if(!tmp.renameTo(new File(Root.BASE,"health-shizuku.sha256")))throw new IOException("hash_publish_failed");
    }
   });
  }catch(Exception ignored){}finally{
   staged.delete();
   try{
    String message=outcome.equals("updated")?"已核验官方 Shizuku 更新并交接启动组件，下轮恢复检查":outcome.equals("busy")?"Shizuku 已更新，等待通话空闲后核验交接":outcome.equals("paused")?"Shizuku 已更新，网关停用或后台授权未完成，暂不交接":outcome.equals("untrusted")?"Shizuku 缺失或签名不符，恢复已暂停，请检查官方安装包":"Shizuku 更新核验未完成，未加载新组件，请打开助手检查";
    JSONObject j=new JSONObject();j.put("checkedAt",System.currentTimeMillis());j.put("phase","dependency_"+outcome);j.put("message",message);j.put("repaired",outcome.equals("updated"));
    File tmp=new File(Root.BASE,"health-status.tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(j.toString().getBytes(StandardCharsets.UTF_8));}
    tmp.renameTo(new File(Root.BASE,"health-status.json"));
   }catch(Exception ignored){}
  }
  System.exit(outcome.equals("updated")||outcome.equals("busy")||outcome.equals("changed")?75:0);
 }
}
