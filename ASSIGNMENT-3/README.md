# CSA09 – Assignment 3 (CO3): Exception Handling & Multithreading
Name: ________  Register No: ________  Java: OpenJDK 21 (works on 17+)

Compile/run (inside each QN folder):
- Q1: `javac -d out *.java && java -cp out AtmDemo` (also UncaughtTrace, ErrorCodeAtm; bad/BrokenOrder.java must FAIL to compile)
- Q2: `javac -d out portal/Portal.java portal/ExceptionCost.java && java -cp out Portal`; `javac -d o demo/StorageDemo.java && java -cp o StorageDemo`
- Q3: `javac -d out ReportGen.java && java -cp out ReportGen`
- Q4: `javac -d out *.java && java -cp out Stations` (PriorityDaemon [daemon], PoolStations)
- Q5: `javac -d out sync/SeatBooking.java && java -cp out SeatBooking` (unsync/ for the racy version; Race.java for the storm tests)
- Q6: `javac -d out *.java && java -cp out TokenCounter` (Wallet, CounterStudy; ifv/ = while->if experiment)
- Q7: `javac -d out ResultProcessor.java AltApproaches.java ThreadPerFileLong.java && java -cp out ResultProcessor gen` then `small` / `bench`

Results: Q1 all paths exercised; Q2 expected output reproduced; Q3 leaks 2/0/0; Q4 8 distinct orders/10 runs;
Q5 10/10 with synchronized; Q6 sum 210; Q7 10,000 = 9,498 + 502.
