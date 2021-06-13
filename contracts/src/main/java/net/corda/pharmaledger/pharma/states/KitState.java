package net.corda.pharmaledger.pharma.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.pharma.contracts.KitStateContract;

@BelongsToContract(KitStateContract.class)
public class KitState {
    private int kitID;
    private String patientID;
    

    public KitState() {
    }

    public int getKitID() {
        return this.kitID;
    }

    public void setKitID(int kitID) {
        this.kitID = kitID;
    }

    public String getPatientID() {
        return this.patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }
}
