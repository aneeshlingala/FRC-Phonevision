#!/usr/bin/env python3
"""Pretend to be the roboRIO: runs an NT4 server on this laptop and prints what the phone publishes.
   pip install pyntcore ; python3 mock_robot.py [camName]
   In the app set 'Robot IP override' to this laptop's IP. Ctrl-C to quit."""
import ntcore, sys, time
cam = sys.argv[1] if len(sys.argv) > 1 else "phone"
inst = ntcore.NetworkTableInstance.getDefault(); inst.startServer(port4=5810)
sc = {k: inst.getDoubleTopic(f"/PhoneVision/{cam}/{k}").subscribe(0.0) for k in ("tv","tx","ty","ta","tid","tl","cl","td")}
sub = inst.getDoubleArrayTopic(f"/PhoneVision/{cam}/data").subscribe([], ntcore.PubSubOptions(keepDuplicates=True, pollStorage=50))
last = 0
while True:
    for v in sub.readQueue():
        a = v.value
        if len(a) < 13: continue
        n = int(a[3])
        if n < 0 or len(a) < 13 + 11 * n: print('malformed packet, len', len(a)); continue
        age = (ntcore._now() - v.time) / 1000
        line = f"seq {int(a[0])} fps {a[2]:.0f} latency {a[1]:.0f}ms (ts age now {age:.0f}ms) tags {n}"
        if a[4] >= 2: line += f" | MULTI({int(a[4])}) cam@field xyz=({a[5]:.2f},{a[6]:.2f},{a[7]:.2f}) rms={a[12]:.2f}px"
        print(line)
        print('    LL-style: ' + ' '.join(f'{k}={s.get():.2f}' for k, s in sc.items()))
        for i in range(n):
            o = 13 + 11 * i
            print(f"    id {int(a[o])} cam->tag xyz=({a[o+1]:.2f},{a[o+2]:.2f},{a[o+3]:.2f}) err={a[o+8]:.2f}px amb={a[o+9]:.2f} margin={a[o+10]:.0f}")
    time.sleep(0.005)
