package com.aiaagent.app;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
public class AgentAccessibilityService extends AccessibilityService {
 public static AgentAccessibilityService instance;
 @Override public void onServiceConnected(){super.onServiceConnected();instance=this;}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){}
 @Override public void onInterrupt(){}
 @Override public void onDestroy(){instance=null;super.onDestroy();}
 public boolean doAction(String a){
  switch(a){
   case "back": return performGlobalAction(GLOBAL_ACTION_BACK);
   case "home": return performGlobalAction(GLOBAL_ACTION_HOME);
   case "recents": return performGlobalAction(GLOBAL_ACTION_RECENTS);
   case "notifications": return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
   case "quicksettings": return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
   default: return false;
  }
 }
}
