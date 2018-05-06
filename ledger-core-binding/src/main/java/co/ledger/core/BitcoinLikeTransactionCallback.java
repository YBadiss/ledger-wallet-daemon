// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from callback.djinni

package co.ledger.core;

/**
 *Callback triggered by main completed task,
 *returns optional result of template type T
 */
public abstract class BitcoinLikeTransactionCallback {
    /**
     * Method triggered when main task complete
     * @params result optional of type T, non null if main task failed
     * @params error optional of type Error, non null if main task succeeded
     */
    public abstract void onCallback(BitcoinLikeTransaction result, Error error);
}
