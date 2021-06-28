class Process {
    final int id;
    final int cpuBurst;
    final int arrivalTime;
    final int priority;
    final char oneDigitID;
    int remaining;
    
    double responseRatio; // (rr-1)
    int waiting;
    int turnaround;
    
    Process(int id, int cpuBurst, int arrivalTime, int priority) {
        this.id = id;
        this.cpuBurst = cpuBurst;
        this.arrivalTime = arrivalTime;
        this.priority = priority;
        if (id < 10) {
            oneDigitID = (char) ((int)'0' + id);
        } else {
            oneDigitID = (char) ((int)'A' + (id - 10));
        }
        remaining = cpuBurst;
    }
    
    Process(Process p) {
        id = p.id;
        cpuBurst = p.cpuBurst;
        arrivalTime = p.arrivalTime;
        priority = p.priority;
        oneDigitID = p.oneDigitID;
        remaining = p.remaining;
    }
    
    void finish(int finishTime) {
        turnaround  = finishTime - arrivalTime;
        waiting = turnaround - cpuBurst;
    }
}
