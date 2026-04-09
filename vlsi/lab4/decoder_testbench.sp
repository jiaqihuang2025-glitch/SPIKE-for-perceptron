* Decoder testbench

.lib '/home/ff/eecs251b/sky130/sky130_cds/sky130_release_0.0.4/models/sky130.lib.spice' tt
.include '../build/lab4/pex-rundir/decoder.post.sp'

vvdd vdd 0 1.8
vgnd vss 0 0
vin a3 0 0 PULSE(0 1.8 0 50p 50p 2n 4n)

xdecoder vss z7 z4 vss vdd z14 z10 z1 vss vss z9 z11 z15 z13 z12 z5 z0 z8 vss z6 z2 z3 a3 decoder

c0 z0 vss 2E-15
c1 z1 vss 2E-15
c2 z2 vss 2E-15
c3 z3 vss 2E-15
c4 z4 vss 2E-15
c5 z5 vss 2E-15
c6 z6 vss 2E-15
c7 z7 vss 2E-15
c8 z8 vss 2E-15
c9 z9 vss 2E-15
c10 z10 vss 2E-15
c11 z11 vss 2E-15
c12 z12 vss 2E-15
c13 z13 vss 2E-15
c14 z14 vss 2E-15
c15 z15 vss 2E-15

.tran 10p 4n

.probe v(a3) v(z8)
.meas tran tplh trig v(a3) val='1.8/2' rise=1 targ v(z8) val='1.8/2' rise=1
.meas tran vdd_power AVG power from=0ns to=4ns

.END
