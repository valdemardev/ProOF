/*
 * To change this template, choose Tools | Templates
 * and open this template in the editor.
 */
package ProOF.apl.sample1.problem.PSPER;

import ProOF.opt.abst.problem.meta.objective.SingleObjective;

/**
 * PSPER objective placeholder.
 */
public class PSPERObjective extends SingleObjective<PSPER, cPSPER, PSPERObjective> {

    public PSPERObjective() throws Exception {
        super();
    }

    @Override
    public void evaluate(PSPER prob, cPSPER codif) throws Exception {
        double fitness = 0.0;
        // TODO: compute objective based on assignment, demand, preferences, and fairness.
        set(fitness);
    }

    @Override
    public PSPERObjective build(PSPER prob) throws Exception {
        return new PSPERObjective();
    }
}
