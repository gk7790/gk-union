package com.gk.ledger.support;
public final class LedgerCacheKeys {
 public static String subjectDisplay(Long t,String s,Long id){return "ledger:subject-display:"+t+":"+s+":"+id;}
 public static String tenantDict(String s,boolean include){return "sys:tenant:dict:"+s+":"+include;} public static String tenantDictPattern(){return "sys:tenant:dict:*";} private LedgerCacheKeys(){}
}
