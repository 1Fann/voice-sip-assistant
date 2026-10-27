package com.fann.voicesiphelper;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;

public class MainActivity extends Activity {
 final Handler handler=new Handler(Looper.getMainLooper());
 final int ink=Color.rgb(27,47,51),muted=Color.rgb(91,112,118),teal=Color.rgb(13,102,101);
 LinearLayout body;TextView versions,status,progress,healthStatus;Switch boot,health;EditText healthMinutes;boolean updating;volatile boolean readingHealth;int ticks;
 final ArrayList<Button> operations=new ArrayList<>();
 final Runnable tick=new Runnable(){public void run(){update();handler.postDelayed(this,1500);}};
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);return t;}
 void add(LinearLayout box,View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);box.addView(v,p);}
 LinearLayout card(String title,String desc){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(20),dp(20),dp(20),dp(20));c.setBackground(shape(Color.WHITE,20));add(body,c,14);TextView t=text(title,19,ink);t.setTypeface(null,Typeface.BOLD);c.addView(t);if(!desc.isEmpty())add(c,text(desc,14,muted),7);return c;}
 Button button(LinearLayout c,String label,boolean primary,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setMinHeight(dp(49));b.setPadding(dp(10),dp(6),dp(10),dp(6));b.setTextColor(primary?Color.WHITE:teal);b.setBackground(shape(primary?teal:Color.rgb(230,244,240),12));add(c,b,11);b.setOnClickListener(v->action.run());operations.add(b);return b;}
 void confirm(String title,String message,Runnable action){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("取消",null).setPositiveButton("继续",(d,w)->action.run()).show();}
 public void onCreate(Bundle state){
  Root.selectAgent(this);
  super.onCreate(state);getWindow().setStatusBarColor(Color.rgb(242,247,246));getWindow().setNavigationBarColor(Color.rgb(242,247,246));getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(242,247,246));body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(18),dp(22),dp(18),dp(30));scroll.addView(body);setContentView(scroll);
  TextView title=text("Voice SIP 助手",29,ink);title.setTypeface(null,Typeface.BOLD);body.addView(title);add(body,text("管理通话网关 · 默认卡与指定 SIM 呼出",15,muted),6);
  LinearLayout stateCard=card("当前状态","");versions=text("正在读取版本…",15,ink);stateCard.addView(versions);status=text("点击刷新状态并授权 Root",14,muted);add(stateCard,status,12);progress=text("就绪",14,teal);add(stateCard,progress,14);
  LinearLayout monitor=card("网关自动检查与恢复","检查 Shizuku、网关音频就绪和 SIP 注册，异常时自动恢复；服务器由 Asterisk 助手独立检查与恢复。清理助手后台仍有效，开机首次解锁后恢复检查。通话时跳过修复，保留静音，尊重手动停用；严重权限异常时可能打开网关页面。深度休眠可能延后检查。");
  health=new Switch(this);health.setText("开启自动检查与恢复");health.setTextSize(16);add(monitor,health,12);
  health.setOnCheckedChangeListener((b,on)->{if(updating)return;updating=true;health.setChecked(!on);updating=false;saveHealth(on);});
  add(monitor,text("检查间隔（分钟，1–1440）",14,ink),12);
  healthMinutes=new EditText(this);healthMinutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);healthMinutes.setSingleLine(true);healthMinutes.setText(Integer.toString(getSharedPreferences("state",0).getInt("healthMinutes",5)));add(monitor,healthMinutes,4);
  button(monitor,"保存检查间隔",true,()->saveHealth(getSharedPreferences("state",0).getBoolean("healthEnabled",false)));
  button(monitor,"立即检查并恢复",false,()->start("HEALTH_NOW"));
  healthStatus=text("尚未检查",14,muted);add(monitor,healthStatus,12);
  LinearLayout deploy=card("一键部署","从官方 GitHub 获取最新正式版，并校验安装包与签名。首次安装关闭接管，保留系统电话。需要在 BakaSU 为本助手授予 Root。");
  button(deploy,Root.AGENT.equals(Root.DUALSIM)?"官方版部署（整合版需单独更新）":"一键部署最新版",true,()->confirm("部署官方 Voice SIP Agent","将安装或升级官方 APK，并准备 Shizuku。已有配置会保留；升级可能短暂中断服务，请先结束通话。首次使用仍需在官方 App 中允许 Shizuku 并填写 SIP 账号。",()->start("DEPLOY")));
  button(deploy,"检查官方最新版本",false,()->start("CHECK"));
  LinearLayout control=card("可视化管理","SIP 账号、密码和音频参数在官方界面管理，本助手不保存这些内容。");
  Button open=button(control,"打开 Voice SIP 网关 / 配置",true,()->open(Root.AGENT));operations.remove(open);
  button(control,"刷新状态 / Root 授权",false,()->start("REFRESH"));
  button(control,"启动 / 修复 Shizuku",false,()->start("SHIZUKU"));
  button(control,"修复联发科 Shizuku 连接",false,()->confirm("修复权限服务连接","适用于 Shizuku 已授权但官方 App 一直显示 BINDING 的联发科系统。使用官方已授权的服务请求启动 Root 兼容入口，不修改官方 APK。修复可能重新连接网关，请先结束通话。",()->start("MTK_FIX")));
  Button shi=button(control,"打开 Shizuku 授权管理",false,()->open(Root.SHIZUKU));operations.remove(shi);
  button(control,"重新连接 SIP",false,()->start("RECONNECT"));
  LinearLayout calls=card("通话接管","伴侣模式保留系统电话界面。官方版仍把蜂窝来电送给服务器，后续行为取决于服务器路由。小米互联共存需要实测；未修改官方版的来电逻辑。");
  button(calls,"关闭通话接管",false,()->start("OFF"));
  button(calls,"启用伴侣模式接管",false,()->confirm("启用通话接管","将设置伴侣模式、直接电话音频，再启用接管；保留你选择的本机通话静音设置。蜂窝来电也会转发到 SIP，可能影响小米互联。请先在官方 App 完成 Shizuku 授权和 SIP 配置。",()->start("ON")));
  button(calls,"关闭接管并恢复系统电话",false,()->confirm("恢复系统电话","先关闭 Voice SIP Agent 接管，等待通话结束，再恢复系统电话。仅调整 Voice SIP Agent 的伴侣登记和默认电话。小米互联是否恢复需要实际来电验证。",()->start("RESTORE")));
  LinearLayout startup=card("开机准备","此独立选项只准备 Shizuku，不开启通话接管。上方自动恢复开启时，会另行在开机解锁后恢复检查并准备所需服务。");
  boot=new Switch(this);boot.setText("开机准备 Shizuku");boot.setTextSize(15);add(startup,boot,12);boot.setOnCheckedChangeListener((b,on)->{if(updating)return;updating=true;boot.setChecked(!on);updating=false;start(on?"BOOT_ON":"BOOT_OFF");});
  LinearLayout about=card("来源与说明","Voice SIP 助手 1.3.1 是独立管理工具，与上游作者无隶属关系。一键部署获取上游原始签名 APK；本机整合版是独立签名的修改版，支持默认卡及按次选卡。自动检查与恢复使用 Root 独立运行，恢复清理造成的异常；需要时可能重启 Shizuku，其他使用它的应用可能短暂断开。不修改 SIP 密码或通话音频设置。卸载助手前请关闭自动检查与恢复。");
  Button source=button(about,"查看上游项目与使用说明",false,()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/sparkcyf/android-voice-sip-agent"))));operations.remove(source);
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1);
 }
 void start(String action){if(DeployService.busy){Toast.makeText(this,"正在处理上一项操作",Toast.LENGTH_SHORT).show();return;}startForegroundService(new Intent(this,DeployService.class).putExtra("action",action));}
 void saveHealth(boolean on){
  if(DeployService.busy){Toast.makeText(this,"正在处理上一项操作",Toast.LENGTH_SHORT).show();return;}
  String minutes=healthMinutes.getText().toString().trim();try{HealthPolicy.minutes(minutes);}catch(Exception e){healthMinutes.setError(e.getMessage());return;}
  startForegroundService(new Intent(this,DeployService.class).putExtra("action","HEALTH_CONFIG").putExtra("healthEnabled",on).putExtra("healthMinutes",minutes));
 }
 void refreshHealth(){if(readingHealth||DeployService.busy)return;readingHealth=true;final android.content.Context c=getApplicationContext();new Thread(()->{try{DeployService.readHealthStatus(c);}catch(Exception ignored){}finally{readingHealth=false;}},"health-ui-status").start();}
 void open(String pkg){Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);if(launch==null){Toast.makeText(this,"尚未安装，请先一键部署",Toast.LENGTH_LONG).show();return;}try{startActivity(launch);}catch(Exception e){Toast.makeText(this,"无法打开，请从桌面启动该 App",Toast.LENGTH_LONG).show();}}
 String version(String pkg){PackageInfo info=Releases.installed(this,pkg);return info==null?"未安装":info.versionName;}
 void update(){
  SharedPreferences p=getSharedPreferences("state",0);String latest=p.getString("latest","尚未检查");versions.setText("网关（"+(Root.AGENT.equals(Root.DUALSIM)?"整合版":"官方版")+"）   "+version(Root.AGENT)+"\n官方最新版本       "+latest+"\nShizuku                  "+version(Root.SHIZUKU));
  String raw=p.getString("rootStatus","");String agent=p.getString("agentStatus","");String dialer="未检查";for(String line:raw.split("\n"))if(line.startsWith("dialer="))dialer=line.substring(7);
  String line="默认电话："+(dialer.equals("com.android.contacts")?"系统电话":dialer.equals(Root.AGENT)?"Voice SIP Agent":dialer)+"\nShizuku 进程："+(raw.contains("shizuku=running")?"运行中":raw.isEmpty()?"未检查":"未运行");
  line+="\n通话接管："+(agent.contains("takeover=on")?"已开启":agent.contains("takeover=off")?"已关闭":"未检查");
  line+="\nSIP 注册："+(agent.contains("sip=ready")?"就绪":agent.contains("sip=down")?"未就绪":"未检查");
  if(raw.contains("compat=on"))line+="\n联发科权限服务兼容：已启用";
  if(agent.contains("pending"))line+="（变更等待生效）";
  long checked=p.getLong("checkedAt",0);if(checked>0)line+="\n状态时间："+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(checked));status.setText(line);
  progress.setText(p.getString("progress","就绪")+(DeployService.busy?"\n处理中，请保留通知":""));
  for(Button b:operations)b.setEnabled(!DeployService.busy);boot.setEnabled(!DeployService.busy);updating=true;boot.setChecked(raw.contains("boot=on"));updating=false;
  updating=true;health.setChecked(p.getBoolean("healthEnabled",false));updating=false;health.setEnabled(!DeployService.busy);healthMinutes.setEnabled(!DeployService.busy);
  String details=health.isChecked()?"每 "+p.getInt("healthMinutes",5)+" 分钟检查一次":"自动检查已关闭";
  try{org.json.JSONObject h=new org.json.JSONObject(p.getString("healthStatus","{}"));long time=h.optLong("checkedAt");if(time>0)details+="\n上次检查："+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.MEDIUM).format(new Date(time))+"\n"+h.optString("message");}catch(Exception ignored){}
  healthStatus.setText(details);
  if(++ticks%5==0)refreshHealth();
 }
 public void onResume(){super.onResume();handler.post(tick);refreshHealth();}
 public void onPause(){handler.removeCallbacks(tick);super.onPause();}
}
