package com.fann.voicesiphelper;
public final class DependencyHandoffChecks {
 static int passed;static void ok(boolean v){if(!v)throw new AssertionError();passed++;}
 static final class H implements DependencyHandoff.Host {
  boolean authorized=true,trusted=true,idle=true,change,stop,busy;int prepared,committed;
  public boolean authorized(){return authorized;}public boolean trusted(){return trusted;}public boolean idle(){return idle;}
  public String fingerprint(){return change&&prepared>0?"changed":"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";}
  public void prepare(){prepared++;if(stop)authorized=false;if(busy)idle=false;}
  public void commit(String s){committed++;}
 }
 public static void main(String[] args)throws Exception{
  H h=new H();ok(DependencyHandoff.attempt(h).equals("updated"));ok(h.committed==1);
  for(int i=0;i<6;i++){
   h=new H();if(i==0)h.trusted=false;if(i==1)h.authorized=false;if(i==2)h.idle=false;if(i==3)h.change=true;if(i==4)h.stop=true;if(i==5)h.busy=true;
   ok(!DependencyHandoff.attempt(h).equals("updated"));ok(h.committed==0);
  }
  System.out.println("PASS: "+passed+" dependency handoff checks (signature, update race, stop and call guards)");
 }
}
