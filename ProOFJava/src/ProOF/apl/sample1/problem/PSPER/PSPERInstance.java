/*
 * To change this template, choose Tools | Templates
 * and open this template in the editor.
 */
package ProOF.apl.sample1.problem.PSPER;

import ProOF.com.Linker.LinkerParameters;
import ProOF.opt.abst.problem.Instance;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * PSPER instance reader and data container.
 */
public class PSPERInstance extends Instance {
    public File file;

    public int H; // horizon in days
    public int nShifts;
    public int nPhysicians;
    public int nWeeks;
    public int nMonths;

    public String[] shiftId;
    public int[] shiftLength;
    public boolean[][] incompatibleNext;
    public HashMap<String, Integer> shiftIndex;

    public String[] physicianId;
    public HashMap<String, Integer> physicianIndex;

    public int[][] maxShiftsByType; // [p][s]
    public int[] maxTotalMinutes;
    public int[] minTotalMinutes;
    public int[] maxConsecutiveShifts;
    public int[] minConsecutiveShifts;
    public int[] minConsecutiveDaysOff;
    public int[] maxWeekends;

    public boolean[][] unavailable; // [p][d]
    public int[][][] shiftOnRequestWeight; // [p][d][s]
    public int[][][] shiftOffRequestWeight; // [p][d][s]

    public int[][] demand; // [d][s]
    public int[][] underWeight; // [d][s]
    public int[][] overWeight; // [d][s]

    public int[] weekOfDay; // [d]
    public boolean[] isWeekend; // [d]
    public int[] monthOfDay; // [d]

    public double[] targetHours; // [p], placeholder for future target hours by physician

    private final List<String> shiftLines = new ArrayList<String>();
    private final List<String> staffLines = new ArrayList<String>();
    private final List<String> daysOffLines = new ArrayList<String>();
    private final List<String> shiftOnLines = new ArrayList<String>();
    private final List<String> shiftOffLines = new ArrayList<String>();
    private final List<String> coverLines = new ArrayList<String>();
    private final List<String> targetHoursLines = new ArrayList<String>();
    private final List<String> monthLines = new ArrayList<String>();

    public PSPERInstance() {
        shiftIndex = new HashMap<String, Integer>();
        physicianIndex = new HashMap<String, Integer>();
    }

    @Override
    public String name() {
        return "Instance-PSPER ";
    }

    @Override
    public void parameters(LinkerParameters link) throws Exception {
        file = link.File("Instances for PSPER", null, "txt");
    }

    @Override
    public void load() throws FileNotFoundException {
        Scanner sc = new Scanner(file);
        try {
            collectSectionLines(sc);
            processShiftLines();
            processStaffLines();
            buildTimeIndexes();
            initializeSolutionStorage();
            processDaysOffLines();
            processShiftOnLines();
            processShiftOffLines();
            processCoverLines();
            processTargetHoursLines();
            processMonthLines();
            initializePlaceholderTargets();
        } finally {
            sc.close();
        }
    }
    
    
    
    
    private void collectSectionLines(Scanner sc) {
        String section = null;
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("SECTION_")) {
                section = line;
                continue;
            }
            if (section == null) {
                continue;
            }
            switch (section) {
                case "SECTION_HORIZON":
                    parseHorizon(line);
                    break;
                case "SECTION_SHIFTS":
                    shiftLines.add(line);
                    break;
                case "SECTION_STAFF":
                    staffLines.add(line);
                    break;
                case "SECTION_DAYS_OFF":
                    daysOffLines.add(line);
                    break;
                case "SECTION_SHIFT_ON_REQUESTS":
                    shiftOnLines.add(line);
                    break;
                case "SECTION_SHIFT_OFF_REQUESTS":
                    shiftOffLines.add(line);
                    break;
                case "SECTION_COVER":
                    coverLines.add(line);
                    System.out.println(line);
                    break;
                case "SECTION_TARGET_HOURS":
                    targetHoursLines.add(line);
                    break;
                case "SECTION_MONTHS":
                    monthLines.add(line);
                    break;
                default:
                    break;
            }
        }
    }

    private void parseHorizon(String line) {
        if (H == 0) {
            H = Integer.parseInt(line);
        }
    }

    private void processShiftLines() {
        nShifts = shiftLines.size();
        shiftId = new String[nShifts];
        shiftLength = new int[nShifts];
        incompatibleNext = new boolean[nShifts][nShifts];
        shiftIndex.clear();

        for (int i = 0; i < nShifts; i++) {
            String[] tokens = shiftLines.get(i).split(",");
            shiftId[i] = tokens[0].trim();
            shiftIndex.put(shiftId[i], i);
            shiftLength[i] = Integer.parseInt(tokens[1].trim());
        }

        for (int i = 0; i < nShifts; i++) {
            String[] tokens = shiftLines.get(i).split(",");
            if (tokens.length > 2 && !tokens[2].trim().isEmpty()) {
                String[] incompatible = tokens[2].split("\\|");
                for (String id : incompatible) {
                    id = id.trim();
                    if (!id.isEmpty()) {
                        Integer idx = shiftIndex.get(id);
                        if (idx != null) {
                            incompatibleNext[i][idx] = true;
                        }
                    }
                }
            }
        }
    }

    private void processStaffLines() {
        nPhysicians = staffLines.size();
        physicianId = new String[nPhysicians];
        maxShiftsByType = new int[nPhysicians][nShifts];
        maxTotalMinutes = new int[nPhysicians];
        minTotalMinutes = new int[nPhysicians];
        maxConsecutiveShifts = new int[nPhysicians];
        minConsecutiveShifts = new int[nPhysicians];
        minConsecutiveDaysOff = new int[nPhysicians];
        maxWeekends = new int[nPhysicians];
        physicianIndex.clear();

        for (int i = 0; i < nPhysicians; i++) {
            String[] tokens = staffLines.get(i).split(",");
            physicianId[i] = tokens[0].trim();
            physicianIndex.put(physicianId[i], i);
            parseMaxShifts(i, tokens[1].trim());
            maxTotalMinutes[i] = Integer.parseInt(tokens[2].trim());
            minTotalMinutes[i] = Integer.parseInt(tokens[3].trim());
            maxConsecutiveShifts[i] = Integer.parseInt(tokens[4].trim());
            minConsecutiveShifts[i] = Integer.parseInt(tokens[5].trim());
            minConsecutiveDaysOff[i] = Integer.parseInt(tokens[6].trim());
            maxWeekends[i] = Integer.parseInt(tokens[7].trim());
        }
    }

    private void parseMaxShifts(int p, String token) {
        String[] parts = token.split("\\|");
        for (String part : parts) {
            String[] kv = part.split("=");
            if (kv.length == 2) {
                String shift = kv[0].trim();
                int value = Integer.parseInt(kv[1].trim());
                int idx = findShiftIndex(shift);
                if (idx >= 0) {
                    maxShiftsByType[p][idx] = value;
                }
            }
        }
    }

    private void processDaysOffLines() {
        for (String line : daysOffLines) {
            String[] tokens = line.split(",");
            int p = physicianIndex.get(tokens[0].trim());
            for (int i = 1; i < tokens.length; i++) {
                int day = Integer.parseInt(tokens[i].trim());
                if (day >= 0 && day < H) {
                    unavailable[p][day] = true;
                }
            }
        }
    }

    private void processShiftOnLines() {
        for (String line : shiftOnLines) {
            String[] tokens = line.split(",");
            int p = physicianIndex.get(tokens[0].trim());
            int d = Integer.parseInt(tokens[1].trim());
            int s = findShiftIndex(tokens[2].trim());
            int weight = Integer.parseInt(tokens[3].trim());
            if (p >= 0 && d >= 0 && d < H && s >= 0) {
                shiftOnRequestWeight[p][d][s] = weight;
            }
        }
    }

    private void processShiftOffLines() {
        for (String line : shiftOffLines) {
            String[] tokens = line.split(",");
            int p = physicianIndex.get(tokens[0].trim());
            int d = Integer.parseInt(tokens[1].trim());
            int s = findShiftIndex(tokens[2].trim());
            int weight = Integer.parseInt(tokens[3].trim());
            if (p >= 0 && d >= 0 && d < H && s >= 0) {
                shiftOffRequestWeight[p][d][s] = weight;
            }
        }
    }
    
    
    public int longestShift()
    {
        int max = 0;
        for(int i=0;i < nShifts; i++){
            if(shiftLength[i] > max){
             max = shiftLength[i];
            }
        }
        return max;
    }
    
    
    public int endingM(int m) {
	return (m+1)*getMLenght();
    }
    
    public int beginingM(int m) {
	return m*getMLenght();
    }
    
    private void processCoverLines() {
        for (String line : coverLines) {
            String[] tokens = line.split(",");
            if (tokens.length < 5) {
                System.out.println("Skipping invalid cover line (insufficient tokens): "+line);
                continue;
            }

            int d;
            try {
                d = Integer.parseInt(tokens[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("Skipping invalid cover line (day not integer) in file "+ (file!=null?file.getName():"?") +": "+line);
                continue;
            }

            int s = findShiftIndex(tokens[1].trim());
            int req, uw, ov;
            try {
                req = Integer.parseInt(tokens[2].trim());
                uw = Integer.parseInt(tokens[3].trim());
                ov = Integer.parseInt(tokens[4].trim());
            } catch (NumberFormatException e) {
                System.out.println("Skipping invalid cover line (non-integer values) in file "+ (file!=null?file.getName():"?") +": "+line);
                continue;
            }

            if (d >= 0 && d < H && s >= 0) {
                demand[d][s] = req;
                underWeight[d][s] = uw;
                overWeight[d][s] = ov;
            }
        }
    }

    private void processTargetHoursLines() {
        if (targetHoursLines.isEmpty()) {
            return;
        }
        targetHours = new double[nPhysicians];
        for (String line : targetHoursLines) {
            String[] tokens = line.split(",");
            int p = physicianIndex.get(tokens[0].trim());
            double target = Double.parseDouble(tokens[1].trim());
            if (p >= 0 && p < nPhysicians) {
                targetHours[p] = target;
            }
        }
    }

    private void processMonthLines() {
        if (monthLines.isEmpty()) {
            return;
        }
        for (String line : monthLines) {
            String[] tokens = line.split(",");
            if (tokens.length == 2) {
                int day = Integer.parseInt(tokens[0].trim());
                int month = Integer.parseInt(tokens[1].trim());
                if (day >= 0 && day < H) {
                    monthOfDay[day] = month;
                    nMonths = Math.max(nMonths, month + 1);
                }
            }
        }
    }

    private int findShiftIndex(String id) {
        for (int i = 0; i < nShifts; i++) {
            if (shiftId[i].equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void buildTimeIndexes() {
        if (H <= 0) {
            return;
        }
        weekOfDay = new int[H];
        isWeekend = new boolean[H];
        monthOfDay = new int[H];
        nWeeks = (H + 6) / 7;
        nMonths = 1;
        for (int d = 0; d < H; d++) {
            weekOfDay[d] = d / 7;
            isWeekend[d] = (d % 7 == 5 || d % 7 == 6);
            monthOfDay[d] = 0;
        }
    }

    private void initializePlaceholderTargets() {
        if (targetHours == null || targetHours.length != nPhysicians) {
            targetHours = new double[nPhysicians];
            for (int p = 0; p < nPhysicians; p++) {
                targetHours[p] = maxTotalMinutes[p];
            }
        }
    }
    
    public int getMLenght()
    {
        return H > 14 ? 28 : 14;
    }
    
    public void initializeSolutionStorage() {
        unavailable = new boolean[nPhysicians][H];
        shiftOnRequestWeight = new int[nPhysicians][H][nShifts];
        shiftOffRequestWeight = new int[nPhysicians][H][nShifts];
        demand = new int[H][nShifts];
        underWeight = new int[H][nShifts];
        overWeight = new int[H][nShifts];

        for (int p = 0; p < nPhysicians; p++) {
            for (int d = 0; d < H; d++) {
                for (int s = 0; s < nShifts; s++) {
                    shiftOnRequestWeight[p][d][s] = 0;
                    shiftOffRequestWeight[p][d][s] = 0;
                    demand[d][s] = 0;
                    underWeight[d][s] = 0;
                    overWeight[d][s] = 0;
                }
            }
        }
    }

    public boolean isUnavailable(int p, int d) {
        return unavailable != null && unavailable[p][d];
    }
}
