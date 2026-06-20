/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ProOF.apl.factorys;

import ProOF.apl.sample2.problem.cplex.TSPFull;
import ProOF.apl.advanced2.problem.cplex.GCISTModelFull;
import ProOF.apl.advanced2.problem.cplex.MLCLSPwBFull;
import ProOF.com.language.Factory;
import ProOF.CplexOpt.CplexFull;
import ProOF.apl.sample2.problem.cplex.PSPERFull;
import ilog.concert.IloException;

/**
 *
 * @author marcio
 */
public class fCplexFull extends Factory<CplexFull>{
    public static final fCplexFull obj = new fCplexFull();
    
    @Override
    public String name() {
        return "Models";
    }
    
    @Override
    public CplexFull build(int index) throws IloException {
        switch(index){
            case 0: return new TSPFull();
            case 1: return new GCISTModelFull(null);
            case 2: return new MLCLSPwBFull();
            case 3: return new PSPERFull();
        }
        return null;
    }
}
