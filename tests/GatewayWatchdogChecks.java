package com.fann.voicesiphelper;
public final class GatewayWatchdogChecks {
 static int passed;
 static void ok(boolean value){if(!value)throw new AssertionError();passed++;}
 static GatewayWatchdog armed(){GatewayWatchdog s=new GatewayWatchdog();ok(!s.timeout("boot:package:10:100",1000));ok(!s.timeout("boot:package:10:100",10000));ok(s.timeout("boot:package:10:100",31000));return s;}
 static final class H implements GatewayWatchdog.Host {
  boolean allowed=true,same=true,trusted=true,idle=true,changeOnSave,failSave;int saves,restarts;
  public boolean authorized(){return allowed;}public boolean sameProcess(){return same;}public boolean trusted(){return trusted;}public boolean idle(){return idle;}
  public void persist()throws Exception{saves++;if(failSave)throw new java.io.IOException();if(changeOnSave)same=false;}
  public void restart(){restarts++;}
 }
 public static void main(String[] args)throws Exception{
  GatewayWatchdog s=armed();H h=new H();ok(s.attempt(31000,h));ok(h.restarts==1);ok(s.failures==0);
  s.timeout("new:11:200",32000);s.timeout("new:11:200",42000);ok(!s.timeout("new:11:200",62000));ok(!s.attempt(62000,h));
  ok(s.timeout("new:11:200",332000)==false); // Long observation gap re-arms, never spends stale evidence.
  s=armed();ok(!s.timeout("same-pid-new-start",32000));ok(s.failures==1);
  s=armed();ok(!s.timeout("boot:package:10:100",1));ok(s.failures==1);
  for(int i=0;i<4;i++){s=armed();h=new H();if(i==0)h.allowed=false;if(i==1)h.same=false;if(i==2)h.trusted=false;if(i==3)h.idle=false;ok(!s.attempt(31000,h));ok(h.restarts==0);}
  s=armed();h=new H();h.changeOnSave=true;ok(!s.attempt(31000,h));ok(h.restarts==0);
  s=armed();h=new H();h.failSave=true;try{s.attempt(31000,h);throw new AssertionError();}catch(java.io.IOException expected){ok(h.restarts==0);}
  s=armed();s.clear();h=new H();ok(!s.attempt(31000,h));
  s=new GatewayWatchdog();s.timeout("same",1000);s.timeout("same",1001);s.timeout("same",1002);ok(s.failures==1);
  System.out.println("PASS: "+passed+" gateway watchdog checks (stalls, process replacement, idle/consent/trust, cooldown and persistence failure)");
 }
}
