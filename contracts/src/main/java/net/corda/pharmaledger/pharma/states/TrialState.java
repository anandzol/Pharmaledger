package net.corda.pharmaledger.pharma.states;

import com.google.gson.*;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.pharma.contracts.TrialStateContract;

@BelongsToContract(TrialStateContract.class)
public class TrialState {
    private int trialID;
    private String trialDetails; 


    public int getTrialID() {
        return this.trialID;
    }

    public void setTrialID(int trialID) {
        this.trialID = trialID;
    }

    public String getTrialDetails() {
        return this.trialDetails;
    }

    public void setTrialDetails(String trialDetails) {
        Gson gson = new Gson();
        this.trialDetails = trialDetails;
    }
}
