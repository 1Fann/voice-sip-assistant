package com.fann.voicesiphelper;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.*;

/** Pure parsing plus read-only process identity checks. Tokens are never logged. */
final class MtkRequestToken {
 private static final Pattern RECORD=Pattern.compile("^\\s*([0-9]+\\.[0-9]+)\\s+([0-9]+)\\s+[0-9]+\\s+[VDIWEF]\\s+UserServiceManager\\s*:\\s*New service record ([^ :]+):vsa-privileged \\(([^)]+)\\): version=([0-9]+), daemon=(true|false), apk=(.+)$");
 static final class Record {
  final long at,version;final String token,apk;final boolean daemon;
  Record(long at,long version,String token,String apk,boolean daemon){this.at=at;this.version=version;this.token=token;this.apk=apk;this.daemon=daemon;}
 }
 static int pid(String output)throws IOException{
  if(output==null||!output.trim().matches("[1-9][0-9]*"))throw new IOException("ambiguous_process");
  try{return Integer.parseInt(output.trim());}catch(NumberFormatException e){throw new IOException("invalid_process",e);}
 }
 static long startTicks(String stat)throws IOException{
  int end=stat.lastIndexOf(')');if(end<0)throw new IOException("process_stat_missing");
  String[] fields=stat.substring(end+1).trim().split("\\s+");
  if(fields.length<20)throw new IOException("process_stat_short");
  try{long value=Long.parseLong(fields[19]);if(value<=0)throw new NumberFormatException();return value;}
  catch(NumberFormatException e){throw new IOException("invalid_process_start",e);}
 }
 static long processTicks(int pid,String name)throws IOException{
  byte[] command=Files.readAllBytes(new File("/proc/"+pid+"/cmdline").toPath());int end=0;
  while(end<command.length&&command[end]!=0)end++;
  if(!name.equals(new String(command,0,end,StandardCharsets.UTF_8)))throw new IOException("process_changed");
  return startTicks(new String(Files.readAllBytes(new File("/proc/"+pid+"/stat").toPath()),StandardCharsets.US_ASCII));
 }
 static void sameProcess(int pid,String name,long ticks)throws IOException{
  sameTicks(ticks,processTicks(pid,name));
 }
 static void sameTicks(long expected,long actual)throws IOException{if(expected<=0||actual!=expected)throw new IOException("process_changed");}
 static long startMillis(long ticks,long hz,long wall,long elapsed)throws IOException{
  if(ticks<=0||hz<=0||wall<=0||elapsed<0||ticks>Long.MAX_VALUE/1000)throw new IOException("invalid_process_clock");
  long sinceBoot=ticks*1000/hz;if(sinceBoot>elapsed)throw new IOException("invalid_process_clock");
  return wall-elapsed+sinceBoot;
 }
 static Record parse(String line,String pkg,int serverPid){
  Matcher m=RECORD.matcher(line);if(!m.matches()||!m.group(2).equals(Integer.toString(serverPid))||!m.group(3).equals(pkg))return null;
  try{return new Record(new BigDecimal(m.group(1)).movePointRight(3).setScale(0,RoundingMode.FLOOR).longValueExact(),Long.parseLong(m.group(5)),m.group(4),m.group(7).trim(),Boolean.parseBoolean(m.group(6)));}
  catch(ArithmeticException|NumberFormatException e){return null;}
 }
 static boolean valid(Record record,long serverAfter,long gatewayAfter,long now,String apk,long version){
  if(record==null||serverAfter<=0||gatewayAfter<=0)return false;
  // A daemon's pending authorized record survives its client process. It must still
  // belong to this live server and installed APK, but can predate the current client.
  // Non-daemon requests retain the stricter client-generation boundary.
  long after=record.daemon?serverAfter:Math.max(serverAfter,gatewayAfter);
  return record.at>=after&&record.at<=now&&record.apk.equals(apk)&&record.version==version;
 }
}
