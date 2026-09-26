import org.phonevision.SelfCheck; import org.phonevision.SelfCheck.*; import org.phonevision.CameraController;
public class SelfCheckTest { static int f=0; static void c(String n,boolean ok){System.out.println((ok?"PASS ":"FAIL ")+n); if(!ok)f++;}
 public static void main(String[] a){
  c("fps 30 OK, 20 WARN, 8 FAIL", SelfCheck.fpsLevel(30)==Level.OK&&SelfCheck.fpsLevel(20)==Level.WARN&&SelfCheck.fpsLevel(8)==Level.FAIL);
  c("proc 20 OK, 40 WARN, 80 FAIL", SelfCheck.procLevel(20)==Level.OK&&SelfCheck.procLevel(40)==Level.WARN&&SelfCheck.procLevel(80)==Level.FAIL);
  c("temp 33 OK, 40 WARN, 45 FAIL, thermal3 FAIL", SelfCheck.tempLevel(33,0)==Level.OK&&SelfCheck.tempLevel(40,0)==Level.WARN&&SelfCheck.tempLevel(45,0)==Level.FAIL&&SelfCheck.tempLevel(30,3)==Level.FAIL&&SelfCheck.tempLevel(30,2)==Level.WARN);
  c("exposure exact OK", SelfCheck.exposureLevel(4_000_000L,4_000_000L,0)==Level.OK);
  c("exposure 4ms->4.1ms OK", SelfCheck.exposureLevel(4_000_000L,4_100_000L,0)==Level.OK);
  c("exposure 4ms->6ms WARN (quantized/clamped)", SelfCheck.exposureLevel(4_000_000L,6_000_000L,0)==Level.WARN);
  c("exposure 4ms->20ms FAIL", SelfCheck.exposureLevel(4_000_000L,20_000_000L,0)==Level.FAIL);
  c("AE still ON -> FAIL even if value close", SelfCheck.exposureLevel(4_000_000L,4_000_000L,1)==Level.FAIL);
  c("no result yet -> INFO", SelfCheck.exposureLevel(4_000_000L,-1,0)==Level.INFO);
  c("clamp", SelfCheck.clamp(100,10,50)==50&&SelfCheck.clamp(1,10,50)==10);
  // full run: healthy phone
  Inputs in=new Inputs(); in.camPermission=true; in.nativeOk=true; in.nativeMsg="apriltag loaded (arm64-v8a)"; in.framesFlowing=true; in.fps=30; in.procAvgMs=15; in.tempC=33; in.tempStartC=31; in.charging=true; in.batteryExempt=true; in.ntConnected=true; in.ntStatus="connected";
  in.cam.level="FULL"; in.cam.manual=true; in.cam.expMinNs=100_000L; in.cam.expMaxNs=500_000_000L; in.cam.isoMin=100; in.cam.isoMax=3200; in.reqExposureMs=4; in.reqIso=400; in.actExposureNs=4_000_000L; in.actIso=400; in.actAeMode=0;
  var items=SelfCheck.run(in); boolean allOk=true; for(var it:items){ if(it.level==Level.FAIL||it.level==Level.WARN) allOk=false; }
  c("healthy phone -> no WARN/FAIL ("+items.size()+" rows)", allOk);
  // broken phone
  in.cam.manual=false; in.nativeOk=false; in.nativeMsg="UnsatisfiedLinkError"; in.tempC=46; in.charging=false; in.camPermission=false;
  long bad=SelfCheck.run(in).stream().filter(x->x.level==Level.FAIL||x.level==Level.WARN).count();
  c("broken phone flags many issues ("+bad+")", bad>=5);
  // requested exposure outside hardware range is clamped before comparison
  in.cam.manual=true; in.nativeOk=true; in.camPermission=true; in.reqExposureMs=0.001f; in.cam.expMinNs=100_000L; in.actExposureNs=100_000L; in.actAeMode=0; in.tempC=30; in.charging=true;
  c("req below hardware min uses clamped value (OK)", SelfCheck.run(in).stream().filter(x->x.name.startsWith("Manual exposure test")).allMatch(x->x.level==Level.OK));
  System.exit(f);}}
