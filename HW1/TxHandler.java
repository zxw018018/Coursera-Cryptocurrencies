import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        //create a new pool
        UTXOPool newPool = new UTXOPool();
        double previousTxOutSum = 0;
        double currentTxOutSum = 0;
        //for every member of input
        for (int i = 0; i < tx.numInputs(); i++) {
            //get ith input
            Transaction.Input input = tx.getInput(i);
            //create a new utxo
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            //get the output of utxo
            Transaction.Output output = utxoPool.getTxOutput(utxo);
            //(1) all outputs claimed by {@code tx} are in the current UTXO pool
            if (!utxoPool.contains(utxo)) return false;
            //(2) the signatures on each input of {@code tx} are valid
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature))
                return false;
            //(3) no UTXO is claimed multiple times by {@code tx}
            if (newPool.contains(utxo)) return false;
            //add utxo to newPool
            newPool.addUTXO(utxo, output);
            //update previousTxOutSum
            previousTxOutSum += output.value;
        }
        //for every member of output
        for (Transaction.Output out : tx.getOutputs()) {
            //(4) all of {@code tx}s output values are non-negative
            if (out.value < 0) return false;
            //update currentTxOutSum
            currentTxOutSum += out.value;
        }
        //(5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
        //*     values; and false otherwise.
        return previousTxOutSum >= currentTxOutSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Set<Transaction> validTxs = new HashSet<>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                validTxs.add(tx);
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output output = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, output);
                }
            }
        }

        Transaction[] validTxArray = new Transaction[validTxs.size()];
        return validTxs.toArray(validTxArray);
    
    }

   

}
