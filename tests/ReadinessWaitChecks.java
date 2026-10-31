package com.fann.voicesiphelper;

/** Fake-clock checks: early success, all readiness fields, deadline and read failures. */
public final class ReadinessWaitChecks {
 static final String READY="ready=true shizuku=CONNECTED audioReady=true sip=ready";
 static final class Clock implements ReadinessWait.Clock {
  long now,slept;public long now(){return now;}public void sleep(long millis){now+=millis;slept+=millis;}
 }
 static void require(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  Clock clock=new Clock();require(ReadinessWait.await(1500,()->READY,clock).equals(READY));require(clock.slept==0);
  Clock delayed=new Clock();int[] calls={0};
  require(ReadinessWait.await(1500,()->++calls[0]<3?"ready=false shizuku=CONNECTED audioReady=true sip=down":READY,delayed).equals(READY));
  require(delayed.slept==500&&calls[0]==3);
  Clock never=new Clock();int[] attempts={0};
  String incomplete="ready=true shizuku=CONNECTED audioReady=false sip=ready";
  require(ReadinessWait.await(1500,()->{attempts[0]++;return incomplete;},never).equals(incomplete));
  require(never.now==1500&&attempts[0]==6);
  Clock slow=new Clock();int[] slowReads={0};
  ReadinessWait.await(1500,()->{slowReads[0]++;slow.now+=2000;return incomplete;},slow);
  require(slowReads[0]==1&&slow.slept==0);
  try{ReadinessWait.await(1500,()->{throw new java.io.IOException("unknown");},new Clock());throw new AssertionError();}
  catch(java.io.IOException expected){}
  System.out.println("PASS: 5 startup-readiness polling scenarios");
 }
}
