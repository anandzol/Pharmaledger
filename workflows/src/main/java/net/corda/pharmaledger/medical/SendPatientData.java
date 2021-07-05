package net.corda.pharmaledger.medical;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;

import co.paralleluniverse.fibers.Suspendable;
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
import net.corda.pharmaledger.medical.contracts.PatientStateContract;
import net.corda.pharmaledger.medical.states.PatientState;

@InitiatingFlow
@StartableByRPC
public class SendPatientData extends FlowLogic<String>{

    private String patientID;
    private String shipmentMappingID;
    private String medicalStaff;
    private String fromMedical;
    private String toPharma;
    private int Age;
    private String Gender;
    private int Weight; 
    private int Height;

    public SendPatientData(String patientID, String shipmentMappingID, String medicalStaff, String fromMedical, String toPharma, int Age, String Gender, int Weight, int Height) {
        this.patientID = patientID;
        this.shipmentMappingID = shipmentMappingID;
        this.medicalStaff = medicalStaff;
        this.fromMedical = fromMedical;
        this.toPharma = toPharma;
        this.Age = Age;
        this.Gender = Gender;
        this.Weight = Weight;
        this.Height = Height;
    }

    
    @Override
    @Suspendable
    public String call() throws FlowException {
        
        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(fromMedical).get(0).getState().getData();
        PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();

        AccountInfo targetAccount = accountService.accountInfo(toPharma).get(0).getState().getData();
        AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));

        PatientState patientData = new PatientState(patientID, shipmentMappingID, medicalStaff, Age, Gender, Height, Weight, new AnonymousParty(myKey), targetAcctAnonymousParty);

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TransactionBuilder txbuilder = new TransactionBuilder(notary)
                .addOutputState(patientData)
                .addCommand(new PatientStateContract.Commands.Create(), Arrays.asList(targetAcctAnonymousParty.getOwningKey(),myKey));

        SignedTransaction locallySignedTx = getServiceHub().signInitialTransaction(txbuilder,Arrays.asList(getOurIdentity().getOwningKey(),myKey));
        FlowSession sessionForAccountToSendTo = initiateFlow(targetAccount.getHost());

        List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(new CollectSignatureFlow(locallySignedTx,
                sessionForAccountToSendTo,targetAcctAnonymousParty.getOwningKey()));
        SignedTransaction signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature);
        
        subFlow(new FinalityFlow(signedByCounterParty,
                Arrays.asList(sessionForAccountToSendTo).stream().filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())));

        return "Successfully sent for patient ID:" + patientID;
    }
}
    
@InitiatedBy(SendPatientData.class)
class SendPatientDataResponder extends FlowLogic<Void> {
    //private variable
    private FlowSession counterpartySession;

    //Constructor
    public SendPatientDataResponder(FlowSession counterpartySession) {
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
