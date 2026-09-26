import org.phonevision.Nt4Client;
public class Nt4ClientDemo { public static void main(String[] a) throws Exception {
  Nt4Client c = new Nt4Client("PhoneVision-demo", "/PhoneVision/phone/");
  c.start(); c.setHost("127.0.0.1");
  for (int i = 0; i < 100 && !c.isConnected(); i++) Thread.sleep(50);
  System.out.println("status: " + c.status);
  for (int i = 0; i < 20; i++) { int k = i % 3; double[] d = new double[13 + 11 * k]; d[0] = i; d[1] = 25; d[3] = k; for (int j = 0; j < k; j++) { int o = 13 + 11 * j; d[o] = 4 + j; d[o + 1] = 2.0; d[o + 4] = 1; } c.publishFrame(d, new double[]{1, -3.5 + i, 7.25, 1.5, 4, 8, 12, 2.75, k, i}, 20.0); Thread.sleep(30); }
  Thread.sleep(500);
  c.stop();
}}
