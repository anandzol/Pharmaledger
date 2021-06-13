package net.corda.pharmaledger.pharma;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;

@InitiatingFlow
@StartableByRPC
public class SendTrial extends FlowLogic<String> {


    public SendTrial() {
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        // TODO Auto-generated method stub
        return null;
    }
    
}

@InitiatedBy(SendTrial.class)
class SendTrialResponse extends FlowLogic<Void>{

    public SendTrialResponse() {
    }

    @Override
    public Void call() throws FlowException {
        // TODO Auto-generated method stub
        return null;
    }

}