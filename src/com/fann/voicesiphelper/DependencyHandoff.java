package com.fann.voicesiphelper;

/** A changed APK is accepted only after independent identity and live-state checks. */
final class DependencyHandoff {
 interface Host {
  boolean authorized()throws Exception;
  boolean trusted()throws Exception;
  boolean idle()throws Exception;
  String fingerprint()throws Exception;
  void prepare()throws Exception;
  void commit(String fingerprint)throws Exception;
 }
 static String attempt(Host h)throws Exception{
  if(!h.trusted())return "untrusted";
  if(!h.authorized())return "paused";
  if(!h.idle())return "busy";
  String expected=h.fingerprint();
  if(expected==null||!expected.matches("[a-f0-9]{64}"))return "changed";
  h.prepare();
  if(!h.trusted()||!expected.equals(h.fingerprint()))return "changed";
  if(!h.authorized())return "paused";
  if(!h.idle())return "busy";
  h.commit(expected);return "updated";
 }
}
