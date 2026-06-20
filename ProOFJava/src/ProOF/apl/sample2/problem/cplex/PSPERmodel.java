/*
 * To change this template, choose Tools | Templates
 * and open this template in the editor.
 */
package ProOF.apl.sample2.problem.cplex;

import ProOF.CplexExtended.CplexExtended;
import ProOF.apl.sample1.problem.PSPER.PSPERInstance;
import ProOF.com.Linker.LinkerResults;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * CPLEX model for PSPER.
 *
 * This class provides a skeleton for the mathematical formulation,
 * with separate methods for variables, objective and constraints.
 */
public class PSPERmodel {
    public final PSPERInstance inst;
    
    CplexExtended cpx;

    public IloNumVar[][][] X;        // x[p][d][s]
    public IloNumVar[][] Y;          // y[p][w]
    public IloNumVar[][] W;          // underCoverage[d][s]
    public IloNumVar[][] Z;          // overCoverage[d][s]
    public IloNumVar[][] SigmaH;     // sigmaH[p][m]
    public IloNumVar[][] TargetH;     // TH[p][m]
    public IloNumVar[][] Work;       // work indicator for day
    public IloNumVar[][] Hours;      // hours per physician per month
    public IloNumVar one;            // constant 1 variable for linear expressions

    public boolean relaxed;

    private IloNumExpr ObjF1, ObjF2, ObjF3, ObjF4, ObjF5;    
    private IloNumExpr ObjValue;
    public double weightUnderCoverage = 100.0;
    public double weightOverCoverage = 50.0;
    public double weightShiftOn = -1.0;
    public double weightShiftOff = 1.0;
    public double weightFairnessLAD = 1.0;

    public PSPERmodel(PSPERInstance inst, CplexExtended cpx) {
        this.inst = inst;
        this.cpx = cpx;
    }

    public void buildModel(boolean relaxed) throws Exception {
        model(relaxed);
    }

    public void model(boolean relaxed) throws Exception {
        this.relaxed = relaxed;
        createVariables();
        addObjective();
        addHardConstraints();
        addSoftConstraints();
        addCoverageConstraints();
        
        cpx.exportModel("PSPER_"+ inst.nPhysicians+"_"+inst.H+"_"+inst.nShifts+"_"+inst.underWeight[inst.H-1][inst.nShifts-1]+"u"+inst.overWeight[inst.H-1][inst.nShifts-1]+"o.lp");
   //file.getName()
      //  targetHours();
       // addLADConstraints();
    }

    protected void createVariables() throws IloException {
        if (relaxed) {
            X = cpx.numVarArray(inst.nPhysicians, inst.H, inst.nShifts, 0.0, 1.0, "X");
            Work = cpx.numVarArray(inst.nPhysicians, inst.H, 0.0, 1.0, "Work");
        } else {
            X = cpx.boolVarArray(inst.nPhysicians, inst.H, inst.nShifts, "X");
            Work = cpx.boolVarArray(inst.nPhysicians, inst.H, "Work");
        }
        Y = cpx.boolVarArray(inst.nPhysicians, inst.nWeeks, "Y");
        W = cpx.numVarArray(inst.H, inst.nShifts, 0, 100, "W");
        Z = cpx.numVarArray(inst.H, inst.nShifts, 0, 100, "Z");
        SigmaH = cpx.numVarArray(inst.nPhysicians, inst.nMonths, 0.0, Double.MAX_VALUE, "SigmaH");
        TargetH = cpx.numVarArray(inst.nPhysicians, inst.nMonths, 0.0, Double.MAX_VALUE, "TargetH");

        Hours = cpx.numVarArray(inst.nPhysicians, inst.nMonths, 0.0, Double.MAX_VALUE, "Hours");
        one = cpx.numVar(1.0, 1.0, "ONE");
    }

    protected void addObjective() throws IloException {
        IloNumExpr objective = null;
//Z[j][k]), Y[j][k]

        /** shiftoff request. */
        ObjF1  = null;
        
        for (int p = 0; p < inst.nPhysicians; p++) {
            for (int d = 0; d < inst.H; d++) {
                for (int s = 0; s < inst.nShifts; s++) {
                    int offWeight = inst.shiftOffRequestWeight[p][d][s];   
                    if (offWeight != 0) {
                        ObjF1 = cpx.SumProd(ObjF1, offWeight,  X[p][d][s]);  
                      //  objective = cpx.SumProd(objective, offWeight, X[p][d][s]);
                    }
                }
            }              
        }
        
        
        ObjF2  = null;
        
        for (int p = 0; p < inst.nPhysicians; p++) {
            for (int d = 0; d < inst.H; d++) {
                for (int s = 0; s < inst.nShifts; s++) {
                    int onWeight = inst.shiftOnRequestWeight[p][d][s];   
                    if (onWeight != 0) {
                        
                        ObjF2 = cpx.SumProd(ObjF2, onWeight, cpx.sum(1, cpx.prod(-1, X[p][d][s])));  
                      //  objective = cpx.SumProd(objective, offWeight, X[p][d][s]);
                    }
                }
            }              
        }
        
        ObjF3 = null;
              
        for (int d = 0; d < inst.H; d++) {
            for (int s = 0; s < inst.nShifts; s++) {  
                ObjF3 = cpx.SumProd(ObjF3, inst.overWeight[d][s], Z[d][s]);
            }
        }

        ObjF4 = null;
              
        for (int d = 0; d < inst.H; d++) {
            for (int s = 0; s < inst.nShifts; s++) {  
                ObjF4 = cpx.SumProd(ObjF4, inst.underWeight[d][s], W[d][s]);
            }
        }
        
        ObjF5 = null;
          
        for(int p=0; p<inst.nPhysicians; p++){
            for(int m=0; m<inst.nMonths; m++){
                ObjF5 = cpx.SumProd(ObjF5, weightFairnessLAD, SigmaH[p][m]);
            }
        }     /**/ 
        
       
       
        ObjValue = cpx.sum(ObjF1, ObjF2, ObjF3, ObjF4, ObjF5);
        cpx.addMinimize(ObjValue);
    }

    protected void addHardConstraints() throws IloException {
        addOneShiftPerDay();
        addShiftIncompatibilities();
        addShiftTypeLimits();
        maximumConsecutiveShifts();
        minimumConsecutiveShifts();
        minConsecutiveDaysOff();
    //    addTotalMinutesLimits();
        addUnavailabilityConstraints();
        maxWeekend();
        targetHours();
    }
    
    
    public void print() throws Exception {
        
        double vXij[][][] = cpx.getValues(X);
        
        
        BufferedWriter bw = null;
        FileWriter fw = null;
        
        fw = new FileWriter("PSPER_"+ inst.nPhysicians+"_"+inst.H+"_"+inst.nShifts+"_"+inst.underWeight[inst.H-1][inst.nShifts-1]+"u"+inst.overWeight[inst.H-1][inst.nShifts-1]+"o.csv");
        bw = new BufferedWriter(fw);
        
        
        bw.write(" [ Xijk ] ");
        for(int d=0; d<inst.H; d++){
            for(int t=0; t<inst.nShifts; t++){
                bw.write("\t Dia "+(d+1)+": "+(inst.shiftId[t]));
            }
        }
        
        bw.write("\n");
        
        for(int i=0; i<inst.nPhysicians; i++){
            bw.write("PHY["+(i+1)+"]");
            for(int j=0; j<inst.H; j++){
                for(int k=0;k<inst.nShifts;k++){
                    bw.write(" \t"+(int)vXij[i][j][k]);
                }
            }
            bw.write("\n");
        }
        
        bw.write("\n");
        
        for(int k=0;k<inst.nShifts;k++){
            bw.write("\tDemand : "+inst.shiftId[k]+"\t Coverage: "+inst.shiftId[k]+"\tDifference: "+inst.shiftId[k]);
        }
        
        bw.write("\n");
        for(int j=0; j<inst.H; j++){
            bw.write("Day "+(j+1));
            for(int k=0;k<inst.nShifts;k++){
                
                int dem = 0;
                for(int i=0; i<inst.nPhysicians; i++){
                    dem += (int)vXij[i][j][k];
                }//demand[d][s]
                bw.write("\t"+inst.demand[j][k] +"\t"+dem+"\t"+(inst.demand[j][k] - dem));
            }
            bw.write("\n");
        }
        
        
        bw.write("\n");
        bw.write("\t \t Hour deviations \n");

        for(int t=0; t< inst.nMonths; t++){
             bw.write("\t Dev. "+(t+1)+"\t Anc. Targ \t Diff \t MHours  \t mHours");
        }

        bw.write("\n");
        int TARGET;
        for(int i=0; i<inst.nPhysicians; i++){
            bw.write("PHY["+( i+1)+"]");
            for(int m=0; m< inst.nMonths; m++){
                int hours = 0;
                for(int j=inst.beginingM(m);j<inst.endingM(m);j++){
                    for(int s =0;s<inst.nShifts;s++){
                        
                        hours += ((int)vXij[i][j][s] > 0.5 ? 1 : 0)*inst.shiftLength[s];
                    }
                }
                TARGET =(int) (inst.minTotalMinutes[i]+inst.maxTotalMinutes[i])/(2*inst.nMonths);
                bw.write("\t"+(int)hours+"\t"+TARGET+"\t "+(TARGET - hours)+"\t"+inst.maxTotalMinutes[i]/inst.nMonths+"\t"+inst.minTotalMinutes[i]/inst.nMonths);
            }
            bw.write("\n");
        }
        
        bw.write("\n");
   
        if (bw != null)
            bw.close();

        if (fw != null)
            fw.close();
        
    }
    
    private void addOneShiftPerDay() throws IloException {
        for (int p = 0; p < inst.nPhysicians; p++) {
            for (int d = 0; d < inst.H; d++) {
                IloNumExpr sum1[] = new IloNumExpr[inst.nShifts];
                IloNumExpr sum = null;
                for (int s = 0; s < inst.nShifts; s++) {
                    sum1[s] = X[p][d][s];
                //    sum = cpx.SumProd(sum, 1, X[p][d][s]);
                }
                cpx.addLe(cpx.Sum(sum1) , 1, "OneShiftADay("+p+","+d+")");
                //cpx.addLe(sum, 1, "OneShift[" + p + "," + d + "]");
            }
        }
    }

    private void addShiftIncompatibilities() throws IloException {
        for (int p = 0; p < inst.nPhysicians; p++) {
            for (int d = 0; d < inst.H - 1; d++) {
                for (int s = 0; s < inst.nShifts; s++) {
                    for (int t = 0; t < inst.nShifts; t++) {
                        if (inst.incompatibleNext[s][t]) {
                            IloNumExpr expr = cpx.sum(X[p][d][s], X[p][d + 1][t]);
                            cpx.addLe(expr, 1, "Incompat[" + p + "," + d + "," + s + "," + t + "]");
                        }
                    }
                }
            }
        }
    }

    private void addShiftTypeLimits() throws IloException {
        for (int p = 0; p < inst.nPhysicians; p++) {
            for (int s = 0; s < inst.nShifts; s++) {
              //  IloNumExpr sum = null;
                IloNumExpr Sum_Xijk[] = new IloNumExpr[inst.H];
                for (int d = 0; d < inst.H; d++) {
                    Sum_Xijk[d] = X[p][d][s];
               //     sum = cpx.SumProd(sum, 1, X[p][d][s]);
                }
                cpx.addLe(cpx.Sum(Sum_Xijk), inst.maxShiftsByType[p][s], "MaxType[" + p + "," + s + "]");
            }
        }
    }

    private void addTotalMinutesLimits() throws IloException {
        for (int p = 0; p < inst.nPhysicians; p++) {
            IloNumExpr Sum_Xijk[][] = new IloNumExpr[inst.H][inst.nShifts];
           
            //IloNumExpr sumMinutes = null;
            for (int d = 0; d < inst.H; d++) {
                for (int s = 0; s < inst.nShifts; s++) {
                    Sum_Xijk[d][s] = cpx.prod(X[p][d][s],  inst.shiftLength[s]);
                }
            }
            cpx.addGe(cpx.Sum(cpx.Sum(Sum_Xijk)), inst.minTotalMinutes[p], "MinMinutes[" + p + "]");
            cpx.addLe(cpx.Sum(cpx.Sum(Sum_Xijk)), inst.maxTotalMinutes[p], "MaxMinutes[" + p + "]");
        }
    }

    private void addUnavailabilityConstraints() throws IloException {
        for (int p = 0; p < inst.nPhysicians; p++) {
            for (int d = 0; d < inst.H; d++) {
                if (inst.unavailable[p][d]) {
                    for (int s = 0; s < inst.nShifts; s++) {
                        cpx.addEq(X[p][d][s], 0, "Unavailable[" + p + "," + d + "," + s + "]");
                    }
                }
            }
        }
    }
    
    
    private void maximumConsecutiveShifts() throws IloException {  
        
        for(int i=0; i<this.inst.nPhysicians;i++){
            int incr = this.inst.maxConsecutiveShifts[i];
                
            for(int d= 0; d<this.inst.H-incr; d++){
                IloNumExpr Sum_Xijk[][]= new IloNumExpr[incr+1][this.inst.nShifts];
                for(int j=d, a=0; j<=d+incr && a <=incr ;j++,a++){
                    for(int t=0; t<this.inst.nShifts; t++){
                        Sum_Xijk[a][t] = X[i][j][t];
                    }
                }
                cpx.addLe(cpx.Sum(Sum_Xijk), incr, "Consecutive("+i+","+d+"-"+(d+incr)+")");
            }
        }
    }
    
    private void minimumConsecutiveShifts() throws IloException {  
        for(int i=0; i < this.inst.nPhysicians; i++)
        {  
            for(int c=1; c<= this.inst.minConsecutiveShifts[i]-1; c++){
                
                for(int d = 0; d< this.inst.H-(c+1);d++)
                {   
                    IloNumExpr Sum_Xidt[] = new IloNumExpr[inst.nShifts];
                    for(int s = 0; s < this.inst.nShifts; s++)
                    {
                        Sum_Xidt[s] = X[i][d][s];
                    }
                    
                    IloNumExpr Sum_Xijt[][] = new IloNumExpr[c][inst.nShifts];
                    for(int j=d+1, a=0;j<=d+c && a < c;j++, a++)
                    {
                        for(int t = 0; t < this.inst.nShifts; t++)
                        {
                            Sum_Xijt[a][t] = X[i][j][t];
                        }
                    }

                    IloNumExpr Sum_Xilt[]= new IloNumExpr[inst.nShifts];
                    for(int t = 0; t < this.inst.nShifts; t++)
                    {
                        Sum_Xilt[t] = X[i][d+c+1][t];
                    }
                   
                    cpx.addGe(cpx.sum(cpx.Sum(Sum_Xidt), cpx.sum(c, cpx.prod(-1, cpx.Sum(Sum_Xijt))), cpx.Sum(Sum_Xilt)),1, "MinConsecutive("+i+","+c+","+d+","+(d+c+1)+")");
                }
            }
        }
    }
    
    private void minConsecutiveDaysOff() throws IloException {          
        for(int i=0; i < this.inst.nPhysicians; i++)
        { 
            for(int b=1; b<= this.inst.minConsecutiveDaysOff[i]-1; b++){
                for(int d = 0; d< this.inst.H-(b+1);d++)
                {   
                    IloNumExpr Sum_Xidt[] = new IloNumExpr[inst.nShifts];
                    for(int t = 0; t < this.inst.nShifts; t++)
                    {
                        Sum_Xidt[t] = X[i][d][t];
                    }
                
                    IloNumExpr Sum_Xijt[][] = new IloNumExpr[b][inst.nShifts];
                    for(int j=d+1, a=0;j<=d+b && a < b;j++, a++)
                    {
                        for(int t = 0; t < this.inst.nShifts; t++)
                        {
                            Sum_Xijt[a][t] = X[i][j][t];
                        }
                    }

                    IloNumExpr Sum_Xilt[]= new IloNumExpr[inst.nShifts];
                    for(int t = 0; t < this.inst.nShifts; t++)
                    {
                        Sum_Xilt[t] = X[i][d+b+1][t];
                    }
                       
                    cpx.addGe(cpx.sum(cpx.sum(1, cpx.prod(-1, cpx.Sum(Sum_Xidt))) , cpx.Sum(Sum_Xijt), cpx.prod(-1, cpx.Sum(Sum_Xilt))), 0, "MinConsDaysOff("+i+","+d+")");
                }
            }
        }
    }
    
    private void maxWeekend() throws IloException {  
        
        for(int i=0; i < this.inst.nPhysicians; i++)
        {
            IloNumExpr Sum_Yiw[] = new IloNumExpr[inst.nWeeks];
            for(int d=5, w =0; d< this.inst.H && w < inst.nWeeks; d+=7, w++){
                   
                IloNumExpr Sum_Xist[] = new IloNumExpr[inst.nShifts];
                IloNumExpr Sum_Xisd[] = new IloNumExpr[inst.nShifts];
                for(int t = 0; t < this.inst.nShifts; t++)
                {
                    Sum_Xist[t] = X[i][d][t];
                    if((d+1)<inst.H)
                        Sum_Xisd[t] = X[i][d+1][t];
                }
                Sum_Yiw[w] = Y[i][w];
                cpx.addLe(cpx.sum(cpx.Sum(Sum_Xist), cpx.Sum(Sum_Xisd)), cpx.prod(2, Y[i][w]), "weekends("+i+","+w+")");
                cpx.addGe(cpx.sum(cpx.Sum(Sum_Xist), cpx.Sum(Sum_Xisd)), Y[i][w], "weekends("+i+","+w+")");
            }
            
            cpx.addLe(cpx.Sum(Sum_Yiw) , inst.maxWeekends[i], "weeksMax("+i+")");
        }
    }
    

    protected void addSoftConstraints() throws IloException {
        // TODO: add additional soft constraints or penalty variables for preferences and fairness.
    }

    protected void addCoverageConstraints() throws IloException {
        
        for(int j=0; j<inst.H; j++){
            for(int k=0; k<inst.nShifts; k++){
                IloNumExpr Sum_X[] = new IloNumExpr[inst.nPhysicians];
                
                for(int i=0; i<inst.nPhysicians; i++){
                       Sum_X[i] = X[i][j][k];
                }
                cpx.addEq(cpx.sum(cpx.Sum(Sum_X), cpx.prod(-1, Z[j][k]), W[j][k]), this.inst.demand[j][k], "MaxDemand("+j+","+k+")");
            }
        }

    }
    
     private void targetHours() throws IloException {
        for(int p=0; p< inst.nPhysicians; p++)
        {
            int value = inst.maxConsecutiveShifts[p] + inst.minConsecutiveDaysOff[p];
            
            int rest = (int) Math.round(inst.getMLenght()/value);
            rest = rest*inst.longestShift()*inst.maxConsecutiveShifts[p];
       
            
            for(int m=0; m<inst.nMonths; m++){
                IloNumExpr Sum_Xijk[][] = new IloNumExpr[inst.getMLenght()][inst.nShifts];
        
                for(int d=inst.beginingM(m), a = 0; d< inst.endingM(m) && a<inst.getMLenght(); d++, a++){
                    for(int s=0; s< inst.nShifts; s++){
                        
                        Sum_Xijk[a][s] = cpx.prod(X[p][d][s], inst.shiftLength[s]);
                        
                    }
                }
                
                IloNumExpr sum = cpx.Sum(cpx.Sum(Sum_Xijk));
                
                cpx.addLe(sum, cpx.sum(SigmaH[p][m], TargetH[p][m] ), "Hours["+p+"]["+m+"]");
                cpx.addGe(sum, cpx.sum(cpx.prod(-1, SigmaH[p][m]), TargetH[p][m]), "Hours["+p+"]["+m+"]");  
                cpx.addLe(SigmaH[p][m], rest, "DEV["+p+"]["+m+"]");
                cpx.addLe(TargetH[p][m], rest, "TARG["+p+"]["+m+"]");   
            }
        }
    }   
    
    protected void addLADConstraints() throws IloException {
        for (int p = 0; p < inst.nPhysicians; p++) {
            for (int m = 0; m < inst.nMonths; m++) {
                IloNumExpr hoursExpr = null;
                for (int d = 0; d < inst.H; d++) {
                    if (inst.monthOfDay[d] == m) {
                        for (int s = 0; s < inst.nShifts; s++) {
                            hoursExpr = cpx.SumProd(hoursExpr, inst.shiftLength[s], X[p][d][s]);
                        }
                    }
                }
                if (hoursExpr == null) {
                    hoursExpr = cpx.prod(0.0, one);
                }
                cpx.addEq(Hours[p][m], hoursExpr, "HoursEq[" + p + "," + m + "]");
                cpx.addGe(SigmaH[p][m], cpx.sum(Hours[p][m], cpx.prod(-inst.targetHours[p], one)), "LAD1[" + p + "," + m + "]");
                cpx.addGe(SigmaH[p][m], cpx.sum(cpx.prod(inst.targetHours[p], one), cpx.prod(-1.0, Hours[p][m])), "LAD2[" + p + "," + m + "]");
            }
        }
    }

    public boolean solve() throws IloException {
        return cpx.solve();
    }

    public void extractSolution() throws IloException {
        // TODO: read solution values from CPLEX and map into a solution object
    }

    public void exportResults() throws IloException {
        // TODO: export result metrics for sensitivity analysis and CSV reporting
    }

    /*public void print() throws Exception {
        System.out.println("PSPER model built with " + inst.nPhysicians + " physicians, " + inst.H + " days, " + inst.nShifts + " shifts.");
    }*/
}
