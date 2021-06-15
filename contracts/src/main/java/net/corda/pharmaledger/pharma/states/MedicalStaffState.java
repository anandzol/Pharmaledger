package net.corda.pharmaledger.pharma.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.pharma.contracts.MedicalStaffStateContract;

@BelongsToContract(MedicalStaffStateContract.class)
public class MedicalStaffState {
    private int staffID;
    private String staffName;
    private String proficiency;


    public int getStaffID() {
        return this.staffID;
    }

    public void setStaffID(int staffID) {
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

}
