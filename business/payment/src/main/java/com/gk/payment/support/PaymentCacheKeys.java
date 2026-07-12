package com.gk.payment.support;
import com.gk.common.tools.StringFormat;
public final class PaymentCacheKeys {
 public static String methodDict(String c,String u,String d,String m,String s){return "payment:method:dict:"+all(c)+":"+all(u)+":"+all(d)+":"+all(m)+":"+all(s);} public static String methodDictPattern(){return "payment:method:dict:*";}
 public static String activePlan(Long t,Long m,Long a,String d,String c,String u,String method){return "payment:plan:active:"+StringFormat.join(":",all(t),all(m),all(a),all(d),all(c),all(u),all(method));} public static String activePlanPattern(){return "payment:plan:active:*";}
 public static String pspDisabled(Long t,String d,Long id){return "payment:psp:disable:"+all(t)+":"+all(d)+":"+all(id);} public static String accountDisabled(Long t,String d,Long id){return "payment:psp-account:disable:"+all(t)+":"+all(d)+":"+all(id);}
 public static String pspHealth(Long t,String d,Long id){return "payment:psp:health:"+all(t)+":"+all(d)+":"+all(id);} public static String accountHealth(Long t,String d,Long id){return "payment:psp-account:health:"+all(t)+":"+all(d)+":"+all(id);}
 private static String all(Object v){return v==null?"*":String.valueOf(v);} private PaymentCacheKeys(){}
}
