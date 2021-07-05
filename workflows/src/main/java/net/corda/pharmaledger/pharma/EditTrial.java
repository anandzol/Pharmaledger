package net.corda.pharmaledger.pharma;

import java.security.PublicKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
import net.corda.pharmaledger.medical.states.PatientState;
import net.corda.pharmaledger.pharma.contracts.TrialStateContract;
import net.corda.pharmaledger.pharma.states.TrialState;

@InitiatingFlow
@StartableByRPC
public class EditTrial extends FlowLogic<String> {
    private String trialID;
    private String patientID;
    private String fromPharma;
    private String toMedical;
    private String trialStartDate;
    private String trialEndDate;
    private boolean editPatient;
    private boolean editDate;


    public EditTrial(String trialID, String patientID, String fromPharma, String toMedical) {
        this.trialID = trialID;
        this.patientID = patientID;
        this.fromPharma = fromPharma;
        this.toMedical = toMedical;
        this.editPatient = true;
    }

    public EditTrial(String trialID, String trialStartDate, String trialEndDate, String fromPharma, String toMedical) {
        this.trialID = trialID;
        this.trialStartDate = trialStartDate;
        this.trialEndDate = trialEndDate;
        this.fromPharma = fromPharma;
        this.toMedical = toMedical;
        this.editDate = true;
    }
    

    @Override
    @Suspendable
    public String call() throws FlowException {
        List<StateAndRef<TrialState>> trialStateAndRefs = getServiceHub().getVaultService()
        .queryBy(TrialState.class).getStates();

        StateAndRef<TrialState> inputStateAndRef = trialStateAndRefs.stream().filter(trialStateAndRef -> {
            TrialState trialstate = trialStateAndRef.getState().getData();
            return trialstate.getTrialID().equals(trialID);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Trial Not Found"));


        List<StateAndRef<PatientState>> patientStateAndRefs = getServiceHub().getVaultService()
        .queryBy(PatientState.class).getStates();
        StateAndRef<PatientState> inputPatientStateAndRef = patientStateAndRefs.stream().filter(patientStateAndRef -> {
        PatientState patientState = patientStateAndRef.getState().getData();
            return patientState.getPatientID().equals(patientID);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Patient data Not Found"));

        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(fromPharma).get(0).getState().getData();
        PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();

        AccountInfo targetAccount = accountService.accountInfo(toMedical).get(0).getState().getData();
        AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));

        TrialState inputTrial = inputStateAndRef.getState().getData();

        String trialPatients = inputTrial.getTrialPatients();
        if (editPatient) {
            if (trialPatients.isEmpty()) {
                trialPatients = patientID;
            } else {
                trialPatients = trialPatients + "," + patientID;
            }
        }
        Date startDate = inputTrial.getStartDate();
        Date endDate = inputTrial.getEndDate();

        if(editDate) {
            try {
                startDate = new SimpleDateFormat("dd/MM/yyyy").parse(trialStartDate);
                endDate = new SimpleDateFormat("dd/MM/yyyy").parse(trialEndDate);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        }

        TrialState updatedTrial = new TrialState(inputTrial.getTrialID(), trialPatients, inputTrial.getTrialTemplateID(), inputTrial.getStatus(), 
        startDate, endDate, new AnonymousParty(myKey), targetAcctAnonymousParty);
        
         final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TransactionBuilder txbuilder = new TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(updatedTrial)
                .addCommand(new TrialStateContract.Commands.Create(), Arrays.asList(targetAcctAnonymousParty.getOwningKey(),myKey));

        SignedTransaction locallySignedTx = getServiceHub().signInitialTransaction(txbuilder,Arrays.asList(getOurIdentity().getOwningKey(),myKey));
        FlowSession sessionForAccountToSendTo = initiateFlow(targetAccount.getHost());

        List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(new CollectSignatureFlow(locallySignedTx,
                sessionForAccountToSendTo,targetAcctAnonymousParty.getOwningKey()));
        SignedTransaction signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature);
        
        subFlow(new FinalityFlow(signedByCounterParty,
                Arrays.asList(sessionForAccountToSendTo).stream().filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())));

        return "Successfully sent trial data with ID: " + trialID;
    }
    
}

@InitiatedBy(EditTrial.class)
class EditTrialResponder extends FlowLogic<Void>{

    private FlowSession counterpartySession;

    public EditTrialResponder(FlowSession counterpartySession) {
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