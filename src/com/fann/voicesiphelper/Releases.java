package com.fann.voicesiphelper;

import android.content.Context;
import android.content.pm.*;
import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;

final class Releases {
 static final String VSA_CERT="b8fe38aa02f9657c6785fb6faeb20abb20deba410f9c643fcd93269b642850c4";
 static final String SHIZUKU_CERT="268b5590e868fb08bae7e0ac413564cd1ff88f5ccff74af9dbd0dc918e30db30";
 static final String SHIZUKU_SHA="a05832ce3716afb1fcccf46f348006d2a296ca777e1ff3d223797dc74d06b31f";
 static final String SHIZUKU_URL="https://github.com/RikkaApps/Shizuku/releases/download/v13.5.4/shizuku-v13.5.4.r1049.0e53409-release.apk";
 static final String SHIZUKU_NEW_SHA="6e273ab0e991c4e79bc8b1bbb9b9dd739ccac1a8712a541a214078886b7b790f";
 static final String SHIZUKU_NEW_URL="https://github.com/RikkaApps/Shizuku/releases/download/v13.6.0/shizuku-v13.6.0.r1086.2650830c-release.apk";
 interface Progress {void report(String message);}
 static final class Release {String version,url,sha;long size;}
 static Release latest()throws Exception{
  HttpsURLConnection c=connection("https://api.github.com/repos/sparkcyf/android-voice-sip-agent/releases/latest");
  try{
   if(c.getResponseCode()!=200)throw new IOException("无法获取 GitHub 最新版，请检查网络或稍后重试");
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>262144)throw new IOException("版本信息超出预期");out.write(b,0,n);}}
   JSONObject json=new JSONObject(out.toString("UTF-8"));
   if(json.optBoolean("draft")||json.optBoolean("prerelease"))throw new IOException("没有可用的正式版本");
   String version=json.getString("tag_name");if(!version.matches("v[0-9]+\\.[0-9]+\\.[0-9]+"))throw new IOException("上游版本格式已改变，请更新助手");
   JSONArray assets=json.getJSONArray("assets");Release release=null;
   for(int i=0;i<assets.length();i++){
    JSONObject asset=assets.getJSONObject(i);
    if(!asset.getString("name").equals("voice-sip-agent-"+version.substring(1)+".apk"))continue;
    if(release!=null)throw new IOException("上游存在多个同名安装包，请稍后重试");
    release=new Release();release.version=version;release.url=asset.getString("browser_download_url");release.size=asset.getLong("size");
    if(!release.url.equals("https://github.com/sparkcyf/android-voice-sip-agent/releases/download/"+version+"/voice-sip-agent-"+version.substring(1)+".apk"))throw new IOException("下载地址未通过来源检查");
    String digest=asset.optString("digest","");release.sha=digest.matches("sha256:[a-fA-F0-9]{64}")?digest.substring(7).toLowerCase(java.util.Locale.ROOT):"";
    if(release.size<100000||release.size>83886080)throw new IOException("安装包大小异常");
   }
   if(release==null)throw new IOException("最新版暂无官方 APK，请稍后重试");return release;
  }finally{c.disconnect();}
 }
 static HttpsURLConnection connection(String url)throws Exception{
  URL u=new URL(url);
  for(int i=0;i<6;i++){
   String host=u.getHost();
   if(!u.getProtocol().equals("https")||!(host.equals("api.github.com")||host.equals("github.com")||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com")))throw new IOException("下载重定向未通过来源检查");
   HttpsURLConnection c=(HttpsURLConnection)u.openConnection();c.setConnectTimeout(20000);c.setReadTimeout(45000);c.setRequestProperty("User-Agent","Voice-SIP-Assistant/1.0.0");c.setInstanceFollowRedirects(false);
   int code=c.getResponseCode();
   if(code==301||code==302||code==303||code==307||code==308){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("下载重定向无效");u=new URL(u,location);continue;}
   return c;
  }
  throw new IOException("下载重定向过多");
 }
 static File download(Context context,String label,String url,long expectedSize,String sha,Progress progress)throws Exception{
  File target=new File(context.getCacheDir(),label+".apk");
  if(target.exists()&&!sha.isEmpty()&&digest(target).equals(sha)&&(expectedSize==0||target.length()==expectedSize)){progress.report(label+" 已下载，正在复核签名");return target;}
  File part=new File(context.getCacheDir(),label+".part");
  try{
   HttpsURLConnection c=connection(url);
   try{
    if(c.getResponseCode()!=200)throw new IOException("下载失败，请确认手机能够访问 GitHub 后重试");
    long size=c.getContentLengthLong(),total=0;int last=-1;
    if(size>83886080)throw new IOException("下载文件过大");
    try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(part)){
     byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){total+=n;if(total>83886080||(expectedSize>0&&total>expectedSize))throw new IOException("安装包大小不符");out.write(b,0,n);
      int percent=expectedSize>0?(int)(100*total/expectedSize):size>0?(int)(100*total/size):-1;
      int step=percent<0?(int)(total/1048576):percent/5;if(step!=last){last=step;progress.report("下载 "+label+" "+(percent<0?(total/1048576)+" MB":percent+"%"));}
     }
    }
    if((expectedSize>0&&total!=expectedSize)||(size>0&&total!=size))throw new IOException("下载中断，请重试");
   }finally{c.disconnect();}
   if(!sha.isEmpty()&&!digest(part).equals(sha))throw new IOException("安装包 SHA-256 校验失败，已停止安装");
   if(target.exists()&&!target.delete())throw new IOException("无法替换缓存");
   if(!part.renameTo(target))throw new IOException("无法保存安装包");return target;
  }finally{if(part.exists())part.delete();}
 }
 static String digest(File file)throws Exception{
  MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}return hex(digest.digest());
 }
 static String hex(byte[] data){StringBuilder s=new StringBuilder();for(byte b:data)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
 static PackageInfo installed(Context context,String pkg){try{return context.getPackageManager().getPackageInfo(pkg,PackageManager.GET_SIGNING_CERTIFICATES);}catch(PackageManager.NameNotFoundException e){return null;}}
 static boolean signedBy(PackageInfo info,String cert)throws Exception{
  if(info==null||info.signingInfo==null||info.signingInfo.hasMultipleSigners())return false;
  for(Signature s:info.signingInfo.getApkContentsSigners())if(hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())).equals(cert))return true;
  return false;
 }
 static void install(Context c,File file,String pkg,String cert,Progress progress)throws Exception{
  PackageInfo archive=c.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
  if(archive==null||!archive.packageName.equals(pkg)||!signedBy(archive,cert))throw new IOException("安装包身份或官方签名校验失败，已停止安装");
  if(pkg.equals(Root.AGENT)&&archive.applicationInfo.minSdkVersion>android.os.Build.VERSION.SDK_INT)throw new IOException("最新版已不支持当前安卓版本");
  PackageInfo current=installed(c,pkg);
  if(current!=null){
   if(!signedBy(current,cert))throw new IOException("已安装同名 App 的签名不同，请自行备份配置后处理；助手不会卸载它");
   if(current.getLongVersionCode()>archive.getLongVersionCode()||(current.getLongVersionCode()==archive.getLongVersionCode()&&java.util.Objects.equals(current.versionName,archive.versionName))){progress.report("已安装相同或更高版本，保留原配置");return;}
  }
  progress.report("正在安装已校验的官方 APK…");
  String temp="/data/local/tmp/voice-sip-assistant-"+android.os.Process.myUid()+"-"+System.nanoTime()+".apk";
  String q=Root.quote(temp);
  String result=Root.run("set -eu; trap 'rm -f "+temp+"' EXIT; cp "+Root.quote(file.getAbsolutePath())+" "+q+"; chmod 644 "+q+"; pm install -r "+q,120);
  if(!result.contains("Success"))throw new IOException("系统未确认安装成功，请检查 USB 安装限制和剩余存储空间");
  PackageInfo now=installed(c,pkg);
  if(now==null||now.getLongVersionCode()<archive.getLongVersionCode()||!java.util.Objects.equals(now.versionName,archive.versionName)||!signedBy(now,cert))throw new IOException("安装后校验未通过，请刷新状态");
 }
}
