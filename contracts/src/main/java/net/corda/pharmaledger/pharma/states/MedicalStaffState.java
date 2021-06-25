package net.corda.pharmaledger.pharma.states;

import java.util.ArrayList;
import java.util.List;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.pharmaledger.pharma.contracts.MedicalStaffStateContract;

@BelongsToContract(MedicalStaffStateContract.class)
public class MedicalStaffState implements ContractState {
    private String staffID;
    private String staffName;
    private String proficiency;
    private List<AbstractParty> participants;


    public MedicalStaffState(String staffID, String staffName, String proficiency, AnonymousParty medicalAccount, AnonymousParty pharmaAccount) {
        this.staffID = staffID;
        this.staffName = staffName;
        this.proficiency = proficiency;
        this.participants = new ArrayList<AbstractParty>();
        participants.add(medicalAccount);
        participants.add(pharmaAccount);
    }


    public String getStaffID() {
        return this.staffID;
    }

    public void setStaffID(String staffID) {
        this.staffID = staffID;
    }

    public String getStaffName() {
        return this.staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getProficiency() {
        return this.proficiency;
    }

    public void setProficiency(String proficiency) {
        this.proficiency = proficiency;
    }


    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }

}