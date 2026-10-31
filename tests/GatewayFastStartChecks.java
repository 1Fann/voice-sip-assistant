package com.fann.voicesiphelper;

/** Exercises rejected inputs and races at the launch boundary, without Android or a live service. */
public final class GatewayFastStartChecks {
 static final String PACKAGE="com.fann.voicesip.dualsim";static int passed;
 static void require(boolean value){if(!value)throw new AssertionError();passed++;}
 static class Host implements GatewayFastStart.Host {
  boolean missing=true,monitor=true,background=true,takeover=true,pbx=true,wifi=true,helper=true,gateway=true,shizuku=true,server=true;
  Boolean idle=true;String change="";int starts,idleChecks;String order="";
  public boolean missing(int pid){order+="missing ";return missing;}
  public boolean authorized(){order+="authorized ";return monitor&&background&&takeover&&pbx&&wifi;}
  public boolean trusted(){order+="trusted ";return helper&&gateway&&shizuku;}
  public boolean shizukuRunning(){order+="server ";return server;}
  public Boolean idle(){order+="idle ";idleChecks++;switch(change){case "stop":monitor=false;break;case "off":takeover=false;break;case "pbx":pbx=false;break;case "signature":gateway=false;break;case "pid":missing=false;break;}return idle;}
  public void start(){order+="start ";starts++;}
 }
 static boolean attempt(Host h)throws Exception{return GatewayFastStart.attempt(PACKAGE,"1234",PACKAGE,h);}
 static void rejected(Host h)throws Exception{require(!attempt(h)&&h.starts==0);}
 public static void main(String[] args)throws Exception{
  Host ready=new Host();require(attempt(ready)&&ready.starts==1&&ready.idleChecks==1);
  require(ready.order.equals("missing authorized trusted server idle authorized missing trusted start "));
  for(String pid:new String[]{null,"","0","1","12 13","-2","2147483648","1234; id"}){Host h=new Host();require(!GatewayFastStart.attempt(PACKAGE,pid,PACKAGE,h)&&h.starts==0&&h.order.isEmpty());}
  Host mismatch=new Host();require(!GatewayFastStart.attempt("io.github.sparkcyf.voicesipagent","1234",PACKAGE,mismatch)&&mismatch.order.isEmpty());
  Host h=new Host();h.missing=false;rejected(h);
  h=new Host();h.monitor=false;rejected(h);h=new Host();h.background=false;rejected(h);
  h=new Host();h.takeover=false;rejected(h);h=new Host();h.pbx=false;rejected(h);h=new Host();h.wifi=false;rejected(h);
  h=new Host();h.helper=false;rejected(h);h=new Host();h.gateway=false;rejected(h);h=new Host();h.shizuku=false;rejected(h);
  h=new Host();h.server=false;rejected(h);h=new Host();h.idle=false;rejected(h);h=new Host();h.idle=null;rejected(h);
  for(String race:new String[]{"stop","off","pbx","signature","pid"}){h=new Host();h.change=race;rejected(h);require(h.idleChecks==1);}
  System.out.println("PASS: "+passed+" restricted gateway fast-start checks");
 }
}
