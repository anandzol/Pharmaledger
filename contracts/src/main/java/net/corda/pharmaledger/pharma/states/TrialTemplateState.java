package net.corda.pharmaledger.pharma.states;

import java.util.ArrayList;
import java.util.List;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.pharmaledger.pharma.contracts.TrialTemplateStateContract;

@BelongsToContract(TrialTemplateStateContract.class)
public class TrialTemplateState implements ContractState {
    private String trialTemplateID;
    private String trialResult;   // check for its necessity in future
    private String trialDirection;
    private AnonymousParty fromPharma;
    private AnonymousParty toMedical;
    private List<AbstractParty> participants;


    public TrialTemplateState(String trialTemplateID, String trialResult, String trialDirection, AnonymousParty fromPharma, AnonymousParty toMedical) {
        this.trialTemplateID = trialTemplateID;
        this.trialResult = trialResult;
        this.trialDirection = trialDirection;
        this.fromPharma = fromPharma;
        this.toMedical = toMedical;
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(fromPharma);
        this.participants.add(toMedical);
    }


    public String getTrialTemplateID() {
        return this.trialTemplateID;
    }

    public void setTrialTemplateID(String trialTemplateID) {
        this.trialTemplateID = trialTemplateID;
    }

    public String getTrialResult() {
        return this.trialResult;
    }

    public void setTrialResult(String trialResult) {
        this.trialResult = trialResult;
    }

    public String getTrialDirection() {
        return this.trialDirection;
    }

    public void setTrialDirection(String trialDirection) {
        this.trialDirection = trialDirection;
    }

    public AnonymousParty getFromPharma() {
        return this.fromPharma;
    }

    public void setFromPharma(AnonymousParty fromPharma) {
        this.fromPharma = fromPharma;
    }

    public AnonymousParty getToMedical() {
        return this.toMedical;
    }

    public void setToMedical(AnonymousParty toMedical) {
        this.toMedical = toMedical;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }
}
