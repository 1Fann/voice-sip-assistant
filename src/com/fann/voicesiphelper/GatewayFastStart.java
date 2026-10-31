package com.fann.voicesiphelper;

/** Only an observed old gateway PID loss can enter this preflight; no Android dependencies. */
final class GatewayFastStart {
 interface Host {
  boolean missing(int oldPid)throws Exception;
  boolean authorized()throws Exception;
  boolean trusted()throws Exception;
  boolean shizukuRunning()throws Exception;
  Boolean idle()throws Exception;
  void start()throws Exception;
 }
 static boolean attempt(String observedPackage,String oldPid,String selectedPackage,Host host)throws Exception{
  if(!("com.fann.voicesip.dualsim".equals(selectedPackage)||"io.github.sparkcyf.voicesipagent".equals(selectedPackage))
    ||!selectedPackage.equals(observedPackage)||oldPid==null||!oldPid.matches("[1-9][0-9]{0,9}"))return false;
  final int pid;try{pid=Integer.parseInt(oldPid);}catch(NumberFormatException e){return false;}
  if(pid<=1||!host.missing(pid)||!host.authorized()||!host.trusted()||!host.shizukuRunning())return false;
  // This is the full live cellular/PBX guard, not a cached no-call observation.
  if(!Boolean.TRUE.equals(host.idle()))return false;
  // Stop/config/package/PID changes can race the live call check.
  if(!host.authorized()||!host.missing(pid)||!host.trusted())return false;
  host.start();return true;
 }
}
