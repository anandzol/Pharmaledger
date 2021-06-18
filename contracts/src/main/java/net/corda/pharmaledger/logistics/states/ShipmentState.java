package net.corda.pharmaledger.logistics.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.logistics.contracts.ShipmentStateContract;

@BelongsToContract(ShipmentStateContract.class)
public class ShipmentState {
    // private int shipmentMappingID; 
    //currently using this state only for the view from pharma account
    private String status; 

    /*  public int getShipmentID() {
        return this.shipmentID;
    }

    public void setShipmentID(int shipmentID) {
        this.shipmentID = shipmentID;
    }*/


    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
