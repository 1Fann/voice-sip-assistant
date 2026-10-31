package com.fann.voicesiphelper;

/** Poll only within the existing startup grace period; never interprets partial readiness as success. */
final class ReadinessWait {
 interface Probe { String read()throws Exception; }
 interface Decision { boolean proceed(String status); }
 interface Clock { long now();void sleep(long millis)throws Exception; }
 static String await(long budgetMillis,Probe probe)throws Exception{
  return await(budgetMillis,probe,HealthPolicy::healthy);
 }
 static String await(long budgetMillis,Probe probe,Decision decision)throws Exception{
  return await(budgetMillis,probe,new Clock(){
   public long now(){return System.nanoTime()/1000000;}
   public void sleep(long millis)throws Exception{Thread.sleep(millis);}
  },decision);
 }
 static String await(long budgetMillis,Probe probe,Clock clock)throws Exception{
  return await(budgetMillis,probe,clock,HealthPolicy::healthy);
 }
 static String await(long budgetMillis,Probe probe,Clock clock,Decision decision)throws Exception{
  long deadline=clock.now()+budgetMillis;String status;
  do{
   status=probe.read(); // Uses the same bounded status operation as the old post-sleep check.
   if(decision.proceed(status))return status;
   long left=deadline-clock.now();if(left<=0)return status;
   clock.sleep(Math.min(250,left));
  }while(clock.now()<deadline);
  return status;
 }
}
