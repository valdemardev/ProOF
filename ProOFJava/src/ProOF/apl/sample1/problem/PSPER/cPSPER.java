/*
 * To change this template, choose Tools | Templates
 * and open this template in the editor.
 */
package ProOF.apl.sample1.problem.PSPER;

import ProOF.opt.abst.problem.meta.codification.Codification;

/**
 * Codification for PSPER solutions.
 */
public class cPSPER extends Codification<PSPER, cPSPER> {
    public int[][][] assignment; // [p][d][s]

    public cPSPER(PSPER prob) {
        this.assignment = new int[prob.inst.nPhysicians][prob.inst.H][prob.inst.nShifts];
    }

    @Override
    public void copy(PSPER prob, cPSPER source) throws Exception {
        for (int p = 0; p < assignment.length; p++) {
            for (int d = 0; d < assignment[p].length; d++) {
                System.arraycopy(source.assignment[p][d], 0, this.assignment[p][d], 0, this.assignment[p][d].length);
            }
        }
    }

    @Override
    public cPSPER build(PSPER prob) throws Exception {
        return new cPSPER(prob);
    }
}
