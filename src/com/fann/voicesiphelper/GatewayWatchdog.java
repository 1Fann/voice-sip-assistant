package com.fann.voicesiphelper;

/** Recovery of an explicitly acknowledged, repeatedly unresponsive runtime. */
final class GatewayWatchdog {
 static final long WINDOW=30000, SAMPLE=5000, COOLDOWN=300000;
 String identity="";long first,last,nextAllowed;int failures;
 interface Host {
  boolean authorized()throws Exception;
  boolean sameProcess()throws Exception;
  boolean trusted()throws Exception;
  boolean idle()throws Exception;
  void persist()throws Exception;
  void restart()throws Exception;
 }
 void clear(){identity="";first=last=0;failures=0;}
 boolean timeout(String key,long now){
  if(key==null||key.isEmpty()||now<0){clear();return false;}
  if(!identity.equals(key)||now<last||now-last>120000){clear();identity=key;first=last=now;failures=1;return false;}
  if(now-last>=SAMPLE){failures=Math.min(3,failures+1);last=now;}
  return failures>=3&&now-first>=WINDOW&&now>=nextAllowed;
 }
 boolean attempt(long now,Host host)throws Exception{
  if(failures<3||now-first<WINDOW||now<nextAllowed)return false;
  if(!host.authorized()||!host.sameProcess()||!host.trusted()||!host.idle())return false;
  nextAllowed=now+COOLDOWN;host.persist();
  // Re-read consent, process identity and live call state at the mutation boundary.
  if(!host.authorized()||!host.trusted()||!host.sameProcess()||!host.idle())return false;
  host.restart();clear();host.persist();return true;
 }
}
