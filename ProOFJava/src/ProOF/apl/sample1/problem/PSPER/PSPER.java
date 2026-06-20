/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ProOF.apl.sample1.problem.PSPER;

import ProOF.com.Linker.LinkerApproaches;
import ProOF.gen.best.BestSol;
import ProOF.opt.abst.problem.meta.Objective;
import ProOF.opt.abst.problem.meta.Problem;
import ProOF.opt.abst.problem.meta.codification.Codification;

/**
 * PSPER problem definition.
 *
 * This class follows the same structure used by the TSP example in ProOF.
 */
public class PSPER extends Problem<BestSol> {
    public final PSPERInstance inst = new PSPERInstance();

    @Override
    public String name() {
        return "PSPER";
    }

    @Override
    public Codification build_codif() throws Exception {
        return new cPSPER(this);
    }

    @Override
    public Objective build_obj() throws Exception {
        return new PSPERObjective();
    }

    @Override
    public void services(LinkerApproaches link) throws Exception {
        super.services(link);
        link.add(inst);
        // link.add(PSPEROpertator.obj); // use this if operators are implemented later
    }

    @Override
    public BestSol best() {
        return BestSol.object();
    }

    @Override
    public void start() throws Exception {
        // add_gap("gap", inst.optimal); // optional if optimal value is known
    }
}
