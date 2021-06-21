package net.corda.pharmaledger.logistics;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.CollectSignatureFlow;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.ReceiveFinalityFlow;
import net.corda.core.flows.SignTransactionFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.pharmaledger.accountUtilities.NewKeyForAccount;
import net.corda.pharmaledger.logistics.contracts.ShipmentStateContract;
import net.corda.pharmaledger.logistics.states.ShipmentState;

@InitiatingFlow
@StartableByRPC
public class UpdateShipmentTracker extends FlowLogic<String>{

    private String packageID;
    private String status;
    private String fromLogistics;
    private String toPharma;

    public UpdateShipmentTracker(String packageID, String status, String fromLogistics, String toPharma) {
        this.packageID = packageID;
        this.status = status;
        this.fromLogistics = fromLogistics;
        this.toPharma = toPharma;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(fromLogistics).get(0).getState().getData();
        PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();

        AccountInfo targetAccount = accountService.accountInfo(toPharma).get(0).getState().getData();
        AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));
        
        List<StateAndRef<ShipmentState>> ShipmentStateAndRefs = getServiceHub().getVaultService()
        .queryBy(ShipmentState.class).getStates();

        StateAndRef<ShipmentState> inputStateAndRef = ShipmentStateAndRefs.stream().filter(ShipmentStateAndRef -> {
            ShipmentState shipmentstate = ShipmentStateAndRef.getState().getData();
            return shipmentstate.getpackageID().equals(packageID);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Shipment MappingID Not Found"));
        
       
        ShipmentState shipment = new ShipmentState(packageID, status, new AnonymousParty(myKey), targetAcctAnonymousParty);

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TransactionBuilder txbuilder = new TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(shipment)
                .addCommand(new ShipmentStateContract.Commands.Create(), Arrays.asList(targetAcctAnonymousParty.getOwningKey(),myKey));

        SignedTransaction locallySignedTx = getServiceHub().signInitialTransaction(txbuilder,Arrays.asList(getOurIdentity().getOwningKey(),myKey));
        FlowSession sessionForAccountToSendTo = initiateFlow(targetAccount.getHost());

        List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(new CollectSignatureFlow(locallySignedTx,
                sessionForAccountToSendTo,targetAcctAnonymousParty.getOwningKey()));
        SignedTransaction signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature);

        subFlow(new FinalityFlow(signedByCounterParty,
                Arrays.asList(sessionForAccountToSendTo).stream().filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())));

        
        return "Shipment Created with Package ID: " + packageID;
    }
 
}

@InitiatedBy(UpdateShipmentTracker.class)
class UpdateShipmentTrackerResponder extends FlowLogic<Void> {
    //private variable
    private FlowSession counterpartySession;

    //Constructor
    public UpdateShipmentTrackerResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        subFlow(new ReceiveFinalityFlow(counterpartySession));
        return null;
    }
}