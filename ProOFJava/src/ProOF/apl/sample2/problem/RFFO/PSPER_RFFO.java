/*
 * To change this template, choose Tools | Templates
 * and open this template in the editor.
 */
package ProOF.apl.sample2.problem.RFFO;

import ProOF.CplexExtended.CplexExtended;
import ProOF.apl.advanced2.FMS.RFFO.RFFOModel;
import ProOF.apl.advanced2.FMS.RFFO.RelaxVar;
import ProOF.apl.sample1.problem.PSPER.PSPERInstance;
import ProOF.apl.sample2.problem.cplex.PSPERmodel;
import ProOF.com.Linker.LinkerApproaches;
import ProOF.com.Linker.LinkerParameters;
import java.util.ArrayList;

/**
 * PSPER Relax-and-Fix / Fix-and-Optimize skeleton.
 */
public class PSPER_RFFO extends RFFOModel {

    public enum PartitionStrategy {
        BY_DAY,
        BY_PHYSICIAN,
        BY_LP_VALUE
    }

    public PSPERInstance inst = new PSPERInstance();
    private PSPERmodel model;
 //   CplexExtended cpx;
    
    public PartitionStrategy strategy = PartitionStrategy.BY_DAY;
    public int windowSize = 7;
    public int overlapSize = 2;
    public int rfIncrement = 10;
    public double improvementThreshold = 0.01;
    public double timeLimitSeconds = 60.0;

    public String instanceName;
    public String strategyName;
    public String statusSummary;
    public double UB;
    public double LB;
    public double gap;
    public double runtime;
    public double timeToBest;
    public double totalUnderCoverage;
    public double totalOverCoverage;
    public double totalPreferencePenalty;
    public double totalLAD;
    public int infeasibilityCount;

    public double weightUnderCoverage = 100.0;
    public double weightOverCoverage = 1.0;
    public double weightShiftOn = 1.0;
    public double weightShiftOff = 1.0;
    public double weightFairnessLAD = 10.0;

    
    private int windowsTypeRF;
    private int windowsTypeFO;

    @Override
    public String name() {
        return "PSPER-rffo";
    }

    @Override
    public void parameters(LinkerParameters link) throws Exception {
        super.parameters(link);
        
        windowsTypeRF = link.Itens("RF-Type", 0, "value-wise", 
                "row-wise", "column-wise");
        
        windowsTypeFO = link.Itens("FO-Type", 2,
                "only row", "only column", 
                "row>>column", "column>>row");
    }
    
    @Override
    public void services(LinkerApproaches link) throws Exception {
        super.services(link);
        inst = link.add(inst);
    }

    @Override
    public void model() throws Exception {
        model = new PSPERmodel(inst, cpx);
        model.model(true);
    }

    @Override
    public void print() throws Exception {
        model.print();
    }

    @Override
    public ArrayList<RelaxVar> relax_variables() throws Exception {
        
        switch (windowsTypeRF) {
            case 0: return byDistRef(row_wise(), 0.5);
            case 1: return row_wise();
            case 2: return column_wise();
        }
        throw new Exception("windowsTypeRF = "+windowsTypeRF+" is invalid"); 
    }
    
    
    private ArrayList<RelaxVar> row_wise() {
        ArrayList<RelaxVar> list = new ArrayList<RelaxVar>();
        for (int p = 0; p < inst.nPhysicians; p++) {
            for (int d = 0; d < inst.H; d++) {
                for (int s = 0; s < inst.nShifts; s++) {
                    list.add(new RelaxVar(model.X[p][d][s]));
                }
            }
        }
        return list;
    }
    private ArrayList<RelaxVar> column_wise() {
        ArrayList<RelaxVar> list = new ArrayList<RelaxVar>();
        for (int d = 0; d < inst.H; d++) {
            for (int p = 0; p < inst.nPhysicians; p++) {
                for (int s = 0; s < inst.nShifts; s++) {
                    list.add(new RelaxVar(model.X[p][d][s]));
                }
            }
        }
        return list;
    }
    
    
    @Override
    public ArrayList<RelaxVar> fix_variables() throws Exception {
        ArrayList<RelaxVar> list = new ArrayList<RelaxVar>();
        if (windowsTypeFO ==0) {
            for (int p = 0; p < inst.nPhysicians; p++) {
                for (int d = 0; d < inst.H; d++) {
                    for (int s = 0; s < inst.nShifts; s++) {
                        list.add(new RelaxVar(model.X[p][d][s]));
                    }
                }
            }
        } else {
            for (int d = 0; d < inst.H; d++) {
                for (int p = 0; p < inst.nPhysicians; p++) {
                    for (int s = 0; s < inst.nShifts; s++) {
                        list.add(new RelaxVar(model.X[p][d][s]));
                    }
                }
            }
        }
        return list;
    }

    public void run() throws Exception {
        instanceName = inst.file != null ? inst.file.getName() : "unknown";
        strategyName = strategy.name();
        statusSummary = "started";
        long start = System.currentTimeMillis();

        relaxAndFix();
        fixAndOptimize();

        runtime = (System.currentTimeMillis() - start) / 1000.0;
        statusSummary = cpx.getStatus().toString();
        collectMetrics();
    }

    protected void relaxAndFix() throws Exception {
        status = "Relax And Fix";
        fix_variables();
        solvePhase(timeLimitSeconds);
    }

    protected void fixAndOptimize() throws Exception {
        status = "Fix And Opt";
        solvePhase(timeLimitSeconds);
    }

    protected void initializeWindow() {
        // TODO: initialize window state and fixed/free variable sets based on strategy.
    }

    protected void moveWindow(int fromIndex, int toIndex) throws Exception {
        moveWindow(relax_variables, fromIndex, toIndex);
    }

    protected void updateFixedVariables() throws Exception {
        // TODO: fix variables progressively after each RF window.
    }

    protected void applyVariableStates() throws Exception {
        // TODO: apply fixed, relaxed and MIP variable states to the model.
    }

    protected void handleInfeasibility() throws Exception {
        // TODO: handle infeasible subproblems and free variables if needed.
        infeasibilityCount++;
    }

    protected void collectMetrics() throws Exception {
        if (cpx.getStatus() == null) {
            return;
        }
        UB = cpx.getObjValue();
        LB = cpx.getBestObjValue();
        gap = gap();
        timeToBest = 0.0; // TODO: capture time to best if available
        totalUnderCoverage = 0.0; // TODO: compute from model solution
        totalOverCoverage = 0.0; // TODO: compute from model solution
        totalPreferencePenalty = 0.0; // TODO: compute from model solution
        totalLAD = 0.0; // TODO: compute from model solution
    }

    
    protected String exportCsvLine() {
        return String.format("%s,%s,%s,%.4f,%.4f,%.4f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d",
                instanceName,
                strategyName,
                statusSummary,
                UB,
                LB,
                gap,
                runtime,
                timeToBest,
                totalUnderCoverage,
                totalOverCoverage,
                totalPreferencePenalty,
                totalLAD,
                infeasibilityCount);
    }

    protected void solvePhase(double timeLimit) throws Exception {
        if (!cpx.solve()) {
            handleInfeasibility();
        }
    }
}
