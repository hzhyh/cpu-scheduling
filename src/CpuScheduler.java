import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

public class CpuScheduler {
    private static Scanner scanner = new Scanner(System.in); // 用來接受使用者的輸入
    private String filename;
    private int method;
    private int timeSlice;
    private int processesAmount;
    private ArrayList<Process> processes = new ArrayList<Process>();
    private int[] sortedIDs;
    private String[] ganttCharts = new String[5];
    private int[][] waitings;
    private int[][] turnarounds;
    

    public static void main(String[] args) {

        // 印出作者與題目資訊
        opening();
        
        while (true) {
            // 開始一個新的排序
            CpuScheduler scheduler = new CpuScheduler();
            
            // 讀檔
            scheduler.read();

            // 排程並輸出
            scheduler.run();
            
            // 決定是否要開始新的排程
            if (ending()) {
                break;
            }
        }
        
        System.out.println("Bye!");
    }
    
    private static void opening() {
        System.out.println("1092 作業系統 作業二");
        System.out.println();
        System.out.println("題目簡介：");
        System.out.println("給定一檔案內有各個Process之ID、CPU Burst、Arrival Time以及Priority，\n"
                         + "請根據這些資訊撰寫一程式模擬各種指定的CPU排程法。\n"
                         + "輸出結果必須繪出排程法的Gantt Chart，\n"
                         + "並計算每個Process的Turnaround Time及Waiting Time。\n"
                         + "程式須實現以下排程方法(Method)\n"
                         + "1.FCFS (First Come First Serve)\n"
                         + "2.RR (Round Robin)\n"
                         + "3.SRTF (Shortest Remaining Time First)\n"
                         + "4.PPRR (Preemptive Priority + RR)\n"
                         + "5.HRRN (Highest Response Ratio Next)\n"
                         + "6.ALL");
        System.out.println();
        System.out.println("Let's get started!");
        System.out.println();
    }

    private static boolean ending() {
        System.out.print("\nDo you want to schedule another file? (Y/N): ");
        char c = scanner.next().charAt(0);
        if (c != 'y' && c != 'Y') {
            return true;
        }
        return false;
    }

    private void read() {
        // 確認input資料夾存在
        File file = new File("input");
        while (!file.exists() || !file.isDirectory()) {
            System.out.println("Please put all input files in CpuScheduler/input/");
            System.out.println("Done? Press ENTER to continue...");
            scanner.nextLine();
        }
        
        // 輸入檔名，取得檔案
        System.out.print("Input filename: ");
        filename = scanner.next();
        file = new File("input", filename);
        
        // 若檔案不存在，要求重新輸入直到取得存在的檔案
        while (!file.exists()) {
            System.out.println("File not found! Check up and input again.");
            System.out.print("Input filename: ");
            filename = scanner.next();
            file = new File("input", filename);
        }
        
        // 讀取檔案內容
        Scanner fileScanner = null;
        try {
            fileScanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        method = fileScanner.nextInt();
        timeSlice = fileScanner.nextInt();
        System.out.println("Method: " + method + " Time Slice: " + timeSlice);
        fileScanner.nextLine();
        fileScanner.nextLine();
        while (true) {
            if (fileScanner.hasNextInt()) {
                int id = fileScanner.nextInt();
                int burst = fileScanner.nextInt();
                int arrival = fileScanner.nextInt();
                int priority = fileScanner.nextInt();
                processes.add(new Process(id, burst, arrival, priority));
            } else {
                break;
            }
        }
        fileScanner.close();
        
        processesAmount = processes.size();
        waitings = new int[6][processesAmount];
        turnarounds = new int[6][processesAmount];
        for (int i = 0; i < 5; i++) {
            ganttCharts[i] = "";
        }
        sortIDs();
        // 將processes的依arrival和id排序好
        sortProcessesByArrival(processes);
        
        printProcesses();
    }
    
    private void sortIDs() {
        sortedIDs = new int[processesAmount];
        for (int i = 0; i < processesAmount; i++) {
            sortedIDs[i] = processes.get(i).id;
        }
        Arrays.sort(sortedIDs);
    }
    
    private void printProcesses() {
        System.out.println("ID  CPU_Burst  Arrival_Time  Priority");
        for (int i = 0; i < processesAmount; i++) {
            Process p = processes.get(i);
            System.out.printf("%-4d%-11d%-14d%-10d%c", p.id, p.cpuBurst, p.arrivalTime, p.priority, p.oneDigitID);
            System.out.println();
        }
    }
    
    private void run() {
        // 排程
        switch (method) {
            case 1:
                fcfs();
                break;
            case 2:
                rr();
                break;
            case 3:
                srtf();
                break;
            case 4:
                pprr();
                break;
            case 5:
                hrrn();
                break;
            case 6:
                all();
        }
        // 輸出
        output();
    }
    
    private void saveData(ArrayList<Process> queue, int method) {
        for (int i = 0; i < processesAmount; i++) {
            int id = queue.get(i).id;
            for (int j = 0; j < processesAmount; j++) {
                if (sortedIDs[j] == id) {
                    waitings[method-1][j] = queue.get(i).waiting;
                    turnarounds[method-1][j] = queue.get(i).turnaround;
                }
            }
        }
    }

    private void sortProcessesByArrival(ArrayList<Process> queue) {
        // 先比ArrivalTime，再比ID
        queue.sort(
                Comparator.<Process, Integer>comparing(p -> p.arrivalTime)
                .thenComparing(p -> p.id)
        );
    }
    
    private void sortProcessesByRemaining(ArrayList<Process> queue) {
        // 先比remaining cpuBurst，再比arrival，再比ID
        queue.sort(
                Comparator.<Process, Integer>comparing(p -> p.remaining)
                .thenComparing(p -> p.arrivalTime)
                .thenComparing(p -> p.id)
        );
    }
    
    private void sortProcessesByRR(ArrayList<Process> queue, int time) {
        // 更新各process的RR-1(waiting/burstTime)
        for (Process p : queue) {
            p.responseRatio = (double)(time - p.arrivalTime) / p.cpuBurst;
        }
        // 先比RR，再比arrival，再比ID
        queue.sort(
                Comparator.<Process, Double>comparing(p -> p.responseRatio).reversed()
                .thenComparing(p -> p.arrivalTime)
                .thenComparing(p -> p.id)
        );
    }
    
    private void sortProcessesByArrivalPriorityID(ArrayList<Process> queue) {
        // 先比ArrivalTime，再比ID
        queue.sort(
                Comparator.<Process, Integer>comparing(p -> p.arrivalTime)
                .thenComparing(p -> p.priority)
                .thenComparing(p -> p.id)
        );
    }
    
    private ArrayList<Process> processesClone() {
        ArrayList<Process> list = new ArrayList<Process>();
        
        for (Process p : processes) {
            list.add(new Process(p));
        }
        
        return list;
    }
    
    private void fcfs() {
        int time = 0;
        
        // 依序把process加入排程
        for (int i = 0; i < processesAmount; i++) {
            Process p = processes.get(i);
            // 當cpu時間還沒到達當前要加入的process的arrival時間，cpu不做事，直到time=arrivalTime
            while (time < p.arrivalTime) {
                ganttCharts[0] += "-";
                time++;
            }
            // ganttChart 加入當前 process
            for (int j = 0; j < p.cpuBurst; j++) {
                ganttCharts[0] += p.oneDigitID;
                time++;
            }
            // 紀錄 waiting time 和 turnaround time
            p.finish(time);
        }
        
        // 更新waitings與turnarounds
        saveData(processes, 1);
    }
    
    private void rr() {
        // 將所有process加入jobQueue
        // 時間到了就將process從jobQueue移到readyQueue
        // process執行完畢再移至finishedQueue
        
        ArrayList<Process> jobQueue = processesClone();
        ArrayList<Process> readyQueue = new ArrayList<Process>();
        ArrayList<Process> finishedQueue = new ArrayList<Process>();
        int time = 0;
        
        boolean timeout = false; // 用來判斷下面的迴圈在上一輪有沒有人timeout，如果有，新加入的process要插到他前面
        // 如果還沒將所有行程排程完畢
        while (finishedQueue.size() < processesAmount) {
            // 若時間到了就將process加入readyQueue
            while (jobQueue.size() > 0) {
                if (jobQueue.get(0).arrivalTime <= time) {
                    if (timeout) {
                        // 插到上一回合timeout的process的前面
                        readyQueue.add(readyQueue.size()-1, jobQueue.get(0));
                    } else {
                        // 排到最後面
                        readyQueue.add(jobQueue.get(0));
                    }
                    jobQueue.remove(0);
                } else {
                    break;
                }
            }
            
            // 如果readyQueue是空的就不做事，否則抓第一個process來跑
            if (readyQueue.isEmpty()) {
                ganttCharts[1] += "-";
                time++;
            } else {
                // 拿出readyQueue的第一個process來執行
                Process p = readyQueue.get(0);
                readyQueue.remove(0);
                
                if (p.remaining <= timeSlice) {
                    // done
                    for (int i = 0; i < p.remaining; i++) {
                        ganttCharts[1] += p.oneDigitID;
                    }
                    time += p.remaining;
                    // 紀錄 waiting time 和 turnaround time
                    p.finish(time);
                    finishedQueue.add(p);
                    timeout = false;
                } else {
                    // timeout
                    for (int i = 0; i < timeSlice; i++) {
                        ganttCharts[1] += p.oneDigitID;
                    }
                    time += timeSlice;
                    p.remaining -= timeSlice;
                    readyQueue.add(p);
                    timeout = true;
                }
            }
        }
        
        // 更新waitings與turnarounds
        saveData(finishedQueue, 2);
    }
    
    private void srtf() {
        // 將所有process加入jobQueue
        // 時間到了就將process從jobQueue移到readyQueue
        // 每次都重新排序readyQueue再選第一個process執行
        // process執行完畢再移至finishedQueue
        ArrayList<Process> jobQueue = processesClone();
        ArrayList<Process> readyQueue = new ArrayList<Process>();
        ArrayList<Process> finishedQueue = new ArrayList<Process>();
        int time = 0;
        
        // 如果還沒將所有行程排程完畢
        while (finishedQueue.size() < processesAmount) {
            
            // 若時間到了就將process加入readyQueue
            while (jobQueue.size() > 0) {
                if (jobQueue.get(0).arrivalTime <= time) {
                    readyQueue.add(jobQueue.get(0));
                    jobQueue.remove(0);
                } else {
                    break;
                }
            }
            
            // 以剩餘cpuBurst重新排序readyQueue
            sortProcessesByRemaining(readyQueue);
            // 如果readyQueue是空的就不做事，否則抓第一個process來跑
            if (readyQueue.isEmpty()) {
                ganttCharts[2] += "-";
            } else {
                // 拿出readyQueue的第一個process來執行1個cpu time
                Process p = readyQueue.get(0);
                ganttCharts[2] += p.oneDigitID;
                p.remaining--;
                
                // 若此process執行完畢
                if (p.remaining == 0) {
                    // 紀錄 waiting time 和 turnaround time
                    p.finish(time + 1);
                    finishedQueue.add(p);
                    readyQueue.remove(0);
                }
            }
            
            time++;
        }
        
        // 更新waitings與turnarounds
        saveData(finishedQueue, 3);
    }
    
    private void pprr() {
        ArrayList<Process> jobQueue = processesClone();
        ArrayList<Process> readyQueue = new ArrayList<Process>();
        ArrayList<Process> finishedQueue = new ArrayList<Process>();
        int time = 0;
        
        boolean running = false; // 有沒有running process
        int runningPriority = -1; // running process 的 priority
        int remainingTime = timeSlice; // running process 的剩餘 timeSlice
        
        boolean timeout = false; // 紀錄下面的迴圈在上一輪有沒有人timeout
        int timeoutPriority = -1; // timeout的process的priority
        
        // 如果還沒將所有行程排程完畢
        while (finishedQueue.size() < processesAmount) {
            
            // 若時間到了就將process加入readyQueue
            while (jobQueue.size() > 0) {
                if (jobQueue.get(0).arrivalTime <= time) {
                    Process p = jobQueue.get(0);
                    jobQueue.remove(0);
                    // 如果p比running process更優先就preempt
                    if (running && p.priority < runningPriority) {
                        running = false;
                        remainingTime = timeSlice;
                        // 取出被preempt的process，讓他重新排隊
                        Process preempted = readyQueue.get(0);
                        readyQueue.remove(0);
                        timeout = true;
                        timeoutPriority = runningPriority;
                        pprrInsert(readyQueue, preempted, false, -1);
                        // 把p加入readyQueue
                        pprrInsert(readyQueue, p, false, -1);
                    } else {
                        pprrInsert(readyQueue, p, timeout, timeoutPriority);
                    }
                } else {
                    break;
                }
            }
            
            timeout = false;
            
            // 如果readyQueue是空的就不做事，否則抓第一個process來跑
            if (readyQueue.isEmpty()) {
                ganttCharts[3] += "-";
            } else {
                // 執行readyQueue的第一個process
                Process p = readyQueue.get(0);
                running = true;
                runningPriority = p.priority;
                ganttCharts[3] += p.oneDigitID;
                p.remaining--;
                remainingTime--;
                // 如果執行完畢，p放到finishedQueue
                if (p.remaining == 0) {
                    // 紀錄 waiting time 和 turnaround time
                    p.finish(time + 1);
                    readyQueue.remove(0);
                    finishedQueue.add(p);
                    running = false;
                    remainingTime = timeSlice;
                } else if (remainingTime == 0) {
                    // 如果timeout就去重新排隊
                    timeout = true;
                    timeoutPriority = p.priority;
                    running = false;
                    readyQueue.remove(0);
                    pprrInsert(readyQueue, p, false, -1);
                    remainingTime = timeSlice;
                }
            }
            
            time++;
        } // while
        
        // 更新waitings與turnarounds
        saveData(finishedQueue, 4);
    }
    
    private void pprrInsert(ArrayList<Process> queue, Process p, boolean timeout, int priority) {

        // 找出p要插入queue的index
        int index = -1;
        for (int i = 0; i < queue.size(); i++) {
            if (p.priority < queue.get(i).priority) {
                index = i;
                break;
            }
        }
        
        // 將p插入正確位置
        if (index == -1) { // 若p的priority比大家小，排到最後面
            queue.add(p);
        } else {
            if (timeout && p.priority == priority) {
                // 若p的priority與剛timeout或剛被preempt的process相同
                // 插到該timeout的process之前
                queue.add(index-1, p);
            } else {
                queue.add(index, p);
            }
        }
    }
    
    private void hrrn() {
        ArrayList<Process> jobQueue = processesClone();
        ArrayList<Process> readyQueue = new ArrayList<Process>();
        ArrayList<Process> finishedQueue = new ArrayList<Process>();
        int time = 0;
        
        // 如果還沒將所有行程排程完畢
        while (finishedQueue.size() < processesAmount) {
            
            // 若時間到了就將process加入readyQueue
            while (jobQueue.size() > 0) {
                if (jobQueue.get(0).arrivalTime <= time) {
                    readyQueue.add(jobQueue.get(0));
                    jobQueue.remove(0);
                } else {
                    break;
                }
            }
            
            // 以RR重新排序readyQueue
            sortProcessesByRR(readyQueue, time);
            // 如果readyQueue是空的就不做事，否則抓第一個process來跑
            if (readyQueue.isEmpty()) {
                ganttCharts[4] += "-";
                time++;
            } else {
                // 拿出readyQueue的第一個process來執行至結束
                Process p = readyQueue.get(0);
                for (int i = 0; i < p.cpuBurst; i++) {
                    ganttCharts[4] += p.oneDigitID;
                    time++;
                }
                // 紀錄 waiting time 和 turnaround time
                p.finish(time);
                finishedQueue.add(p);
                readyQueue.remove(0);
            }
        }
        
        // 更新waitings與turnarounds
        saveData(finishedQueue, 5);
    }
    
    private void all() {
        fcfs();
        rr();
        srtf();
        pprr();
        hrrn();
    }
    
    private void output() {
        
        // 創建output資料夾如果它不存在
        File file = new File("output");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }
        
        // 弄出output檔名
        filename = "out_" + filename;
        
        // 建立writer並開始輸出
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new File("output", filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        if (method == 6) writer.print("All\n");
        if (method == 6 || method == 1) {
            if (method == 6) {
                writer.print("==        FCFS==\n");
            } else {
                writer.print("FCFS\n");
            }
            writer.print(ganttCharts[0] + "\n");
        }
        if (method == 6 || method == 2) {
            if (method == 6) {
                writer.print("==          RR==\n");
            } else {
                writer.print("RR\n");
            }
            writer.print(ganttCharts[1] + "\n");
        }
        if (method == 6 || method == 3) {
            if (method == 6) {
                writer.print("==        SRTF==\n");
            } else {
                writer.print("SRTF\n");
            }
            writer.print(ganttCharts[2] + "\n");
        }
        if (method == 6 || method == 4) {
            if (method == 6) {                
                writer.print("==        PPRR==\n");
            } else {
                writer.print("PPRR\n");
            }
            writer.print(ganttCharts[3] + "\n");
        }
        if (method == 6 || method == 5) {
            if (method == 6) {                
                writer.print("==        HRRN==\n");
            } else {
                writer.print("HRRN\n");
            }
            writer.print(ganttCharts[4] + "\n");
        }
        writer.print("===========================================================\n\n");
        
        writer.print("waiting\n");
        writer.print("ID");
        if (method == 6 || method == 1) writer.print("\tFCFS");
        if (method == 6 || method == 2) writer.print("\tRR");
        if (method == 6 || method == 3) writer.print("\tSRTF");
        if (method == 6 || method == 4) writer.print("\tPPRR");
        if (method == 6 || method == 5) writer.print("\tHRRN");
        writer.print("\n===========================================================\n");
        // 印出各id的waiting
        for (int i = 0; i < processesAmount; i++) {
            writer.print(sortedIDs[i]);
            if (method == 6 || method == 1) writer.print("\t" + waitings[0][i]);
            if (method == 6 || method == 2) writer.print("\t" + waitings[1][i]);
            if (method == 6 || method == 3) writer.print("\t" + waitings[2][i]);
            if (method == 6 || method == 4) writer.print("\t" + waitings[3][i]);
            if (method == 6 || method == 5) writer.print("\t" + waitings[4][i]);
            writer.print("\n");
        }
        writer.print("===========================================================\n\n");
        
        writer.print("Turnaround Time\n");
        writer.print("ID");
        if (method == 6 || method == 1) writer.print("\tFCFS");
        if (method == 6 || method == 2) writer.print("\tRR");
        if (method == 6 || method == 3) writer.print("\tSRTF");
        if (method == 6 || method == 4) writer.print("\tPPRR");
        if (method == 6 || method == 5) writer.print("\tHRRN");
        writer.print("\n===========================================================\n");
        // 印出各id的Turnaround time
        for (int i = 0; i < processesAmount; i++) {
            writer.print(sortedIDs[i]);
            if (method == 6 || method == 1) writer.print("\t" + turnarounds[0][i]);
            if (method == 6 || method == 2) writer.print("\t" + turnarounds[1][i]);
            if (method == 6 || method == 3) writer.print("\t" + turnarounds[2][i]);
            if (method == 6 || method == 4) writer.print("\t" + turnarounds[3][i]);
            if (method == 6 || method == 5) writer.print("\t" + turnarounds[4][i]);
            writer.print("\n");
        }
        if (method == 6) {            
            writer.print("===========================================================\n\n");
        }
        
        writer.close();
    }
}
