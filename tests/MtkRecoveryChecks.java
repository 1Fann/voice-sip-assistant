package com.fann.voicesiphelper;

import java.io.IOException;

/** No Android or live tokens: state gates, process identity and request-window regression checks. */
public final class MtkRecoveryChecks {
 static int passed;
 static void require(boolean value){if(!value)throw new AssertionError();passed++;}
 interface Action{void run()throws Exception;}
 static void rejected(Action action)throws Exception{try{action.run();throw new AssertionError();}catch(IOException expected){passed++;}}
 static final String PKG="com.fann.voicesip.dualsim",APK="/data/app/current/base.apk";
 static String line(String time,int pid,String pkg,String token,String apk){return time+"  "+pid+"  5006 I UserServiceManager: New service record "+pkg+":vsa-privileged ("+token+"): version=123, daemon=false, apk="+apk;}
 public static void main(String[] args)throws Exception{
  String binding="ready=false takeover=on sip=down shizuku=BINDING audioReady=false calls=No cellular call";
  require(HealthPolicy.fastMtk(binding,true));require(!HealthPolicy.fastMtk(binding,false));
  for(String unsafe:new String[]{binding.replace("takeover=on","takeover=off"),binding+" pending",binding.replace("No cellular call","Unknown"),binding.replace("No cellular call","1 cellular call"),binding.replace("BINDING","PERMISSION_REQUIRED"),binding.replace("BINDING","PERMISSION_DENIED"),binding.replace("BINDING","SHIZUKU_NOT_RUNNING"),binding.replace("BINDING","STOPPED"),binding.replace("BINDING","CONNECTED")})require(!HealthPolicy.fastMtk(unsafe,true));
  ReadinessWaitChecks.Clock clock=new ReadinessWaitChecks.Clock();
  require(ReadinessWait.await(1500,()->binding,clock,s->HealthPolicy.healthy(s)||HealthPolicy.fastMtk(s,true)).equals(binding));require(clock.slept==0);
  require(MtkRequestToken.pid(" 4177\n")==4177);
  rejected(()->MtkRequestToken.pid("4177 5006"));rejected(()->MtkRequestToken.pid("0"));rejected(()->MtkRequestToken.pid("4177; id"));
  StringBuilder stat=new StringBuilder("42 (worker (odd) name) S");for(int field=4;field<22;field++)stat.append(" 0");stat.append(" 12345 0");
  require(MtkRequestToken.startTicks(stat.toString())==12345);rejected(()->MtkRequestToken.startTicks("42 (bad) S 0"));
  MtkRequestToken.sameTicks(12345,12345);passed++;rejected(()->MtkRequestToken.sameTicks(12345,12346));
  require(MtkRequestToken.startMillis(12345,100,2000000,500000)==1623450);
  rejected(()->MtkRequestToken.startMillis(12345,0,2000000,500000));rejected(()->MtkRequestToken.startMillis(12345,100,2000000,100));
  MtkRequestToken.Record current=MtkRequestToken.parse(line("1793407546.442",4177,PKG,"current",APK),PKG,4177);
  require(current!=null&&current.at==1793407546442L);
  require(MtkRequestToken.valid(current,1793407546400L,1793407546441L,1793407546500L,APK,123));
  require(!MtkRequestToken.valid(current,1793407546400L,1793407546443L,1793407546500L,APK,123));
  require(!MtkRequestToken.valid(current,1793407546400L,1793407546400L,1793407546441L,APK,123));
  require(!MtkRequestToken.valid(current,1793407546400L,1793407546400L,1793407546500L,APK,124));
  require(!MtkRequestToken.valid(current,1793407546400L,1793407546400L,1793407546500L,"/data/app/old/base.apk",123));
  require(MtkRequestToken.parse(line("1793407546.442",4178,PKG,"wrong-process",APK),PKG,4177)==null);
  require(MtkRequestToken.parse(line("1793407546.442",4177,PKG+".other","wrong-package",APK),PKG,4177)==null);
  require(MtkRequestToken.parse(line("1793407546.442999",4177,PKG,"microseconds",APK),PKG,4177).at==1793407546442L);
  MtkRequestToken.Record daemon=MtkRequestToken.parse(line("1793407546.442",4177,PKG,"warm",APK).replace("daemon=false","daemon=true"),PKG,4177);
  require(daemon!=null&&daemon.daemon);
  require(MtkRequestToken.valid(daemon,1793407546441L,1793407546441L,1793407546500L,APK,123));
  require(!MtkRequestToken.valid(daemon,1793407546443L,1793407546443L,1793407546500L,APK,123));
  require(!MtkRequestToken.valid(daemon,1793407546400L,1793407546400L,1793407546500L,APK,124));
  require(!MtkRequestToken.valid(daemon,1793407546400L,1793407546400L,1793407546500L,"/data/app/old/base.apk",123));
  require(!MtkRequestToken.valid(null,0,0,Long.MAX_VALUE,APK,123));
  // Cold boot: the pending daemon request survives replacement of its client process.
  long serverStart=1793407546000L,clientRestart=1793407546480L,now=1793407546500L;
  require(MtkRequestToken.valid(daemon,serverStart,clientRestart,now,APK,123));
  require(!MtkRequestToken.valid(current,serverStart,clientRestart,now,APK,123));
  require(!MtkRequestToken.valid(daemon,clientRestart,serverStart,now,APK,123));
  require(!MtkRequestToken.valid(current,clientRestart,serverStart,now,APK,123));
  require(!MtkRequestToken.valid(daemon,serverStart,clientRestart,now,APK,124));
  require(!MtkRequestToken.valid(daemon,serverStart,clientRestart,now,"/data/app/replaced/base.apk",123));
  require(!MtkRequestToken.valid(daemon,serverStart,clientRestart,daemon.at-1,APK,123));
  require(!MtkRequestToken.valid(daemon,0,clientRestart,now,APK,123));
  require(!MtkRequestToken.valid(daemon,serverStart,0,now,APK,123));
  require(MtkRequestToken.parse(line("1793407546.442",4176,PKG,"previous-server",APK).replace("daemon=false","daemon=true"),PKG,4177)==null);
  require(MtkRequestToken.valid(daemon,daemon.at,clientRestart,now,APK,123));
  require(!MtkRequestToken.valid(daemon,daemon.at+1,clientRestart,now,APK,123));
  System.out.println("PASS: "+passed+" MTK request and recovery policy checks");
 }
}
