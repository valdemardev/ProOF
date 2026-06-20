/*
 * To change this template, choose Tools | Templates
 * and open this template in the editor.
 */
package ProOF.apl.sample2.problem.cplex;

import ProOF.CplexExtended.CplexExtended;
import ProOF.apl.sample1.problem.PSPER.PSPERInstance;
import ProOF.com.Linker.LinkerApproaches;
import ProOF.CplexOpt.CplexFull;
import ilog.concert.IloException;

/**
 * Full CPLEX entry point for PSPER.
 */
public class PSPERFull extends CplexFull {
    public PSPERInstance inst = new PSPERInstance();
    private PSPERmodel model;

    public PSPERFull() throws IloException {
        super();
    }

    @Override
    public void services(LinkerApproaches link) throws Exception {
        super.services(link);
        inst = link.add(inst);
    }

    @Override
    public String name() {
        return "PSPER-full";
    }

    @Override
    public void model() throws Exception {
        model = new PSPERmodel(inst, cpx);
        model.model(false);
      //  cpx.exportModel("../../../"+inst.file.getName()+".lp");
    }

    @Override
    public void print() throws Exception {
        super.print();
        model.print();
    }
}
